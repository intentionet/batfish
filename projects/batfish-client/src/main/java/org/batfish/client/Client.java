package org.batfish.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Closer;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.batfish.client.Command.TestComparisonMode;
import org.batfish.client.answer.LoadQuestionAnswerElement;
import org.batfish.client.config.Settings;
import org.batfish.client.config.Settings.RunMode;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.Container;
import org.batfish.common.CoordConsts;
import org.batfish.common.CoordConsts.WorkStatusCode;
import org.batfish.common.Pair;
import org.batfish.common.Task;
import org.batfish.common.Task.Batch;
import org.batfish.common.Version;
import org.batfish.common.WorkItem;
import org.batfish.common.plugin.AbstractClient;
import org.batfish.common.plugin.IClient;
import org.batfish.common.util.Backoff;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.WorkItemBuilder;
import org.batfish.common.util.ZipUtility;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.Protocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.AutocompleteSuggestion.CompletionType;
import org.batfish.datamodel.pojo.WorkStatus;
import org.batfish.datamodel.questions.BgpPeerPropertySpecifier;
import org.batfish.datamodel.questions.BgpProcessPropertySpecifier;
import org.batfish.datamodel.questions.InstanceData;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.InterfacesSpecifier;
import org.batfish.datamodel.questions.NamedStructureSpecifier;
import org.batfish.datamodel.questions.NodePropertySpecifier;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.Variable;
import org.batfish.datamodel.questions.VxlanVniPropertySpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.RoutingProtocolSpecifier;
import org.batfish.specifier.ShorthandInterfaceSpecifier;
import org.batfish.specifier.SpecifierFactories;
import org.batfish.specifier.parboiled.ParboiledIpSpaceSpecifierFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;
import sun.misc.Signal;
import sun.misc.SignalHandler;

@SuppressWarnings("restriction")
public class Client extends AbstractClient implements IClient {

  private static final Set<String> COMPARATORS =
      new HashSet<>(Arrays.asList(">", ">=", "==", "!=", "<", "<="));

  private static final String DEFAULT_NETWORK_PREFIX = "np";

  private static final String DEFAULT_QUESTION_PREFIX = "q";

  private static final String DEFAULT_SNAPSHOT_PREFIX = "ss_";

  private static final String DIFF_NOT_READY_MSG =
      "Cannot ask differential question without first setting reference snapshot\n";

  private static final String ENV_HOME = "HOME";

  private static final String FLAG_FAILING_TEST = "-error";

  private static final String HISTORY_FILE = ".batfishclient_history";

  private static final int NUM_TRIES_WARNING_THRESHOLD = 5;

  private static final String STARTUP_FILE = ".batfishclientrc";

  /**
   * Verify that every non-optional variable has value assigned to it.
   *
   * @throws BatfishException when there exists a missing parameter: it is not optional in {@code
   *     variable}, but the user failed to provide it.
   */
  static void checkVariableState(Map<String, Variable> variables) throws BatfishException {
    for (Entry<String, Variable> e : variables.entrySet()) {
      String variableName = e.getKey();
      Variable variable = e.getValue();
      if (!variable.getOptional() && variable.getValue() == null) {
        throw new BatfishException(String.format("Missing parameter: %s", variableName));
      }
    }
  }

  /**
   * For each key in {@code parameters}, validate that its value satisfies the requirements
   * specified by {@code variables} for that specific key. Set value to {@code variables} if
   * validation passed.
   *
   * @throws BatfishException if the key in parameters does not exist in variable, or the values in
   *     {@code parameters} do not match the requirements in {@code variables} for that specific
   *     key.
   */
  static void validateAndSet(Map<String, JsonNode> parameters, Map<String, Variable> variables)
      throws BatfishException {
    for (Entry<String, JsonNode> e : parameters.entrySet()) {
      String parameterName = e.getKey();
      JsonNode value = e.getValue();
      Variable variable = variables.get(parameterName);
      if (variable == null) {
        throw new BatfishException(
            "No variable named: '" + parameterName + "' in supplied question template");
      }
      if (variable.getMinElements() != null) {
        // Value is an array, check size and validate each elements in it
        if (!value.isArray() || value.size() < variable.getMinElements()) {
          throw new BatfishException(
              String.format(
                  "Invalid value for parameter %s: %s. "
                      + "Expecting a JSON array of at least %d "
                      + "elements",
                  parameterName, value, variable.getMinElements()));
        }
        for (JsonNode node : value) {
          validateNode(node, variable, parameterName);
        }
      } else {
        validateNode(value, variable, parameterName);
      }
      // validation passed.
      variable.setValue(value);
    }
  }

  /**
   * Validate that json-encoded {@code jsonPath} is a valid jsonPath dictionary (A valid jsonPath
   * contains key 'path' which mapping to a String, and an optional key 'suffix' which mapping to a
   * boolean value).
   *
   * @throws BatfishException if {@code jsonPath} is not a valid jsonPath dictionary.
   */
  static void validateJsonPath(JsonNode jsonPath) throws BatfishException {
    if (!jsonPath.isContainerNode()) {
      throw new BatfishException(
          String.format(
              "Expecting a JSON dictionary for a Batfish %s", Variable.Type.JSON_PATH.getName()));
    }
    if (jsonPath.get("path") == null) {
      throw new BatfishException(
          String.format("Missing 'path' element of %s", Variable.Type.JSON_PATH.getName()));
    }
    if (!jsonPath.get("path").isTextual()) {
      throw new BatfishException(
          String.format(
              "'path' element of %s must be a JSON string", Variable.Type.JSON_PATH.getName()));
    }
    if (jsonPath.get("suffix") != null && !jsonPath.get("suffix").isBoolean()) {
      throw new BatfishException(
          String.format(
              "'suffix' element of %s must be a JSON boolean", Variable.Type.JSON_PATH.getName()));
    }
  }

  /**
   * Validate that {@code jsonPathRegex} contains a valid Java regular expression of a {@code
   * JsonPath} (Starts with "/", ends with either "/" or "/i", contains a valid Java regular
   * expression between "/").
   *
   * <p>As written, this function will accept the strings "/" and "/i" as complete expressions –
   * resulting in an empty inner Java regular expression.
   *
   * @throws BatfishException if the content of {@code jsonPathRegex} is not a valid Java regular
   *     expression of a JsonPath.
   */
  static void validateJsonPathRegex(String jsonPathRegex) throws BatfishException {
    if (!jsonPathRegex.startsWith("/")) {
      throw new BatfishException(
          String.format(
              "A Batfish %s must start with \"/\"", Variable.Type.JSON_PATH_REGEX.getName()));
    }
    if (!(jsonPathRegex.endsWith("/") || jsonPathRegex.endsWith("/i"))) {
      throw new BatfishException(
          String.format(
              "A Batfish %s must end in either \"/\" or \"/i\"",
              Variable.Type.JSON_PATH_REGEX.getName()));
    }
    String innerPath = "";
    if (jsonPathRegex.lastIndexOf('/') > 0) {
      innerPath = jsonPathRegex.substring(1, jsonPathRegex.lastIndexOf('/'));
    }
    try {
      Pattern.compile(innerPath);
    } catch (PatternSyntaxException e) {
      throw new BatfishException(
          String.format(
              "Invalid %s at interior of %s",
              Variable.Type.JAVA_REGEX.getName(), Variable.Type.JSON_PATH_REGEX.getName()),
          e);
    }
  }

  /**
   * This method calls {@link Client#validateType(JsonNode, Variable)} to check that the contents
   * encoded in {@code value} match the requirement specified in {@code variable}. Also, this method
   * validates the input {@code value} is allowed according to {@link Variable#_values values}
   * specified in {@code variable}.
   *
   * @throws BatfishException if the content type encoded in input {@code value} does not satisfy
   *     the type requirements specified in {@code variable}, or the input {@code value} is not an
   *     allowed value for {@code variable}.
   */
  static void validateNode(JsonNode value, Variable variable, String parameterName)
      throws BatfishException {
    try {
      validateType(value, variable);
    } catch (BatfishException e) {
      String errorMessage =
          String.format("Invalid value for parameter %s: %s", parameterName, value);
      throw new BatfishException(errorMessage, e);
    }
    if (!variable.getValues().isEmpty()
        && variable.getValues().stream()
            .noneMatch(allowedValue -> allowedValue.getName().equals(value.asText()))) {
      throw new BatfishException(
          String.format(
              "Invalid value: %s, allowed values are: %s", value.asText(), variable.getValues()));
    }
  }

  /**
   * Validate the contents contained in json-encoded {@code value} matches the type required by
   * {@code variable}, and the length of input string meets the requirement of minimum length if
   * specified in {@code variable}. Call {@link Variable#getType()} on {@code variable} gives the
   * expected type.
   *
   * @throws BatfishException if the content encoded in input {@code value} does not satisfy the
   *     requirements specified in {@code variable}.
   */
  static void validateType(JsonNode value, Variable variable) throws BatfishException {
    int minLength = variable.getMinLength() == null ? 0 : variable.getMinLength();
    if (value.isTextual() && value.textValue().length() < minLength) {
      throw new BatfishException(
          String.format("Must be at least %s characters in length", minLength));
    }
    Variable.Type expectedType = variable.getType();
    switch (expectedType) {
      case ADDRESS_GROUP_AND_BOOK:
        if (!value.isTextual() || value.asText().split(",").length != 2) {
          throw new BatfishException(
              String.format(
                  "A Batfish %s must be a JSON string with two comma-separated values",
                  expectedType.getName()));
        }
        break;
      case ANSWER_ELEMENT:
        // this will barf with JsonProcessingException if the value is not castable
        try {
          BatfishObjectMapper.mapper().treeToValue(value, AnswerElement.class);
        } catch (JsonProcessingException e) {
          throw new BatfishException(
              String.format("Could not cast value to AnswerElement: %s", value), e);
        }
        break;
      case BGP_PEER_PROPERTY_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new BgpPeerPropertySpecifier(value.textValue());
        break;
      case BGP_PROCESS_PROPERTY_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new BgpProcessPropertySpecifier(value.textValue());
        break;
      case BGP_SESSION_STATUS:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case BGP_SESSION_TYPE:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case BOOLEAN:
        if (!value.isBoolean()) {
          throw new BatfishException(
              String.format("It is not a valid JSON %s value", expectedType.getName()));
        }
        break;
      case COMPARATOR:
        if (!(COMPARATORS.contains(value.textValue()))) {
          throw new BatfishException(
              String.format(
                  "It is not a known %s. Valid options are:" + " %s",
                  expectedType.getName(), COMPARATORS));
        }
        break;
      case DOUBLE:
        if (!value.isDouble()) {
          throw new BatfishException(
              String.format("It is not a valid JSON %s value", expectedType.getName()));
        }
        break;
      case DISPOSITION_SPEC:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("It is not a valid JSON %s value", expectedType.getName()));
        }
        break;
      case FILTER:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case FILTER_SPEC:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case FLOAT:
        if (!value.isFloat()) {
          throw new BatfishException(
              String.format("It is not a valid JSON %s value", expectedType.getName()));
        }
        break;
      case FLOW_STATE:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        FlowState.fromString(value.textValue());
        break;
      case HEADER_CONSTRAINT:
        if (!value.isObject() && !value.isNull()) {
          throw new BatfishException(
              String.format("Not a valid packet header constraint: %s", expectedType.getName()));
        }
        break;
      case INTEGER:
        if (!value.isInt()) {
          throw new BatfishException(
              String.format("It is not a valid JSON %s value", expectedType.getName()));
        }
        break;
      case INTEGER_SPACE:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        IntegerSpace.parse(value.asText());
        break;
      case INTERFACE:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case INTERFACE_GROUP_AND_BOOK:
        if (!value.isTextual() || value.asText().split(",").length != 2) {
          throw new BatfishException(
              String.format(
                  "A Batfish %s must be a JSON string with two comma-separated values",
                  expectedType.getName()));
        }
        break;
      case INTERFACE_NAME:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case INTERFACE_PROPERTY_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new InterfacePropertySpecifier(value.textValue());
        break;
      case INTERFACE_TYPE:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        Enum.valueOf(InterfaceType.class, value.textValue().toUpperCase());
        break;
      case INTERFACES_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        SpecifierFactories.getInterfaceSpecifierOrDefault(
            value.textValue(), new ShorthandInterfaceSpecifier(InterfacesSpecifier.ALL));
        break;
      case IP:
        // TODO: Need to double check isInetAddress()
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        Ip.parse(value.textValue());
        break;
      case IP_PROTOCOL:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        try {
          IpProtocol.fromString(value.textValue());
        } catch (IllegalArgumentException e) {
          throw new BatfishException(String.format("Unknown %s string", expectedType.getName()));
        }
        break;
      case IP_SPACE_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format(
                  "A Batfish %s must be a JSON string with IpSpaceSpec grammar",
                  expectedType.getName()));
        }
        new ParboiledIpSpaceSpecifierFactory().buildIpSpaceSpecifier(value.asText());
        break;
      case IP_WILDCARD:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new IpWildcard(value.textValue());
        break;
      case IPSEC_SESSION_STATUS:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case JAVA_REGEX:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        try {
          Pattern.compile(value.textValue());
        } catch (PatternSyntaxException e) {
          throw new BatfishException("It is not a valid Java regular " + "expression", e);
        }
        break;
      case JSON_PATH_REGEX:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        validateJsonPathRegex(value.textValue());
        break;
      case LOCATION_SPEC:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case LONG:
        if (!value.isLong()) {
          throw new BatfishException(
              String.format("It is not a valid JSON %s value", expectedType.getName()));
        }
        break;
      case NAMED_STRUCTURE_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new NamedStructureSpecifier(value.textValue());
        break;
      case NODE_PROPERTY_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new NodePropertySpecifier(value.textValue());
        break;
      case NODE_ROLE_DIMENSION:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case NODE_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        SpecifierFactories.getNodeSpecifierOrDefault(
            value.textValue(), AllNodesNodeSpecifier.INSTANCE);
        break;
      case OSPF_PROPERTY_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new OspfPropertySpecifier(value.textValue());
        break;
      case PATH_CONSTRAINT:
        if (!(value.isObject()) && !value.isNull()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON object or null", expectedType.getName()));
        }
        break;
      case PREFIX:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        Prefix.parse(value.textValue());
        break;
      case PREFIX_RANGE:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        PrefixRange.fromString(value.textValue());
        break;
      case PROTOCOL:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        Protocol.fromString(value.textValue());
        break;
      case QUESTION:
        // TODO: Implement
        break;
      case ROUTING_PROTOCOL_SPEC:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new RoutingProtocolSpecifier(value.textValue());
        break;
      case STRING:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case STRUCTURE_NAME:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case SUBRANGE:
        if (!(value.isTextual() || value.isInt())) {
          throw new BatfishException(
              String.format(
                  "A Batfish %s must be a JSON string or " + "integer", expectedType.getName()));
        }
        Object actualValue = value.isTextual() ? value.textValue() : value.asInt();
        new SubRange(actualValue);
        break;
      case VRF:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case VXLAN_VNI_PROPERTY_SPEC:
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new VxlanVniPropertySpecifier(value.textValue());
        break;
      case ZONE:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        break;
      case JSON_PATH:
        validateJsonPath(value);
        break;
      default:
        throw new BatfishException(String.format("Unsupported parameter type: %s", expectedType));
    }
  }

  private Map<String, String> _additionalBatfishOptions;

  private boolean _backgroundExecution = false;

  private final Map<String, String> _bfq;

  private String _currContainerName = null;

  private String _currDeltaTestrig;

  private String _currTestrig = null;

  private boolean _exit;

  BatfishLogger _logger;

  WorkItem _polledWorkItem = null;

  private LineReader _reader;

  private Settings _settings;

  private BfCoordWorkHelper _workHelper;

  public Client(Settings settings) {
    super(false);
    _additionalBatfishOptions = new HashMap<>();
    _bfq = new TreeMap<>();
    _settings = settings;

    switch (_settings.getRunMode()) {
      case batch:
        if (_settings.getBatchCommandFile() == null) {
          System.err.println(
              "org.batfish.client: Command file not specified while running in batch mode.");
          System.err.printf(
              "Use '-%s <cmdfile>' if you want batch mode, or '-%s interactive' if you want "
                  + "interactive mode\n",
              Settings.ARG_COMMAND_FILE, Settings.ARG_RUN_MODE);
          System.exit(1);
        }
        _logger =
            new BatfishLogger(_settings.getLogLevel(), false, _settings.getLogFile(), false, false);
        break;
      case genquestions:
        if (_settings.getQuestionsDir() == null) {
          System.err.println(
              "org.batfish.client: Out dir not specified while running in genquestions mode.");
          System.err.printf("Use '-%s <cmdfile>'\n", Settings.ARG_QUESTIONS_DIR);
          System.exit(1);
        }
        _logger =
            new BatfishLogger(_settings.getLogLevel(), false, _settings.getLogFile(), false, false);
        break;
      case interactive:
        try {
          Terminal terminal = TerminalBuilder.builder().build();
          _reader =
              LineReaderBuilder.builder()
                  .terminal(terminal)
                  .completer(new ArgumentCompleter(new CommandCompleter(), new NullCompleter()))
                  .build();
          Path historyPath = Paths.get(System.getenv(ENV_HOME), HISTORY_FILE);
          historyPath.toFile().createNewFile();
          _reader.setVariable(LineReader.HISTORY_FILE, historyPath.toAbsolutePath().toString());
          _reader
              .getTerminal()
              .handle(org.jline.terminal.Terminal.Signal.INT, signal -> handleSigInt());
          _reader.unsetOpt(Option.INSERT_TAB); // supports completion with nothing entered

          PrintWriter pWriter = new PrintWriter(_reader.getTerminal().output(), true);
          OutputStream os = new WriterOutputStream(pWriter, StandardCharsets.UTF_8);
          PrintStream ps = new PrintStream(os, true);
          _logger = new BatfishLogger(_settings.getLogLevel(), false, ps);
        } catch (Exception e) {
          System.err.printf("Could not initialize client: %s\n", e.getMessage());
          e.printStackTrace();
        }
        break;
      default:
        System.err.println("org.batfish.client: Unknown run mode.");
        System.exit(1);
    }
  }

  public Client(String[] args) {
    this(new Settings(args));
  }

  private boolean addBatfishOption(String[] words, List<String> options, List<String> parameters) {
    if (!isValidArgument(
        options, parameters, 0, 1, Integer.MAX_VALUE, Command.ADD_BATFISH_OPTION)) {
      return false;
    }
    String optionKey = parameters.get(0);
    String optionValue =
        String.join(" ", Arrays.copyOfRange(words, 2 + options.size(), words.length));
    _additionalBatfishOptions.put(optionKey, optionValue);
    return true;
  }

  private boolean answer(
      String questionTemplateName, String paramsLine, boolean isDelta, FileWriter outWriter) {
    String questionContentUnmodified = _bfq.get(questionTemplateName.toLowerCase());
    if (questionContentUnmodified == null) {
      throw new BatfishException("Invalid question template name: '" + questionTemplateName + "'");
    }
    Map<String, JsonNode> parameters = parseParams(paramsLine);
    JSONObject questionJson;
    try {
      questionJson = new JSONObject(questionContentUnmodified);
    } catch (JSONException e) {
      throw new BatfishException("Question content is not valid JSON", e);
    }
    String questionName = DEFAULT_QUESTION_PREFIX + "_" + UUID.randomUUID();
    if (parameters.containsKey("questionName")) {
      questionName = parameters.get("questionName").asText();
      parameters.remove("questionName");
    }
    // check if differential parameter was provided
    boolean differentialParamValue = false;
    if (parameters.containsKey("differential")) {
      if (!parameters.get("differential").isBoolean()) {
        throw new IllegalArgumentException(
            String.format(
                "The value of 'differential' should be boolean. Got '%s'",
                parameters.get("differential").asText()));
      }
      differentialParamValue = parameters.get("differential").asBoolean();
      parameters.remove("differential");
    }
    try {
      questionJson = QuestionHelper.fillTemplate(questionJson, parameters, questionName);
    } catch (IOException | JSONException e) {
      throw new BatfishException("Could not fill template: ", e);
    }
    String modifiedQuestionStr = questionJson.toString();

    boolean questionJsonDifferential;
    try {
      questionJsonDifferential =
          questionJson.has(BfConsts.PROP_DIFFERENTIAL)
              && questionJson.getBoolean(BfConsts.PROP_DIFFERENTIAL);
    } catch (JSONException e) {
      throw new BatfishException("Could not find whether question is explicitly differential", e);
    }
    if (questionJsonDifferential && _currDeltaTestrig == null) {
      _logger.output(DIFF_NOT_READY_MSG);
      return false;
    }
    Path questionFile = createTempFile(BfConsts.RELPATH_QUESTION_FILE, modifiedQuestionStr);
    questionFile.toFile().deleteOnExit();
    // upload the question
    boolean resultUpload =
        _workHelper.uploadQuestion(
            _currContainerName,
            isDelta ? _currDeltaTestrig : _currTestrig,
            questionName,
            questionFile.toAbsolutePath().toString());
    if (!resultUpload) {
      return false;
    }
    _logger.debug("Uploaded question. Answering now.\n");
    // delete the temporary params file
    CommonUtil.deleteIfExists(questionFile);
    // answer the question
    WorkItem wItemAs =
        WorkItemBuilder.getWorkItemAnswerQuestion(
            questionName,
            _currContainerName,
            _currTestrig,
            _currDeltaTestrig,
            questionJsonDifferential || differentialParamValue,
            isDelta);
    return execute(wItemAs, outWriter);
  }

  private boolean answer(
      String[] words,
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta) {
    Command command = delta ? Command.ANSWER_REFERENCE : Command.ANSWER;
    if (!isValidArgument(options, parameters, 0, 1, Integer.MAX_VALUE, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true) || (delta && !isDeltaReady())) {
      return false;
    }
    String qTypeStr = parameters.get(0);
    String paramsLine =
        String.join(" ", Arrays.copyOfRange(words, 2 + options.size(), words.length));
    return answer(qTypeStr, paramsLine, delta, outWriter);
  }

  private boolean answerFile(
      Path questionFile, boolean isDifferential, boolean isDelta, FileWriter outWriter) {

    if (!Files.exists(questionFile)) {
      throw new BatfishException("Question file not found: " + questionFile);
    }

    String questionName = DEFAULT_QUESTION_PREFIX + "_" + UUID.randomUUID();

    // upload the question
    boolean resultUpload =
        _workHelper.uploadQuestion(
            _currContainerName,
            isDelta ? _currDeltaTestrig : _currTestrig,
            questionName,
            questionFile.toAbsolutePath().toString());

    if (!resultUpload) {
      return false;
    }

    _logger.debug("Uploaded question. Answering now.\n");

    // answer the question
    WorkItem wItemAs =
        WorkItemBuilder.getWorkItemAnswerQuestion(
            questionName,
            _currContainerName,
            _currTestrig,
            _currDeltaTestrig,
            isDifferential,
            isDelta);

    return execute(wItemAs, outWriter);
  }

  private boolean answerType(
      String questionType, String paramsLine, boolean isDelta, FileWriter outWriter) {
    JSONObject questionJson;
    try {
      String questionString = QuestionHelper.getQuestionString(questionType, _questions, false);
      questionJson = new JSONObject(questionString);

      Map<String, JsonNode> parameters = parseParams(paramsLine);
      for (Entry<String, JsonNode> e : parameters.entrySet()) {
        String parameterName = e.getKey();
        String parameterValue = e.getValue().toString();
        Object parameterObj;
        try {
          parameterObj = new JSONTokener(parameterValue).nextValue();
          questionJson.put(parameterName, parameterObj);
        } catch (JSONException e1) {
          throw new BatfishException(
              "Failed to apply parameter: '"
                  + parameterName
                  + "' with value: '"
                  + parameterValue
                  + "' to question JSON",
              e1);
        }
      }
    } catch (JSONException e) {
      throw new BatfishException("Failed to convert unmodified question string to JSON", e);
    } catch (BatfishException e) {
      _logger.errorf("Could not construct a question: %s\n", e.getMessage());
      return false;
    }

    String modifiedQuestionJson = questionJson.toString();
    Question modifiedQuestion = null;
    try {
      modifiedQuestion =
          BatfishObjectMapper.mapper().readValue(modifiedQuestionJson, Question.class);
    } catch (IOException e) {
      throw new BatfishException(
          "Modified question is no longer valid, likely due to invalid parameters", e);
    }
    if (modifiedQuestion.getDifferential() && _currDeltaTestrig == null) {
      _logger.output(DIFF_NOT_READY_MSG);
      return false;
    }
    // if no exception is thrown, then the modifiedQuestionJson is good
    Path questionFile = createTempFile("question", modifiedQuestionJson);
    questionFile.toFile().deleteOnExit();
    boolean result =
        answerFile(questionFile, modifiedQuestion.getDifferential(), isDelta, outWriter);
    if (questionFile != null) {
      CommonUtil.deleteIfExists(questionFile);
    }
    return result;
  }

  private boolean autoComplete(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 1, 2, 2, Command.AUTOCOMPLETE)) {
      return false;
    }
    if (!isSetContainer(true) || !isSetTestrig()) {
      return false;
    }

    int maxSuggestions = options.size() == 1 ? Integer.parseInt(options.get(0)) : Integer.MAX_VALUE;
    CompletionType completionType = CompletionType.valueOf(parameters.get(0).toUpperCase());
    String query = parameters.get(1);

    String suggestionsJson =
        _workHelper.autoComplete(
            _currContainerName, _currTestrig, completionType, query, maxSuggestions);
    _logger.outputf("Auto complete: %s\n", suggestionsJson);
    return true;
  }

  private boolean cat(String[] words) throws IOException {
    if (words.length != 2) {
      _logger.errorf("Invalid arguments: %s\n", Arrays.toString(words));
      printUsage(Command.CAT);
      return false;
    }
    String filename = words[1];

    try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
      String line = null;
      while ((line = br.readLine()) != null) {
        _logger.outputf("%s\n", line);
      }
    }

    return true;
  }

  private boolean checkApiKey(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.CHECK_API_KEY)) {
      return false;
    }
    String isValid = _workHelper.checkApiKey();
    _logger.outputf("Api key validitiy: %s\n", isValid);
    return true;
  }

  private boolean clearScreen(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.CLEAR_SCREEN)) {
      return false;
    }
    _reader.getTerminal().puts(Capability.clear_screen);
    _reader.getTerminal().flush();
    return false;
  }

  private Path createTempFile(String filePrefix, String content) {
    Path tempFilePath;
    try {
      tempFilePath = Files.createTempFile(filePrefix, null);
    } catch (IOException e) {
      throw new BatfishException("Failed to create temporary file", e);
    }
    File tempFile = tempFilePath.toFile();
    tempFile.deleteOnExit();
    _logger.debugf("Creating temporary %s file: %s\n", filePrefix, tempFilePath.toAbsolutePath());
    FileWriter writer;
    try {
      writer = new FileWriter(tempFile);
      writer.write(content + "\n");
      writer.close();
    } catch (IOException e) {
      throw new BatfishException("Failed to write content to temporary file", e);
    }
    return tempFilePath;
  }

  private boolean configureTemplate(
      String[] words,
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters) {
    if (!isValidArgument(
        options, parameters, 0, 2, Integer.MAX_VALUE, Command.CONFIGURE_TEMPLATE)) {
      return false;
    }

    String newTemplateName = parameters.get(0);
    String oldTemplateName = parameters.get(1);
    String paramsLine =
        String.join(" ", Arrays.copyOfRange(words, 3 + options.size(), words.length));
    Map<String, JsonNode> params = parseParams(paramsLine);
    JsonNode exceptions = params.get("exceptions");
    JsonNode assertion = params.get("assertion");

    String oldTemplate = _bfq.get(oldTemplateName.toLowerCase());
    if (oldTemplate == null) {
      throw new BatfishException("Template not found: '" + oldTemplateName + "'");
    }
    if (!Sets.difference(params.keySet(), Sets.newHashSet("exceptions", "assertion")).isEmpty()) {
      throw new BatfishException("Unknown parameter(s): " + params.keySet());
    }

    String newTemplate = _workHelper.configureTemplate(oldTemplate, exceptions, assertion);

    if (newTemplate == null) {
      return false;
    }

    _bfq.put(newTemplateName.toLowerCase(), newTemplate);
    logOutput(outWriter, newTemplate);
    return true;
  }

  private boolean delAnalysis(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEL_ANALYSIS)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    String analysisName = parameters.get(0);

    boolean result = _workHelper.delAnalysis(_currContainerName, analysisName);

    logOutput(outWriter, "Result of deleting analysis " + analysisName + ": " + result + "\n");
    return result;
  }

  private boolean delAnalysisQuestions(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(
        options, parameters, 0, 2, Integer.MAX_VALUE, Command.DEL_ANALYSIS_QUESTIONS)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    String analysisName = parameters.get(0);

    String delQuestionsStr = "[]";

    try {
      JSONArray delQuestionsArray = new JSONArray();
      for (int index = 1; index < parameters.size(); index++) {
        delQuestionsArray.put(parameters.get(index));
      }
      delQuestionsStr = delQuestionsArray.toString(1);
    } catch (JSONException e) {
      throw new BatfishException("Failed to get JSONObject for analysis", e);
    }

    boolean result =
        _workHelper.configureAnalysis(
            _currContainerName, false, analysisName, null, delQuestionsStr);

    logOutput(outWriter, "Result of deleting analysis questions: " + result + "\n");
    return result;
  }

  private boolean delBatfishOption(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEL_BATFISH_OPTION)) {
      return false;
    }
    String optionKey = parameters.get(0);

    if (!_additionalBatfishOptions.containsKey(optionKey)) {
      _logger.outputf("Batfish option %s does not exist\n", optionKey);
      return false;
    }
    _additionalBatfishOptions.remove(optionKey);
    return true;
  }

  private boolean delNetwork(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEL_NETWORK)) {
      return false;
    }
    String containerName = parameters.get(0);
    boolean result = _workHelper.delNetwork(containerName);
    _logger.outputf("Result of deleting network: %s\n", result);
    return true;
  }

  private boolean delQuestion(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEL_QUESTION)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    String qName = parameters.get(0);
    boolean result = _workHelper.delQuestion(_currContainerName, _currTestrig, qName);
    _logger.outputf("Result of deleting question: %s\n", result);
    return true;
  }

  private boolean delSnapshot(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEL_SNAPSHOT)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    String testrigName = parameters.get(0);
    boolean result = _workHelper.delSnapshot(_currContainerName, testrigName);
    logOutput(outWriter, "Result of deleting snapshot: " + result + "\n");
    return true;
  }

  private boolean dir(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 1, Command.DIR)) {
      return false;
    }
    String dirname = (parameters.size() == 1) ? parameters.get(0) : ".";
    File currDirectory = new File(dirname);
    for (File file : currDirectory.listFiles()) {
      _logger.output(file.getName() + "\n");
    }
    return true;
  }

  private boolean echo(String[] words) {
    _logger.outputf("%s\n", String.join(" ", Arrays.copyOfRange(words, 1, words.length)));
    return true;
  }

  private boolean execute(WorkItem wItem, @Nullable FileWriter outWriter) {
    _logger.infof("work-id is %s\n", wItem.getId());
    ActiveSpan activeSpan = GlobalTracer.get().activeSpan();
    if (activeSpan != null) {
      activeSpan.setTag("work-id", wItem.getId().toString());
    }
    wItem.addRequestParam(BfConsts.ARG_LOG_LEVEL, _settings.getBatfishLogLevel());
    for (String option : _additionalBatfishOptions.keySet()) {
      wItem.addRequestParam(option, _additionalBatfishOptions.get(option));
    }
    boolean queueWorkResult = _workHelper.queueWork(wItem);
    _logger.infof("Queuing result: %s\n", queueWorkResult);
    if (!queueWorkResult) {
      return queueWorkResult;
    }

    boolean result = true;

    if (!_backgroundExecution) {
      _polledWorkItem = wItem;
      result = pollWorkAndGetAnswer(wItem, outWriter);
      _polledWorkItem = null;
    }

    return result;
  }

  private boolean exit(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.EXIT)) {
      return false;
    }
    _exit = true;
    return true;
  }

  private boolean generateDataplane(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.GEN_DP)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    // generate the data plane
    WorkItem wItemGenDp =
        WorkItemBuilder.getWorkItemGenerateDataPlane(_currContainerName, _currTestrig);

    return execute(wItemGenDp, outWriter);
  }

  private boolean generateReferenceDataplane(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.GEN_REFERENCE_DP)) {
      return false;
    }
    if (!isDeltaReady() || !isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    WorkItem wItemGenDdp =
        WorkItemBuilder.getWorkItemGenerateDeltaDataPlane(
            _currContainerName, _currTestrig, _currDeltaTestrig);

    return execute(wItemGenDdp, outWriter);
  }

  private void generateQuestions() {

    File questionsDir = Paths.get(_settings.getQuestionsDir()).toFile();

    if (!questionsDir.exists() && !questionsDir.mkdirs()) {
      _logger.errorf("Could not create questions dir %s\n", _settings.getQuestionsDir());
      System.exit(1);
    }

    _questions.forEach(
        (qName, supplier) -> {
          try {
            String questionString = QuestionHelper.getQuestionString(qName, _questions, true);
            String qFile =
                Paths.get(_settings.getQuestionsDir(), qName + ".json").toFile().getAbsolutePath();

            PrintWriter writer = new PrintWriter(qFile);
            writer.write(questionString);
            writer.close();
          } catch (Exception e) {
            _logger.errorf("Could not write question %s: %s\n", qName, e.getMessage());
          }
        });
  }

  private boolean get(
      String[] words,
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta) {
    Command command = delta ? Command.GET_REFERENCE : Command.GET;
    if (!isValidArgument(options, parameters, 0, 1, Integer.MAX_VALUE, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true) || (delta && !isDeltaReady())) {
      return false;
    }
    String qTypeStr = parameters.get(0).toLowerCase();
    String paramsLine =
        String.join(" ", Arrays.copyOfRange(words, 2 + options.size(), words.length));
    return answerType(qTypeStr, paramsLine, delta, outWriter);
  }

  private boolean getAnalysisAnswers(
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta,
      boolean differential) {
    Command command =
        differential
            ? Command.GET_ANALYSIS_ANSWERS_DIFFERENTIAL
            : delta ? Command.GET_ANALYSIS_ANSWERS_REFERENCE : Command.GET_ANALYSIS_ANSWERS;
    if (!isValidArgument(options, parameters, 0, 1, 1, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    String analysisName = parameters.get(0);

    String baseTestrig;
    String deltaTestrig;
    if (differential) {
      baseTestrig = _currTestrig;
      deltaTestrig = _currDeltaTestrig;
    } else if (delta) {
      baseTestrig = _currDeltaTestrig;
      deltaTestrig = null;
    } else {
      baseTestrig = _currTestrig;
      deltaTestrig = null;
    }
    String answer =
        _workHelper.getAnalysisAnswers(_currContainerName, baseTestrig, deltaTestrig, analysisName);

    if (answer == null) {
      return false;
    }

    logOutput(outWriter, answer + "\n");

    return true;
  }

  private boolean getAnswer(
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta,
      boolean differential) {
    Command command =
        differential
            ? Command.GET_ANSWER_DIFFERENTIAL
            : delta ? Command.GET_ANSWER_REFERENCE : Command.GET_ANSWER;
    if (!isValidArgument(options, parameters, 0, 1, 1, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    String questionName = parameters.get(0);

    String baseTestrig;
    String deltaTestrig;
    if (differential) {
      baseTestrig = _currTestrig;
      deltaTestrig = _currDeltaTestrig;
    } else if (delta) {
      baseTestrig = _currDeltaTestrig;
      deltaTestrig = null;
    } else {
      baseTestrig = _currTestrig;
      deltaTestrig = null;
    }
    String answerString =
        _workHelper.getAnswer(_currContainerName, baseTestrig, deltaTestrig, questionName);

    String answerStringToPrint = answerString;
    if (outWriter == null && _settings.getPrettyPrintAnswers()) {
      Answer answer;
      try {
        answer = BatfishObjectMapper.mapper().readValue(answerString, Answer.class);
      } catch (IOException e) {
        throw new BatfishException(
            "Response does not appear to be valid JSON representation of "
                + Answer.class.getSimpleName());
      }
      answerStringToPrint = answer.prettyPrint();
    }

    logOutput(outWriter, answerStringToPrint + "\n");

    return true;
  }

  private List<String> getCommandOptions(String[] words) {
    List<String> options = new LinkedList<>();

    int currIndex = 1;

    while (currIndex < words.length && words[currIndex].startsWith("-")) {
      options.add(words[currIndex]);
      currIndex++;
    }

    return options;
  }

  private List<String> getCommandParameters(String[] words, int numOptions) {
    return Arrays.asList(words).subList(numOptions + 1, words.length);
  }

  /**
   * Get a string representation of the file content for configuration file {@code configName}.
   *
   * <p>Returns {@code true} if successfully get file content, {@code false} otherwise.
   */
  private boolean getConfiguration(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 3, 3, Command.GET_CONFIGURATION)) {
      return false;
    }
    String containerName = parameters.get(0);
    String testrigName = parameters.get(1);
    String configName = parameters.get(2);
    String configContent = _workHelper.getConfiguration(containerName, testrigName, configName);
    if (configContent != null) {
      _logger.outputf("%s\n", configContent);
      return true;
    }
    return false;
  }

  /**
   * Get information of the container (first element in {@code parameters}).
   *
   * <p>Returns {@code true} if successfully get container information, {@code false} otherwise
   */
  private boolean getNetwork(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.GET_NETWORK)) {
      return false;
    }
    String containerName = parameters.get(0);
    Container container = _workHelper.getNetwork(containerName);
    if (container != null) {
      _logger.output(container.getTestrigs() + "\n");
      return true;
    }
    return false;
  }

  @Override
  public BatfishLogger getLogger() {
    return _logger;
  }

  private boolean getObject(
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta) {
    Command command = delta ? Command.GET_OBJECT_REFERENCE : Command.GET_OBJECT;
    if (!isValidArgument(options, parameters, 0, 1, 1, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true) || (delta && !isDeltaReady())) {
      return false;
    }

    String testrig = delta ? _currDeltaTestrig : _currTestrig;
    String objectName = parameters.get(0);
    String tmpPath = _workHelper.getObject(_currContainerName, testrig, objectName);
    String objectString = CommonUtil.readFile(Paths.get(tmpPath));

    logOutput(outWriter, objectString + "\n");

    return true;
  }

  /**
   * Returns the name from a JSON representing a question
   *
   * @param question question Json
   * @param questionIdentifier question path or question JSON key
   * @return name of question
   * @throws if any of instance or instanceName not found in question
   */
  static String getQuestionName(JSONObject question, String questionIdentifier) {
    if (!question.has(BfConsts.PROP_INSTANCE)) {
      throw new BatfishException(
          String.format("question %s does not have instance field", questionIdentifier));
    }
    try {
      if (!question.getJSONObject(BfConsts.PROP_INSTANCE).has(BfConsts.PROP_INSTANCE_NAME)) {
        throw new BatfishException(
            String.format(
                "question %s does not have instanceName field in instance", questionIdentifier));
      } else {
        return question
            .getJSONObject(BfConsts.PROP_INSTANCE)
            .getString(BfConsts.PROP_INSTANCE_NAME);
      }
    } catch (JSONException e) {
      throw new BatfishException(
          String.format("Failure in extracting instanceName from question %s", questionIdentifier));
    }
  }

  private boolean getQuestionTemplates(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.GET_QUESTION_TEMPLATES)) {
      return false;
    }

    JSONObject templates = _workHelper.getQuestionTemplates();

    if (templates == null) {
      return false;
    }

    _logger.outputf("Found %d templates\n", templates.length());

    try {
      _logger.output(templates.toString(1));
    } catch (JSONException e) {
      throw new BatfishException("Failed to print templates", e);
    }

    return true;
  }

  private boolean getWorkStatus(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.GET_WORK_STATUS)) {
      return false;
    }

    UUID workId = UUID.fromString(parameters.get(0));

    Pair<WorkStatusCode, String> response = _workHelper.getWorkStatus(workId);
    if (response == null) {
      return false;
    }

    printWorkStatusResponse(response, true);

    logOutput(outWriter, "status: " + response.getFirst());

    return true;
  }

  public Settings getSettings() {
    return _settings;
  }

  private String getTestComparisonString(Answer answer, TestComparisonMode comparisonMode)
      throws JsonProcessingException {
    switch (comparisonMode) {
      case COMPAREANSWER:
        // Use an array rather than a list to serialize the answer elements; this preserves
        // the type information. See https://github.com/FasterXML/jackson-databind/issues/336,
        // though this is a different workaround.
        AnswerElement[] elements = answer.getAnswerElements().toArray(new AnswerElement[0]);
        return BatfishObjectMapper.writePrettyString(elements);
      case COMPAREALL:
        return BatfishObjectMapper.writePrettyString(answer);
      case COMPAREFAILURES:
        return BatfishObjectMapper.writePrettyString(answer.getSummary().getNumFailed());
      case COMPARESUMMARY:
        return BatfishObjectMapper.writePrettyString(answer.getSummary());
      default:
        throw new BatfishException("Unhandled TestComparisonMode: " + comparisonMode);
    }
  }

  public void handleSigInt() {
    _logger.info("Got SIGINT\n");
    WorkItem wItem = _polledWorkItem;
    if (wItem != null) {
      _logger.outputf("Killing %s\n", wItem.getId());
      boolean result = _workHelper.killWork(_polledWorkItem.getId());
      _logger.outputf("Result of killing %s: %s\n", wItem.getId(), result);
    } else {
      _logger.output("No work being polled\n");
    }
  }

  private boolean help(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, Integer.MAX_VALUE, Command.HELP)) {
      return false;
    }
    if (parameters.size() == 1) {
      Command cmd = Command.fromName(parameters.get(0));
      printUsage(cmd);
    } else {
      printUsage();
    }
    return true;
  }

  private boolean initNetwork(List<String> options, List<String> parameters) {
    if (options.contains("-setname")) {
      if (!isValidArgument(options, parameters, 1, 1, 1, Command.INIT_NETWORK)) {
        return false;
      }
      _currContainerName = _workHelper.initNetwork(parameters.get(0), null);
    } else {
      if (!isValidArgument(options, parameters, 0, 0, 1, Command.INIT_NETWORK)) {
        return false;
      }
      String containerPrefix = parameters.isEmpty() ? DEFAULT_NETWORK_PREFIX : parameters.get(0);
      _currContainerName = _workHelper.initNetwork(null, containerPrefix);
    }
    if (_currContainerName == null) {
      _logger.errorf("Could not init network\n");
      return false;
    }
    _logger.output("Active network is set");
    _logger.infof(" to  %s\n", _currContainerName);
    _logger.output("\n");
    return true;
  }

  private void initHelpers() {
    switch (_settings.getRunMode()) {
      case batch:
      case interactive:
        break;

      case genquestions:
      default:
        return;
    }

    _workHelper = new BfCoordWorkHelper(_logger, _settings);

    int numTries = 0;

    while (true) {
      try {
        numTries++;
        boolean exceededNumTriesWarningThreshold = numTries > NUM_TRIES_WARNING_THRESHOLD;
        if (_workHelper.isReachable(exceededNumTriesWarningThreshold)) {
          // print this message only we might have printed unable to
          // connect message earlier
          if (exceededNumTriesWarningThreshold) {
            _logger.outputf("Connected to coordinator after %d tries\n", numTries);
          }
          break;
        }
        Thread.sleep(1 * 1000); // 1 second
      } catch (Exception e) {
        _logger.errorf(
            "Exeption while checking reachability to coordinator: %s",
            Throwables.getStackTraceAsString(e));
        System.exit(1);
      }
    }
  }

  private boolean initOrAddAnalysis(
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean newAnalysis) {
    Command command = newAnalysis ? Command.INIT_ANALYSIS : Command.ADD_ANALYSIS_QUESTIONS;
    if (!isValidArgument(options, parameters, 0, 2, 2, command)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    String analysisName = parameters.get(0);
    String questionsPathStr = parameters.get(1);

    Map<String, String> questionMap = new TreeMap<>();
    LoadQuestionAnswerElement ae = new LoadQuestionAnswerElement();

    try {
      // loading questions for the analysis
      Multimap<String, String> analysisQuestions = loadQuestionsFromDir(questionsPathStr);
      Answer answer = new Answer();
      answer.addAnswerElement(ae);
      mergeQuestions(analysisQuestions, questionMap, ae);

      String answerStringToPrint;
      if (_settings.getPrettyPrintAnswers()) {
        answerStringToPrint = answer.prettyPrint();
      } else {
        try {
          answerStringToPrint = BatfishObjectMapper.writePrettyString(answer);
        } catch (JsonProcessingException e) {
          throw new BatfishException("Could not write answer element as string", e);
        }
      }
      _logger.output(answerStringToPrint);
    } catch (BatfishException e) {
      // failure in loading the questions results in failure of loading of analysis
      return false;
    }

    String analysisJsonString = "{}";
    try {
      JSONObject jObject = new JSONObject();
      for (String qName : questionMap.keySet()) {
        jObject.put(qName, new JSONObject(questionMap.get(qName)));
      }
      analysisJsonString = jObject.toString(1);
    } catch (JSONException e) {
      throw new BatfishException("Failed to get JSONObject for analysis", e);
    }

    Path analysisFile = createTempFile("analysis", analysisJsonString);

    boolean result =
        _workHelper.configureAnalysis(
            _currContainerName,
            newAnalysis,
            analysisName,
            analysisFile.toAbsolutePath().toString(),
            null);

    if (analysisFile != null) {
      CommonUtil.deleteIfExists(analysisFile);
    }

    logOutput(outWriter, "Output of configuring analysis " + analysisName + ": " + result + "\n");
    return result;
  }

  private boolean initSnapshot(
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta) {
    Command command = delta ? Command.INIT_REFERENCE_SNAPSHOT : Command.INIT_SNAPSHOT;
    if (!isValidArgument(options, parameters, 1, 1, 2, command)) {
      return false;
    }

    boolean autoAnalyze = false;
    if (options.size() == 1) {
      if (options.get(0).equals("-autoanalyze")) {
        autoAnalyze = true;
      } else {
        _logger.errorf("Unknown option: %s\n", options.get(0));
        printUsage(command);
        return false;
      }
    }

    String testrigLocation = parameters.get(0);
    String testrigName =
        (parameters.size() > 1) ? parameters.get(1) : DEFAULT_SNAPSHOT_PREFIX + UUID.randomUUID();

    // initialize the container if it hasn't been init'd before
    if (!isSetContainer(false)) {
      _currContainerName = _workHelper.initNetwork(null, DEFAULT_NETWORK_PREFIX);
      if (_currContainerName == null) {
        _logger.errorf("Could not init network\n");
        return false;
      }
      _logger.output("Init'ed and set active network");
      _logger.infof(" to %s\n", _currContainerName);
      _logger.output("\n");
    }

    if (!uploadTestrig(testrigLocation, testrigName, autoAnalyze)) {
      unsetTestrig(delta);
      return false;
    }
    _logger.output("Uploaded snapshot.\n");

    if (!autoAnalyze) {
      _logger.output("Parsing now.\n");
      WorkItem wItemParse = WorkItemBuilder.getWorkItemParse(_currContainerName, testrigName);

      if (!execute(wItemParse, outWriter)) {
        unsetTestrig(delta);
        return false;
      }
    }

    if (!delta) {
      _currTestrig = testrigName;
      _logger.infof("Current snapshot is now %s\n", _currTestrig);
    } else {
      _currDeltaTestrig = testrigName;
      _logger.infof("Reference snapshot is now %s\n", _currDeltaTestrig);
    }

    return true;
  }

  private void initTracer() {
    GlobalTracer.register(
        new com.uber.jaeger.Configuration(
                _settings.getServiceName(),
                new SamplerConfiguration(ConstSampler.TYPE, 1),
                new ReporterConfiguration(
                    false,
                    _settings.getTracingAgentHost(),
                    _settings.getTracingAgentPort(),
                    /* flush interval in ms */ 1000,
                    /* max buffered Spans */ 10000))
            .getTracer());
  }

  private boolean isSetContainer(boolean printError) {
    if (!_settings.getSanityCheck()) {
      return true;
    }

    if (_currContainerName == null) {
      if (printError) {
        _logger.errorf("Active network is not set\n");
      }
      return false;
    }

    return true;
  }

  private boolean isDeltaReady() {
    if (!_settings.getSanityCheck()) {
      return true;
    }

    if (_currDeltaTestrig == null) {
      _logger.errorf("Active delta snapshot is not set\n");
      return false;
    }
    return true;
  }

  private boolean isSetTestrig() {
    if (!_settings.getSanityCheck()) {
      return true;
    }

    if (_currTestrig == null) {
      _logger.errorf("Active snapshot is not set.\n");
      _logger.errorf(
          "Specify snapshot on command line (-%s <snapshotdir>) or use command (%s <snapshotdir>)\n",
          Settings.ARG_SNAPSHOT_DIR, Command.INIT_SNAPSHOT.commandName());
      return false;
    }
    return true;
  }

  private boolean isValidArgument(
      List<String> options,
      List<String> parameters,
      int maxNumOptions,
      int minNumParas,
      int maxNumParas,
      Command command) {
    if (options.size() > maxNumOptions
        || (parameters.size() < minNumParas)
        || (parameters.size() > maxNumParas)) {
      _logger.errorf("Invalid arguments: %s %s\n", options, parameters);
      printUsage(command);
      return false;
    }
    return true;
  }

  private boolean killWork(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.KILL_WORK)) {
      return false;
    }
    UUID workId = UUID.fromString(parameters.get(0));
    boolean killed = _workHelper.killWork(workId);
    _logger.outputf("Killed: %s\n", killed);
    return killed;
  }

  private boolean listAnalyses(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.LIST_ANALYSES)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    JSONObject analysisList = _workHelper.listAnalyses(_currContainerName);
    logOutput(outWriter, String.format("Found %d analyses\n", analysisList.length()));

    try {
      logOutput(outWriter, analysisList.toString(1));
    } catch (JSONException e) {
      throw new BatfishException("Failed to print analysis list", e);
    }

    // if (analysisList != null) {
    // Iterator<?> aIterator = analysisList.keys();
    // while (aIterator.hasNext()) {
    // String aName = (String) aIterator.next();
    // _logger.outputf("Analysis: %s\n", aName);
    //
    // try {
    // JSONObject questionList = analysisList.getJSONObject(aName);
    // _logger.outputf("Found %d questions\n", questionList.length());
    //
    // Iterator<?> qIterator = questionList.keys();
    // while (qIterator.hasNext()) {
    // String qName = (String) qIterator.next();
    // _logger.outputf(" Question: %s\n", qName);
    //
    // JSONObject questionJson = questionList.getJSONObject(qName);
    // _logger.outputf("%s\n", questionJson.toString(1));
    // }
    //
    // }
    // catch (JSONException e) {
    // throw new BatfishException("Failed to process analysis list", e);
    // }
    // }
    // }

    return true;
  }

  private boolean listNetworks(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.LIST_NETWORKS)) {
      return false;
    }
    String[] containerList = _workHelper.listNetworks();
    _logger.outputf("Networks: %s\n", Arrays.toString(containerList));
    return true;
  }

  private boolean listIncompleteWork(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.LIST_INCOMPLETE_WORK)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }
    List<WorkStatus> workList = _workHelper.listIncompleteWork(_currContainerName);
    _logger.outputf("Incomplete works: %s\n", workList);

    return true;
  }

  private boolean listQuestions(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.LIST_QUESTIONS)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }
    String[] questionList = _workHelper.listQuestions(_currContainerName, _currTestrig);
    _logger.outputf("Questions: %s\n", Arrays.toString(questionList));
    return true;
  }

  private boolean listSnapshots(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 1, 0, 0, Command.LIST_SNAPSHOTS)) {
      return false;
    }

    boolean showMetadata = true;
    if (options.size() == 1) {
      if (options.get(0).equals("-nometadata")) {
        showMetadata = false;
      } else {
        _logger.errorf("Unknown option: %s\n", options.get(0));
        printUsage(Command.LIST_SNAPSHOTS);
        return false;
      }
    }

    JSONArray testrigArray = _workHelper.listSnapshots(_currContainerName);
    if (testrigArray != null) {
      for (int index = 0; index < testrigArray.length(); index++) {
        try {
          JSONObject jObjTestrig = testrigArray.getJSONObject(index);
          String name = jObjTestrig.getString(CoordConsts.SVC_KEY_TESTRIG_NAME);
          String info = jObjTestrig.getString(CoordConsts.SVC_KEY_TESTRIG_INFO);
          logOutput(outWriter, String.format("Testrig: %s\n%s\n", name, info));
          if (showMetadata) {
            String metadata = jObjTestrig.getString(CoordConsts.SVC_KEY_TESTRIG_METADATA);
            logOutput(outWriter, String.format("TestrigMetadata: %s\n", metadata));
          }
        } catch (JSONException e) {
          throw new BatfishException("Unexpected packaging of testrig data", e);
        }
      }
    }
    return true;
  }

  /**
   * Loads question from a given file
   *
   * @param questionFile File containing the question JSON
   * @return question loaded as a {@link JSONObject}
   * @throws BatfishException if question does not have instanceName or question cannot be parsed
   */
  static JSONObject loadQuestionFromFile(Path questionFile) {
    String questionText = CommonUtil.readFile(questionFile);
    return loadQuestionFromText(questionText, questionFile.toString());
  }

  /**
   * Loads question from a JSON
   *
   * @param questionText Question JSON Text
   * @param questionSource JSON key of question or file path of JSON
   * @return question loaded as a {@link JSONObject}
   * @throws BatfishException if question does not have instanceName or question cannot be parsed
   */
  static JSONObject loadQuestionFromText(String questionText, String questionSource) {
    try {
      JSONObject questionObj = new JSONObject(questionText);
      if (questionObj.has(BfConsts.PROP_INSTANCE) && !questionObj.isNull(BfConsts.PROP_INSTANCE)) {
        JSONObject instanceDataObj = questionObj.getJSONObject(BfConsts.PROP_INSTANCE);
        String instanceDataStr = instanceDataObj.toString();
        InstanceData instanceData =
            BatfishObjectMapper.mapper()
                .readValue(instanceDataStr, new TypeReference<InstanceData>() {});
        validateInstanceData(instanceData);
        return questionObj;
      } else {
        throw new BatfishException(
            String.format("Question in %s has no instance data", questionSource));
      }
    } catch (JSONException | IOException e) {
      throw new BatfishException("Failed to process question", e);
    }
  }

  private boolean loadQuestions(
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      Map<String, String> bfq) {

    // checking the arguments and options
    if (!isValidArgument(options, parameters, 1, 0, 1, Command.LOAD_QUESTIONS)) {
      return false;
    }
    boolean loadRemote = false;
    if (options.size() == 1) {
      if (options.get(0).equals("-loadremote")) {
        loadRemote = true;
      } else {
        _logger.errorf("Unknown option: %s\n", options.get(0));
        printUsage(Command.LOAD_QUESTIONS);
        return false;
      }
    }

    // init answer and answer element
    Answer answer = new Answer();
    LoadQuestionAnswerElement ae = new LoadQuestionAnswerElement();
    answer.addAnswerElement(ae);

    // try to load remote questions if no local disk path is passed or loadremote is forced
    if ((parameters.isEmpty() || loadRemote) && _workHelper != null) {
      JSONObject remoteQuestionsJson = _workHelper.getQuestionTemplates();
      Multimap<String, String> remoteQuestions = loadQuestionsFromServer(remoteQuestionsJson);
      // merging remote questions to bfq and updating answer element
      mergeQuestions(remoteQuestions, bfq, ae);
    }

    // try to load local questions whenever local disk path is provided
    if (!parameters.isEmpty()) {
      Multimap<String, String> localQuestions = loadQuestionsFromDir(parameters.get(0));
      // merging local questions to bfq and updating answer element
      mergeQuestions(localQuestions, bfq, ae);
    }

    // outputting the final answer
    String answerStringToPrint;
    if (outWriter == null && _settings.getPrettyPrintAnswers()) {
      answerStringToPrint = answer.prettyPrint();
    } else {
      try {
        answerStringToPrint = BatfishObjectMapper.writePrettyString(answer);
      } catch (JsonProcessingException e) {
        throw new BatfishException("Could not write answer element as string", e);
      }
    }
    logOutput(outWriter, answerStringToPrint);

    return true;
  }

  /**
   * Loads questions from a JSON containing the questions
   *
   * @param questionTemplatesJson {@link JSONObject} with question key and question content Json
   * @return loadedQuestions {@link Multimap} containing loaded question names and content, empty if
   *     questionTemplatesJson is null
   * @throws BatfishException if loading of any of the questions is not successful, or if
   *     questionTemplatesJson cannot be deserialized
   */
  static Multimap<String, String> loadQuestionsFromServer(JSONObject questionTemplatesJson) {
    try {
      Multimap<String, String> loadedQuestions = HashMultimap.create();
      if (questionTemplatesJson == null) {
        return loadedQuestions;
      }
      Map<String, String> questionsMap =
          BatfishObjectMapper.mapper()
              .readValue(
                  questionTemplatesJson.toString(), new TypeReference<Map<String, String>>() {});

      for (Entry<String, String> question : questionsMap.entrySet()) {
        JSONObject questionJSON = loadQuestionFromText(question.getValue(), question.getKey());
        loadedQuestions.put(
            getQuestionName(questionJSON, question.getKey()), questionJSON.toString());
      }
      return loadedQuestions;
    } catch (IOException e) {
      throw new BatfishException("Could not load remote questions", e);
    }
  }

  /**
   * Loads questions from a local directory containing questions
   *
   * @param questionsPathStr Path of directory
   * @return loadedQuestions {@link Multimap} containing loaded question names and content
   * @throws BatfishException if loading of any of the question is not successful or if cannot walk
   *     the directory provided
   */
  static Multimap<String, String> loadQuestionsFromDir(String questionsPathStr) {
    Path questionsPath = Paths.get(questionsPathStr);
    SortedSet<Path> jsonQuestionFiles = new TreeSet<>();
    try {
      Files.walkFileTree(
          questionsPath,
          EnumSet.of(FOLLOW_LINKS),
          1,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
              String filename = file.getFileName().toString();
              if (filename.endsWith(".json")) {
                jsonQuestionFiles.add(file);
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      throw new BatfishException("Failed to visit questions dir", e);
    }
    Multimap<String, String> loadedQuestions = HashMultimap.create();
    for (Path jsonQuestionFile : jsonQuestionFiles) {
      JSONObject questionJSON = loadQuestionFromFile(jsonQuestionFile);
      loadedQuestions.put(
          getQuestionName(questionJSON, jsonQuestionFile.toString()), questionJSON.toString());
    }
    return loadedQuestions;
  }

  private void logOutput(FileWriter outWriter, String message) {
    if (outWriter == null) {
      _logger.output(message);
    } else {
      try {
        outWriter.write(message);
      } catch (IOException e) {
        throw new BatfishException("Failed to log output to outWriter", e);
      }
    }
  }

  /**
   * Merges questions in source map into questions in destination map and overwrites question with
   * same keys
   *
   * @param sourceMap {@link Multimap} containing question names and content
   * @param destinationMap {@link Map} containing the merged questions
   * @param ae {@link LoadQuestionAnswerElement} containing the merged questions information
   */
  static void mergeQuestions(
      Multimap<String, String> sourceMap,
      Map<String, String> destinationMap,
      LoadQuestionAnswerElement ae) {
    // merging remote questions
    for (String questionName : sourceMap.keySet()) {
      sourceMap
          .get(questionName)
          .forEach(
              questionContent ->
                  updateLoadedQuestionsInfo(questionName, questionContent, destinationMap, ae));
    }
  }

  /**
   * Update info in {@link LoadQuestionAnswerElement} and loaded questions {@link Map} for a given
   * question
   *
   * @param questionName Question name
   * @param questionContent Question content string
   * @param loadedQuestions {@link Map containing the loaded questions}
   * @param ae {@link LoadQuestionAnswerElement} where info has to be updated
   */
  static void updateLoadedQuestionsInfo(
      String questionName,
      String questionContent,
      Map<String, String> loadedQuestions,
      LoadQuestionAnswerElement ae) {
    // adding question name in added list if not present else add in replaced list
    if (loadedQuestions.containsKey(questionName.toLowerCase())) {
      ae.getReplaced().add(questionName);
    } else {
      ae.getAdded().add(questionName);
    }
    loadedQuestions.put(questionName.toLowerCase(), questionContent);
    ae.setNumLoaded(ae.getNumLoaded() + 1);
  }

  private Map<String, JsonNode> parseParams(String paramsLine) {
    String jsonParamsStr = "{ " + paramsLine + " }";
    Map<String, JsonNode> parameters;
    try {
      parameters =
          BatfishObjectMapper.mapper()
              .readValue(
                  new JSONObject(jsonParamsStr).toString(),
                  new TypeReference<Map<String, JsonNode>>() {});
      return parameters;
    } catch (JSONException | IOException e) {
      throw new BatfishException(
          "Failed to parse parameters. (Are all key-value pairs separated by commas? Are all "
              + "values valid JSON?)",
          e);
    }
  }

  private boolean pollWork(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.POLL_WORK)) {
      return false;
    }
    UUID workId = UUID.fromString(parameters.get(0));
    return pollWork(workId);
  }

  private boolean pollWork(UUID wItemId) {

    Pair<WorkStatusCode, String> response;
    try (ActiveSpan workStatusSpan =
        GlobalTracer.get().buildSpan("Waiting for work status").startActive()) {
      assert workStatusSpan != null; // avoid unused warning
      // Poll the work item until it finishes or fails.
      response = _workHelper.getWorkStatus(wItemId);
      if (response == null) {
        return false;
      }

      WorkStatusCode status = response.getFirst();
      Backoff backoff = Backoff.builder().withMaximumBackoff(Duration.ofSeconds(1)).build();
      while (!status.isTerminated() && backoff.hasNext()) {
        printWorkStatusResponse(response, false);
        try {
          Thread.sleep(backoff.nextBackoff().toMillis());
        } catch (InterruptedException e) {
          throw new BatfishException("Interrupted while waiting for work item to complete", e);
        }
        response = _workHelper.getWorkStatus(wItemId);
        if (response == null) {
          return false;
        }
        status = response.getFirst();
      }
      printWorkStatusResponse(response, false);
    }
    return true;
  }

  private boolean pollWorkAndGetAnswer(WorkItem wItem, @Nullable FileWriter outWriter) {

    boolean pollResult = pollWork(wItem.getId());
    if (!pollResult) {
      return false;
    }
    // get the answer
    String ansFileName = wItem.getId() + BfConsts.SUFFIX_ANSWER_JSON_FILE;
    String downloadedAnsFile =
        _workHelper.getObject(wItem.getNetwork(), wItem.getSnapshot(), ansFileName);
    if (downloadedAnsFile == null) {
      _logger.errorf("Failed to get answer file %s. (Was work killed?)\n", ansFileName);
    } else {
      String answerString = CommonUtil.readFile(Paths.get(downloadedAnsFile));

      // Check if we need to make things pretty
      // Don't if we are writing to FileWriter, because we need valid JSON in
      // that case
      String answerStringToPrint = answerString;
      if (outWriter == null && _settings.getPrettyPrintAnswers()) {
        try {
          Answer answer = BatfishObjectMapper.mapper().readValue(answerString, Answer.class);
          answerStringToPrint = answer.prettyPrint();
        } catch (IOException e) {
          _logger.warnf(
              "Using Json for pretty printing because could not deserialize response as %s: %s",
              Answer.class.getSimpleName(), e.getMessage());
        }
      }

      logOutput(outWriter, answerStringToPrint);

      // tests serialization/deserialization when running in debug mode
      if (_logger.getLogLevel() >= BatfishLogger.LEVEL_DEBUG) {
        try {
          ObjectMapper reader = BatfishObjectMapper.mapper();
          Answer answer = reader.readValue(answerString, Answer.class);

          String newAnswerString = BatfishObjectMapper.writeString(answer);
          JsonNode tree = reader.readTree(answerString);
          JsonNode newTree = reader.readTree(newAnswerString);
          if (!CommonUtil.checkJsonEqual(tree, newTree)) {
            // if (!tree.equals(newTree)) {
            _logger.errorf(
                "Original and recovered Json are different. Recovered = %s\n", newAnswerString);
          }
        } catch (Exception e) {
          _logger.outputf("Could NOT deserialize Json to Answer: %s\n", e.getMessage());
        }
      }
    }
    // get and print the log when in debugging mode
    if (_logger.getLogLevel() >= BatfishLogger.LEVEL_DEBUG) {
      _logger.output("---------------- Service Log --------------\n");
      String logFileName = wItem.getId() + BfConsts.SUFFIX_LOG_FILE;
      String downloadedFileStr =
          _workHelper.getObject(wItem.getNetwork(), wItem.getSnapshot(), logFileName);

      if (downloadedFileStr == null) {
        _logger.errorf("Failed to get log file %s\n", logFileName);
        return false;
      } else {
        Path downloadedFile = Paths.get(downloadedFileStr);
        CommonUtil.outputFileLines(downloadedFile, _logger::output);
      }
    }
    return true;
  }

  private void printUsage() {
    for (Command cmd : Command.getUsageMap().keySet()) {
      printUsage(cmd);
    }
  }

  private void printUsage(Command command) {
    Pair<String, String> usage = Command.getUsageMap().get(command);
    _logger.outputf("%s %s\n\t%s\n\n", command.commandName(), usage.getFirst(), usage.getSecond());
  }

  private void printWorkStatusResponse(
      Pair<WorkStatusCode, String> response, boolean unconditionalPrint) {

    if (unconditionalPrint || _logger.getLogLevel() >= BatfishLogger.LEVEL_INFO) {
      WorkStatusCode status = response.getFirst();
      _logger.outputf("status: %s\n", status);

      Task task;
      try {
        task = BatfishObjectMapper.mapper().readValue(response.getSecond(), Task.class);
      } catch (IOException e) {
        _logger.errorf("Could not deserialize task object: %s\n", e);
        return;
      }

      if (task == null) {
        _logger.outputf(".... no task information\n");
        return;
      }

      List<Batch> batches = task.getBatches();

      // when log level is INFO, we only print the last batch
      // else print all
      for (int i = 0; i < batches.size(); i++) {
        if (i == batches.size() - 1 || status.isTerminated()) {
          _logger.outputf(".... %s\n", batches.get(i));
        } else {
          _logger.debugf(".... %s\n", batches.get(i));
        }
      }
      if (status.isTerminated()) {
        _logger.outputf(".... %s: %s\n", task.getTerminated(), status);
      }
    }
  }

  private boolean processCommand(String command) {
    String line = command.trim();
    if (line.length() == 0 || line.startsWith("#")) {
      return true;
    }
    _logger.debugf("Doing command: %s\n", line);
    String[] words = line.split("\\s+");
    return processCommand(words, null);
  }

  boolean processCommand(String[] words, @Nullable FileWriter outWriter) {
    Command command;
    try {
      command = Command.fromName(words[0]);
    } catch (BatfishException e) {
      _logger.errorf("Command failed: %s\n", e.getMessage());
      return false;
    }

    List<String> options = getCommandOptions(words);
    List<String> parameters = getCommandParameters(words, options.size());

    try (ActiveSpan span = GlobalTracer.get().buildSpan(command.commandName()).startActive()) {
      assert span != null; // make span not show up as unused.
      return processCommand(command, words, outWriter, options, parameters);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private boolean processCommand(
      Command command,
      String[] words,
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters)
      throws Exception {
    switch (command) {
      case ADD_ANALYSIS_QUESTIONS:
        return initOrAddAnalysis(outWriter, options, parameters, false);
      case ADD_BATFISH_OPTION:
        return addBatfishOption(words, options, parameters);
      case ANSWER:
        return answer(words, outWriter, options, parameters, false);
      case ANSWER_REFERENCE:
        return answer(words, outWriter, options, parameters, true);
      case AUTOCOMPLETE:
        return autoComplete(options, parameters);
      case CAT:
        return cat(words);
      case CHECK_API_KEY:
        return checkApiKey(options, parameters);
      case CLEAR_SCREEN:
        return clearScreen(options, parameters);
      case CONFIGURE_TEMPLATE:
        return configureTemplate(words, outWriter, options, parameters);
      case DEBUG_DELETE:
        return debugDelete(outWriter, options, parameters);
      case DEBUG_GET:
        return debugGet(outWriter, options, parameters);
      case DEBUG_POST:
        return debugPost(outWriter, options, parameters);
      case DEBUG_PUT:
        return debugPut(outWriter, options, parameters);
      case DEL_ANALYSIS:
        return delAnalysis(outWriter, options, parameters);
      case DEL_ANALYSIS_QUESTIONS:
        return delAnalysisQuestions(outWriter, options, parameters);
      case DEL_BATFISH_OPTION:
        return delBatfishOption(options, parameters);
      case DEL_NETWORK:
        return delNetwork(options, parameters);
      case DEL_QUESTION:
        return delQuestion(options, parameters);
      case DEL_SNAPSHOT:
        return delSnapshot(outWriter, options, parameters);
      case DIR:
        return dir(options, parameters);
      case ECHO:
        return echo(words);
      case GEN_DP:
        return generateDataplane(outWriter, options, parameters);
      case GEN_REFERENCE_DP:
        return generateReferenceDataplane(outWriter, options, parameters);
      case GET:
        return get(words, outWriter, options, parameters, false);
      case GET_CONFIGURATION:
        return getConfiguration(options, parameters);
      case GET_REFERENCE:
        return get(words, outWriter, options, parameters, true);
      case GET_ANALYSIS_ANSWERS:
        return getAnalysisAnswers(outWriter, options, parameters, false, false);
      case GET_ANALYSIS_ANSWERS_REFERENCE:
        return getAnalysisAnswers(outWriter, options, parameters, true, false);
      case GET_ANALYSIS_ANSWERS_DIFFERENTIAL:
        return getAnalysisAnswers(outWriter, options, parameters, false, true);
      case GET_ANSWER:
        return getAnswer(outWriter, options, parameters, false, false);
      case GET_ANSWER_REFERENCE:
        return getAnswer(outWriter, options, parameters, true, false);
      case GET_ANSWER_DIFFERENTIAL:
        return getAnswer(outWriter, options, parameters, false, true);
      case GET_NETWORK:
        return getNetwork(options, parameters);
      case GET_OBJECT:
        return getObject(outWriter, options, parameters, false);
      case GET_OBJECT_REFERENCE:
        return getObject(outWriter, options, parameters, false);
      case GET_QUESTION_TEMPLATES:
        return getQuestionTemplates(options, parameters);
      case GET_WORK_STATUS:
        return getWorkStatus(outWriter, options, parameters);
      case HELP:
        return help(options, parameters);
      case INIT_ANALYSIS:
        return initOrAddAnalysis(outWriter, options, parameters, true);
      case INIT_REFERENCE_SNAPSHOT:
        return initSnapshot(outWriter, options, parameters, true);
      case INIT_NETWORK:
        return initNetwork(options, parameters);
      case INIT_SNAPSHOT:
        return initSnapshot(outWriter, options, parameters, false);
      case KILL_WORK:
        return killWork(options, parameters);
      case LIST_ANALYSES:
        return listAnalyses(outWriter, options, parameters);
      case LIST_INCOMPLETE_WORK:
        return listIncompleteWork(options, parameters);
      case LIST_NETWORKS:
        return listNetworks(options, parameters);
      case LIST_QUESTIONS:
        return listQuestions(options, parameters);
      case LIST_SNAPSHOTS:
        return listSnapshots(outWriter, options, parameters);
      case LOAD_QUESTIONS:
        return loadQuestions(outWriter, options, parameters, _bfq);
      case POLL_WORK:
        return pollWork(options, parameters);
      case PROMPT:
        return prompt(options, parameters);
      case PWD:
        return pwd(options, parameters);
      case RUN_ANALYSIS:
        return runAnalysis(outWriter, options, parameters, false, false);
      case RUN_ANALYSIS_REFERENCE:
        return runAnalysis(outWriter, options, parameters, true, false);
      case RUN_ANALYSIS_DIFFERENTIAL:
        return runAnalysis(outWriter, options, parameters, false, true);
      case SET_BACKGROUND_EXECUCTION:
        return setBackgroundExecution(options, parameters);
      case SET_BATFISH_LOGLEVEL:
        return setBatfishLogLevel(options, parameters);
      case SET_FIXED_WORKITEM_ID:
        return setFixedWorkItemId(options, parameters);
      case SET_REFERENCE_SNAPSHOT:
        return setReferenceSnapshot(options, parameters);
      case SET_LOGLEVEL:
        return setLogLevel(options, parameters);
      case SET_NETWORK:
        return setNetwork(options, parameters);
      case SET_PRETTY_PRINT:
        return setPrettyPrint(options, parameters);
      case SET_SNAPSHOT:
        return setSnapshot(options, parameters);
      case SHOW_API_KEY:
        return showApiKey(options, parameters);
      case SHOW_BATFISH_LOGLEVEL:
        return showBatfishLogLevel(options, parameters);
      case SHOW_BATFISH_OPTIONS:
        return showBatfishOptions(options, parameters);
      case SHOW_COORDINATOR_HOST:
        return showCoordinatorHost(options, parameters);
      case SHOW_REFERENCE_SNAPSHOT:
        return showReferenceSnapshot(options, parameters);
      case SHOW_LOGLEVEL:
        return showLogLevel(options, parameters);
      case SHOW_NETWORK:
        return showNetwork(options, parameters);
      case SHOW_SNAPSHOT:
        return showSnapshot(options, parameters);
      case SHOW_VERSION:
        return showVersion(options, parameters);
      case SYNC_SNAPSHOTS_SYNC_NOW:
        return syncSnapshotsSyncNow(options, parameters);
      case SYNC_SNAPSHOTS_UPDATE_SETTINGS:
        return syncSnapshotsUpdateSettings(words, options, parameters);
      case TEST:
        return test(options, parameters);
      case UPLOAD_CUSTOM_OBJECT:
        return uploadCustomObject(options, parameters);
      case VALIDATE_TEMPLATE:
        return validateTemplate(words, outWriter, options, parameters);

      case EXIT:
      case QUIT:
        return exit(options, parameters);

      default:
        _logger.errorf("Unsupported command %s\n", words[0]);
        _logger.error("Type 'help' to see the list of valid commands\n");
        return false;
    }
  }

  private boolean debugDelete(FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEBUG_DELETE)) {
      return false;
    }
    String urlTail = parameters.get(0);
    try {
      return _workHelper.debugV2(outWriter, HttpMethod.DELETE, urlTail, null, null);
    } catch (IOException e) {
      _logger.error(Throwables.getStackTraceAsString(e));
      return false;
    }
  }

  private boolean debugGet(FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEBUG_GET)) {
      return false;
    }
    String urlTail = parameters.get(0);
    try {
      return _workHelper.debugV2(outWriter, HttpMethod.GET, urlTail, null, null);
    } catch (IOException e) {
      _logger.error(Throwables.getStackTraceAsString(e));
      return false;
    }
  }

  private boolean debugPost(FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 2, 2, 2, Command.DEBUG_POST)) {
      return false;
    }
    boolean file = false;
    boolean raw = false;
    for (String option : options) {
      switch (option) {
        case "-file":
          file = true;
          break;
        case "-raw":
          raw = true;
          break;
        default:
          _logger.errorf("Unknown option: %s\n", options.get(0));
          printUsage(Command.DEBUG_POST);
          return false;
      }
    }
    String urlTail = parameters.get(0);
    String entityParam = parameters.get(1);
    MediaType mediaType;
    Object entity;
    try (Closer closer = Closer.create()) {
      if (file) {
        InputStream inputStream = closer.register(Files.newInputStream(Paths.get(entityParam)));
        if (raw) {
          mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
          entity = inputStream;
        } else {
          mediaType = MediaType.APPLICATION_JSON_TYPE;
          entity = IOUtils.toString(inputStream, UTF_8);
        }
      } else {
        mediaType = MediaType.APPLICATION_JSON_TYPE;
        entity = BatfishObjectMapper.mapper().readTree(entityParam);
      }
      return _workHelper.debugV2(outWriter, HttpMethod.POST, urlTail, entity, mediaType);
    } catch (IOException e) {
      _logger.error(Throwables.getStackTraceAsString(e));
      return false;
    }
  }

  private boolean debugPut(FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 2, 2, 2, Command.DEBUG_PUT)) {
      return false;
    }
    boolean file = false;
    boolean raw = false;
    for (String option : options) {
      switch (option) {
        case "-file":
          file = true;
          break;
        case "-raw":
          raw = true;
          break;
        default:
          _logger.errorf("Unknown option: %s\n", options.get(0));
          printUsage(Command.DEBUG_PUT);
          return false;
      }
    }
    String urlTail = parameters.get(0);
    String entityParam = parameters.get(1);
    MediaType mediaType;
    Object entity;
    try (Closer closer = Closer.create()) {
      if (file) {
        InputStream inputStream = closer.register(Files.newInputStream(Paths.get(entityParam)));
        if (raw) {
          mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
          entity = inputStream;
        } else {
          mediaType = MediaType.APPLICATION_JSON_TYPE;
          entity = IOUtils.toString(inputStream, UTF_8);
        }
      } else {
        mediaType = MediaType.APPLICATION_JSON_TYPE;
        entity = BatfishObjectMapper.mapper().readTree(entityParam);
      }
      return _workHelper.debugV2(outWriter, HttpMethod.PUT, urlTail, entity, mediaType);
    } catch (IOException e) {
      _logger.error(Throwables.getStackTraceAsString(e));
      return false;
    }
  }

  /** Processes a batch of commands and returns true iff every command succeeded. */
  private boolean processCommands(List<String> commands) {
    boolean allPass = true;
    for (String command : commands) {
      allPass = processCommand(command) && allPass;
    }
    return allPass;
  }

  private boolean prompt(List<String> options, List<String> parameters) throws IOException {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.PROMPT)) {
      return false;
    }
    if (_settings.getRunMode() == RunMode.interactive) {
      _logger.output("\n\n[Press enter to proceed]\n\n");
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      in.readLine();
    }
    return true;
  }

  private boolean pwd(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.PWD)) {
      return false;
    }
    final String dir = System.getProperty("user.dir");
    _logger.outputf("working directory = %s\n", dir);
    return true;
  }

  private List<String> readCommands(Path startupFilePath) {
    List<String> commands = null;
    try {
      commands = Files.readAllLines(startupFilePath, StandardCharsets.US_ASCII);
    } catch (Exception e) {
      System.err.printf(
          "Exception reading command file %s: %s\n",
          _settings.getBatchCommandFile(), e.getMessage());
      System.exit(1);
    }
    return commands;
  }

  public void run(List<String> initialCommands) {
    if (_settings.getTracingEnable() && !GlobalTracer.isRegistered()) {
      initTracer();
    }
    loadPlugins();
    initHelpers();
    _logger.debugf(
        "Will use coordinator at %s://%s\n",
        (_settings.getSslDisable()) ? "http" : "https", _settings.getCoordinatorHost());

    if (!processCommands(initialCommands)) {
      return;
    }

    // set container if specified
    if (_settings.getContainerId() != null
        && !processCommand(Command.SET_NETWORK.commandName() + "  " + _settings.getContainerId())) {
      return;
    }

    // set testrig if dir or id is specified
    if (_settings.getSnapshotDir() != null) {
      if (_settings.getSnapshotId() != null) {
        System.err.println("org.batfish.client: Cannot supply both snapshotDir and snapshotId.");
        System.exit(1);
      }
      if (!processCommand(Command.INIT_SNAPSHOT.commandName() + " " + _settings.getSnapshotDir())) {
        return;
      }
    }
    if (_settings.getSnapshotId() != null
        && !processCommand(Command.SET_SNAPSHOT.commandName() + "  " + _settings.getSnapshotId())) {
      return;
    }

    switch (_settings.getRunMode()) {
      case batch:
        {
          runBatchFile();
          break;
        }

      case genquestions:
        generateQuestions();
        break;

      case interactive:
        {
          runStartupFile();
          runInteractive();
          break;
        }

      default:
        System.err.println("org.batfish.client: Unknown run mode.");
        System.exit(1);
    }
  }

  private boolean runAnalysis(
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta,
      boolean differential) {
    Command command =
        differential
            ? Command.RUN_ANALYSIS_DIFFERENTIAL
            : delta ? Command.RUN_ANALYSIS_REFERENCE : Command.RUN_ANALYSIS;
    if (!isValidArgument(options, parameters, 0, 1, 1, command)) {
      return false;
    }
    if (!isSetContainer(true) || !isSetTestrig()) {
      return false;
    }

    String analysisName = parameters.get(0);

    // answer the question
    WorkItem wItemAs =
        WorkItemBuilder.getWorkItemRunAnalysis(
            analysisName, _currContainerName, _currTestrig, _currDeltaTestrig, delta, differential);

    return execute(wItemAs, outWriter);
  }

  private void runBatchFile() {
    Path batchCommandFilePath = Paths.get(_settings.getBatchCommandFile());
    List<String> commands = readCommands(batchCommandFilePath);
    boolean result = processCommands(commands);
    if (!result) {
      System.exit(1);
    }
  }

  private void runInteractive() {
    SignalHandler handler = signal -> _logger.debugf("Client: Ignoring signal: %s\n", signal);
    Signal.handle(new Signal("INT"), handler);
    try {
      while (!_exit) {
        try {
          String rawLine = _reader.readLine("batfish> ");
          if (rawLine == null) {
            break;
          }
          processCommand(rawLine);
        } catch (UserInterruptException e) {
          continue;
        }
      }
    } catch (EndOfFileException e) {
      // ignored
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      try {
        _reader.getHistory().save();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void runStartupFile() {
    Path startupFilePath = Paths.get(System.getenv(ENV_HOME), STARTUP_FILE);
    if (Files.exists(startupFilePath)) {
      List<String> commands = readCommands(startupFilePath);
      boolean result = processCommands(commands);
      if (!result) {
        System.exit(1);
      }
    }
  }

  private boolean setBackgroundExecution(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_BACKGROUND_EXECUCTION)) {
      return false;
    }
    _backgroundExecution = Boolean.valueOf(parameters.get(0));
    _logger.outputf("Changed background execution to %s\n", _backgroundExecution);
    return true;
  }

  private boolean setBatfishLogLevel(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_BATFISH_LOGLEVEL)) {
      return false;
    }
    String logLevelStr = parameters.get(0).toLowerCase();
    if (!BatfishLogger.isValidLogLevel(logLevelStr)) {
      _logger.errorf("Undefined loglevel value: %s\n", logLevelStr);
      return false;
    }
    _settings.setBatfishLogLevel(logLevelStr);
    _logger.outputf("Changed batfish loglevel to %s\n", logLevelStr);
    return true;
  }

  private boolean setNetwork(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_NETWORK)) {
      return false;
    }
    _currContainerName = parameters.get(0);
    _logger.outputf("Active network is now set to %s\n", _currContainerName);
    return true;
  }

  private boolean setReferenceSnapshot(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_REFERENCE_SNAPSHOT)) {
      return false;
    }
    _currDeltaTestrig = parameters.get(0);
    _logger.outputf("Reference snapshot is now %s\n", _currDeltaTestrig);
    return true;
  }

  private boolean setFixedWorkItemId(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_FIXED_WORKITEM_ID)) {
      return false;
    }
    UUID uuid = UUID.fromString(parameters.get(0));
    WorkItem.setFixedUuid(uuid);
    _logger.outputf("Fixed WorkItem UUID to %s\n", uuid);
    return true;
  }

  private boolean setLogLevel(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_LOGLEVEL)) {
      return false;
    }
    String logLevelStr = parameters.get(0).toLowerCase();
    if (!BatfishLogger.isValidLogLevel(logLevelStr)) {
      _logger.errorf("Undefined loglevel value: %s\n", logLevelStr);
      return false;
    }
    _logger.setLogLevel(logLevelStr);
    _settings.setLogLevel(logLevelStr);
    _logger.outputf("Changed client loglevel to %s\n", logLevelStr);
    return true;
  }

  private boolean setPrettyPrint(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_PRETTY_PRINT)) {
      return false;
    }
    String ppStr = parameters.get(0).toLowerCase();
    boolean prettyPrint = Boolean.parseBoolean(ppStr);
    _settings.setPrettyPrintAnswers(prettyPrint);
    _logger.outputf("Set pretty printing answers to %s\n", ppStr);
    return true;
  }

  private boolean setSnapshot(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_SNAPSHOT)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    _currTestrig = parameters.get(0);
    _logger.outputf("Current snapshot is now %s\n", _currTestrig);
    return true;
  }

  private boolean showApiKey(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_API_KEY)) {
      return false;
    }
    _logger.outputf("Current API Key is %s\n", _settings.getApiKey());
    return true;
  }

  private boolean showBatfishLogLevel(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_BATFISH_LOGLEVEL)) {
      return false;
    }
    _logger.outputf("Current batfish log level is %s\n", _settings.getBatfishLogLevel());
    return true;
  }

  private boolean showBatfishOptions(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_BATFISH_OPTIONS)) {
      return false;
    }
    _logger.outputf("There are %d additional batfish options\n", _additionalBatfishOptions.size());
    for (String option : _additionalBatfishOptions.keySet()) {
      _logger.outputf("    %s : %s \n", option, _additionalBatfishOptions.get(option));
    }
    return true;
  }

  private boolean showNetwork(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_NETWORK)) {
      return false;
    }
    _logger.outputf("Current network is %s\n", _currContainerName);
    return true;
  }

  private boolean showCoordinatorHost(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_COORDINATOR_HOST)) {
      return false;
    }
    _logger.outputf("Current coordinator host is %s\n", _settings.getCoordinatorHost());
    return true;
  }

  private boolean showReferenceSnapshot(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_REFERENCE_SNAPSHOT)) {
      return false;
    }
    if (!isDeltaReady()) {
      return false;
    }
    _logger.outputf("Reference snapshot is %s->%s\n", _currDeltaTestrig);
    return true;
  }

  private boolean showLogLevel(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_LOGLEVEL)) {
      return false;
    }
    _logger.outputf("Current client log level is %s\n", _logger.getLogLevelStr());
    return true;
  }

  private boolean showSnapshot(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_SNAPSHOT)) {
      return false;
    }
    if (!isSetTestrig()) {
      return false;
    }
    _logger.outputf("Current snapshot is %s\n", _currTestrig);
    return true;
  }

  private boolean showVersion(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_VERSION)) {
      return false;
    }
    _logger.outputf("Client version is %s\n", Version.getVersion());

    Map<String, String> map = _workHelper.getInfo();

    if (!map.containsKey(CoordConsts.SVC_KEY_VERSION)) {
      _logger.errorf("key '%s' not found in Info\n", CoordConsts.SVC_KEY_VERSION);
      return false;
    }

    String version = map.get(CoordConsts.SVC_KEY_VERSION);
    _logger.outputf("Service version is %s\n", version);
    return true;
  }

  private boolean syncSnapshotsSyncNow(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 1, 1, 1, Command.SYNC_SNAPSHOTS_SYNC_NOW)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    boolean force = false;

    if (options.size() == 1) {
      if (options.get(0).equals("-force")) {
        force = true;
      } else {
        _logger.errorf("Unknown option: %s\n", options.get(0));
        printUsage(Command.SYNC_SNAPSHOTS_SYNC_NOW);
        return false;
      }
    }

    String pluginId = parameters.get(0);

    return _workHelper.syncSnapshotsSyncNow(pluginId, _currContainerName, force);
  }

  private boolean syncSnapshotsUpdateSettings(
      String[] words, List<String> options, List<String> parameters) {
    if (!isValidArgument(
        options, parameters, 0, 1, Integer.MAX_VALUE, Command.SYNC_SNAPSHOTS_UPDATE_SETTINGS)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    String pluginId = parameters.get(0);

    String settingsStr =
        "{" + String.join(" ", Arrays.copyOfRange(words, 2 + options.size(), words.length)) + "}";

    Map<String, String> settings = null;

    try {
      settings =
          BatfishObjectMapper.mapper()
              .readValue(
                  new JSONObject(settingsStr).toString(),
                  new TypeReference<Map<String, String>>() {});
    } catch (JSONException | IOException e) {
      _logger.errorf(
          "Failed to parse parameters. "
              + "(Are all key-value pairs separated by commas? Are all values strings?)\n%s\n",
          e);
      return false;
    }

    return _workHelper.syncSnapshotsUpdateSettings(pluginId, _currContainerName, settings);
  }

  /**
   * Computes a unified diff of the input strings, returning the empty string if the {@code
   * expected} and {@code actual} are equal.
   */
  @Nonnull
  @VisibleForTesting
  static String getPatch(
      String expected, String actual, String expectedFileName, String actualFileName)
      throws DiffException {
    List<String> referenceLines = Arrays.asList(expected.split("\n"));
    List<String> testLines = Arrays.asList(actual.split("\n"));
    Patch<String> patch = DiffUtils.diff(referenceLines, testLines);
    if (patch.getDeltas().isEmpty()) {
      return "";
    } else {
      List<String> patchLines =
          UnifiedDiffUtils.generateUnifiedDiff(
              expectedFileName, actualFileName, referenceLines, patch, 3);
      return StringUtils.join(patchLines, "\n");
    }
  }

  private boolean test(List<String> options, List<String> parameters) throws IOException {
    boolean failingTest = false;
    boolean missingReferenceFile = false;
    int testCommandIndex = 1;
    if (!isValidArgument(options, parameters, 1, 2, Integer.MAX_VALUE, Command.TEST)) {
      return false;
    }

    TestComparisonMode comparisonMode = TestComparisonMode.COMPAREANSWER;
    if (!options.isEmpty()) {
      comparisonMode =
          TestComparisonMode.valueOf(options.get(0).substring(1).toUpperCase()); // remove '-'
    }

    if (parameters.get(testCommandIndex).equals(FLAG_FAILING_TEST)) {
      testCommandIndex++;
      failingTest = true;
    }
    String referenceFileName = parameters.get(0);
    String testFileName = referenceFileName + ".testout";

    String[] testCommand =
        parameters.subList(testCommandIndex, parameters.size()).toArray(new String[0]);

    _logger.debugf("Ref file is %s. \n", referenceFileName, parameters.size());
    _logger.debugf("Test command is %s\n", Arrays.toString(testCommand));

    File referenceFile = new File(referenceFileName);

    if (!referenceFile.exists()) {
      _logger.errorf("Reference file does not exist: %s\n", referenceFileName);
      missingReferenceFile = true;
    }

    // Delete any existing testout filename before running this test.
    Path failedTestoutPath = Paths.get(testFileName);
    CommonUtil.deleteIfExists(failedTestoutPath);

    File testoutFile = Files.createTempFile("test", "out").toFile();
    testoutFile.deleteOnExit();
    FileWriter testoutWriter = new FileWriter(testoutFile);

    boolean testCommandSucceeded = processCommand(testCommand, testoutWriter);
    testoutWriter.close();

    String testOutput = CommonUtil.readFile(Paths.get(testoutFile.getAbsolutePath()));

    boolean testPassed = false;
    String patch = "";
    if (failingTest) {
      if (!testCommandSucceeded) {
        // Command failed in the client.
        testPassed = true;
      } else {
        try {
          Answer testAnswer = BatfishObjectMapper.mapper().readValue(testOutput, Answer.class);
          testPassed = (testAnswer.getStatus() == AnswerStatus.FAILURE);
        } catch (JsonProcessingException e) {
          // pass here and let the test fail.
        }
      }
    } else if (testCommandSucceeded) {
      try {
        if (TestComparisonMode.RAW != comparisonMode) {
          Answer testAnswer = BatfishObjectMapper.mapper().readValue(testOutput, Answer.class);
          testOutput = getTestComparisonString(testAnswer, comparisonMode);
        }

        String referenceOutput =
            missingReferenceFile ? "" : CommonUtil.readFile(Paths.get(referenceFileName));

        patch = getPatch(referenceOutput, testOutput, referenceFileName, testFileName);
        if (patch.isEmpty()) {
          testPassed = true;
        }
      } catch (JsonProcessingException e) {
        _logger.errorf(
            "Error deserializing answer %s: %s\n", testOutput, Throwables.getStackTraceAsString(e));
      } catch (Exception e) {
        _logger.errorf(
            "Exception in comparing test results: %s\n", Throwables.getStackTraceAsString(e));
      }
    }

    _logger.outputf(
        "Test [%s]: %s '%s': %s\n%s",
        comparisonMode,
        StringUtils.join(testCommand, " "),
        failingTest ? "results in error as expected" : "matches " + referenceFileName,
        testPassed ? "Pass" : "Fail",
        testPassed ? "" : patch + "\n");
    if (!testPassed) {
      CommonUtil.writeFile(failedTestoutPath, testOutput);
      _logger.outputf("Copied output to %s\n", failedTestoutPath);
    }
    return testPassed;
  }

  private void unsetTestrig(boolean doDelta) {
    if (doDelta) {
      _currDeltaTestrig = null;
      _logger.info("Reference snapshot is now unset\n");
    } else {
      _currTestrig = null;
      _logger.info("Current snapshot is now unset\n");
    }
  }

  private boolean uploadCustomObject(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 2, 2, Command.UPLOAD_CUSTOM_OBJECT)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    String objectName = parameters.get(0);
    String objectFile = parameters.get(1);

    // upload the object
    return _workHelper.uploadCustomObject(_currContainerName, _currTestrig, objectName, objectFile);
  }

  private boolean uploadTestrig(String fileOrDir, String testrigName, boolean autoAnalyze) {
    Path initialUploadTarget = Paths.get(fileOrDir);
    Path uploadTarget = initialUploadTarget;
    boolean createZip = Files.isDirectory(initialUploadTarget);
    if (createZip) {
      uploadTarget = CommonUtil.createTempFile("testrig", "zip");
      ZipUtility.zipFiles(initialUploadTarget.toAbsolutePath(), uploadTarget.toAbsolutePath());
    }
    try {
      boolean result =
          _workHelper.uploadSnapshot(
              _currContainerName, testrigName, uploadTarget.toString(), autoAnalyze);
      return result;
    } finally {
      if (createZip) {
        CommonUtil.delete(uploadTarget);
      }
    }
  }

  private static void validateInstanceData(InstanceData instanceData) {
    String description = instanceData.getDescription();
    String q = "Question: '" + instanceData.getInstanceName() + "'";
    if (description == null || description.length() == 0) {
      throw new BatfishException(q + " is missing question description");
    }
    for (Entry<String, Variable> e : instanceData.getVariables().entrySet()) {
      String variableName = e.getKey();
      Variable variable = e.getValue();
      String v = "Variable: '" + variableName + "' in " + q;
      String variableDescription = variable.getDescription();
      if (variableDescription == null || variableDescription.length() == 0) {
        throw new BatfishException(v + " is missing description");
      }
    }
  }

  /**
   * Template validation extracts the question template text and parameters, and then relies on
   * {@link QuestionHelper#validateTemplate(JSONObject, Map)}
   *
   * @param words The array of command words that led to this function being called
   * @param outWriter The parsed question is written to this FileWriter
   * @param options The list of options in the command. Should be empty.
   * @param parameters The list of parameters in the command.
   * @return True if the command was valid and the template was valid; false otherwise.
   */
  @VisibleForTesting
  boolean validateTemplate(
      String[] words,
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, Integer.MAX_VALUE, Command.VALIDATE_TEMPLATE)) {
      return false;
    }

    String questionTemplateName = parameters.get(0);
    String questionContentUnmodified = _bfq.get(questionTemplateName.toLowerCase());
    if (questionContentUnmodified == null) {
      throw new BatfishException("Invalid question template name: '" + questionTemplateName + "'");
    }

    Map<String, JsonNode> parsedParameters =
        parseParams(String.join(" ", Arrays.copyOfRange(words, 2 + options.size(), words.length)));

    try {
      Question question =
          QuestionHelper.validateTemplate(
              new JSONObject(questionContentUnmodified), parsedParameters);
      logOutput(outWriter, BatfishObjectMapper.writePrettyString(question));
    } catch (IOException | JSONException e) {
      throw new BatfishException(
          "Could not create or write question template: " + e.getMessage(), e);
    }

    return true;
  }
}
