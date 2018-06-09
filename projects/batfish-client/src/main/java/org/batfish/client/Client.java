package org.batfish.client;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import javax.annotation.Nullable;
import org.apache.commons.io.output.WriterOutputStream;
import org.batfish.client.Command.TestComparisonMode;
import org.batfish.client.answer.LoadQuestionAnswerElement;
import org.batfish.client.config.Settings;
import org.batfish.client.config.Settings.RunMode;
import org.batfish.client.params.InitEnvironmentParams;
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
import org.batfish.common.util.UnzipUtility;
import org.batfish.common.util.WorkItemBuilder;
import org.batfish.common.util.ZipUtility;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
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
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.WorkStatus;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.Question.InstanceData;
import org.batfish.datamodel.questions.Question.InstanceData.Variable;
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

  private static final String DEFAULT_CONTAINER_PREFIX = "cp";

  private static final String DEFAULT_DELTA_ENV_PREFIX = "env_";

  private static final String DEFAULT_ENV_NAME = BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME;

  private static final String DEFAULT_QUESTION_PREFIX = "q";

  private static final String DEFAULT_TESTRIG_PREFIX = "tr_";

  private static final String DIFF_NOT_READY_MSG =
      "Cannot ask differential question without first setting delta testrig/environment\n";

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
   * validates the input {@code value} is allowed according to {@link
   * Question.InstanceData.Variable#_allowedValues allowedValues} specified in {@code variable}.
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
    if (!(variable.getAllowedValues().isEmpty()
        || variable.getAllowedValues().contains(value.asText()))) {
      throw new BatfishException(
          String.format(
              "Invalid value: %s, allowed values are: %s",
              value.asText(), variable.getAllowedValues()));
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
      case FLOAT:
        if (!value.isFloat()) {
          throw new BatfishException(
              String.format("It is not a valid JSON %s value", expectedType.getName()));
        }
        break;
      case INTEGER:
        if (!value.isInt()) {
          throw new BatfishException(
              String.format("It is not a valid JSON %s value", expectedType.getName()));
        }
        break;
      case LONG:
        if (!value.isLong()) {
          throw new BatfishException(
              String.format("It is not a valid JSON %s value", expectedType.getName()));
        }
        break;
      case IP:
        // TODO: Need to double check isInetAddress()
        if (!(value.isTextual())) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new Ip(value.textValue());
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
      case IP_WILDCARD:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        new IpWildcard(value.textValue());
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
      case QUESTION:
        // TODO: Implement
        break;

      case STRING:
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
      case PROTOCOL:
        if (!value.isTextual()) {
          throw new BatfishException(
              String.format("A Batfish %s must be a JSON string", expectedType.getName()));
        }
        Protocol.fromString(value.textValue());
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

  private String _currDeltaEnv = null;

  private String _currDeltaTestrig;

  private String _currEnv = null;

  private String _currTestrig = null;

  private boolean _exit;

  BatfishLogger _logger;

  WorkItem _polledWorkItem = null;

  @SuppressWarnings("unused")
  private BfCoordPoolHelper _poolHelper;

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
      case gendatamodel:
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
          OutputStream os = new WriterOutputStream(pWriter);
          PrintStream ps = new PrintStream(os, true);
          _logger = new BatfishLogger(_settings.getLogLevel(), false, ps);
        } catch (Exception e) {
          System.err.printf("Could not initialize client: %s\n", e.getMessage());
          e.printStackTrace();
          System.exit(1);
        }
        break;
      default:
        System.err.println("org.batfish.client: Unknown run mode.");
        System.exit(1);
    }
  }

  public Client(String[] args) throws Exception {
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
    if (questionJsonDifferential && (_currDeltaEnv == null || _currDeltaTestrig == null)) {
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
    if (questionFile != null) {
      CommonUtil.deleteIfExists(questionFile);
    }
    // answer the question
    WorkItem wItemAs =
        WorkItemBuilder.getWorkItemAnswerQuestion(
            questionName,
            _currContainerName,
            _currTestrig,
            _currEnv,
            _currDeltaTestrig,
            _currDeltaEnv,
            questionJsonDifferential,
            isDelta);
    return execute(wItemAs, outWriter);
  }

  private boolean answer(
      String[] words,
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta) {
    Command command = delta ? Command.ANSWER_DELTA : Command.ANSWER;
    if (!isValidArgument(options, parameters, 0, 1, Integer.MAX_VALUE, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true) || (delta && !isSetDeltaEnvironment())) {
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
            _currEnv,
            _currDeltaTestrig,
            _currDeltaEnv,
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
          parameterObj = new JSONTokener(parameterValue.toString()).nextValue();
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
    if (modifiedQuestion.getDifferential()
        && (_currDeltaEnv == null || _currDeltaTestrig == null)) {
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

  private boolean cat(String[] words) throws IOException, FileNotFoundException {
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

  private boolean clearScreen(List<String> options, List<String> parameters) throws IOException {
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

  private boolean delContainer(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEL_CONTAINER)) {
      return false;
    }
    String containerName = parameters.get(0);
    boolean result = _workHelper.delContainer(containerName);
    _logger.outputf("Result of deleting container: %s\n", result);
    return true;
  }

  private boolean delEnvironment(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEL_ENVIRONMENT)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    String envName = parameters.get(0);
    boolean result = _workHelper.delEnvironment(_currContainerName, _currTestrig, envName);
    _logger.outputf("Result of deleting environment: %s\n", result);
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

  private boolean delTestrig(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.DEL_TESTRIG)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    String testrigName = parameters.get(0);
    boolean result = _workHelper.delTestrig(_currContainerName, testrigName);
    logOutput(outWriter, "Result of deleting testrig: " + result + "\n");
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

  private void generateDatamodel() {
    try {
      JsonSchemaGenerator schemaGenNew = new JsonSchemaGenerator(BatfishObjectMapper.mapper());
      JsonNode schemaNew = schemaGenNew.generateJsonSchema(Configuration.class);
      _logger.output(BatfishObjectMapper.writePrettyString(schemaNew));

      // Reflections reflections = new Reflections("org.batfish.datamodel");
      // Set<Class<? extends AnswerElement>> classes =
      // reflections.getSubTypesOf(AnswerElement.class);
      // _logger.outputf("Found %d classes that inherit %s\n",
      // classes.toArray().length, "AnswerElement");
      //
      // File dmDir = Paths.get(_settings.getDatamodelDir()).toFile();
      // if (!dmDir.exists()) {
      // if (!dmDir.mkdirs()) {
      // throw new BatfishException("Could not create directory: " +
      // dmDir.getAbsolutePath());
      // }
      // }
      //
      // for (Class c : classes) {
      // String className = c.getCanonicalName()
      // .replaceAll("org\\.batfish\\.datamodel\\.", "")
      // .replaceAll("\\.", "-")
      // + ".json";
      // _logger.outputf("%s --> %s\n", c, className);
      // Path file = Paths.get(dmDir.getAbsolutePath(), className);
      // try (PrintWriter out = new
      // PrintWriter(file.toAbsolutePath().toString())) {
      // ObjectMapper mapper = new BatfishObjectMapper();
      // JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
      // JsonNode schema = schemaGen.generateJsonSchema(c);
      // String schemaString = mapper.writeValueAsString(schema);
      // out.println(schemaString);
      // }
      // }

      // JsonSchemaGenerator schemaGenNew = new JsonSchemaGenerator(mapper,
      // true, JsonSchemaConfig.vanillaJsonSchemaDraft4());
      // JsonNode schemaNew =
      // schemaGenNew.generateJsonSchema(Configuration.class);
      // _logger.output(mapper.writeValueAsString(schemaNew));

      // _logger.output("\n");
      // JsonNode schemaNew2 =
      // schemaGenNew.generateJsonSchema(SchemaTest.Parent.class);
      // _logger.output(mapper.writeValueAsString(schemaNew2));
    } catch (Exception e) {
      _logger.errorf("Could not generate data model: %s", e.getMessage());
      e.printStackTrace();
    }
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
        WorkItemBuilder.getWorkItemGenerateDataPlane(_currContainerName, _currTestrig, _currEnv);

    return execute(wItemGenDp, outWriter);
  }

  private boolean generateDeltaDataplane(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.GEN_DELTA_DP)) {
      return false;
    }
    if (!isSetDeltaEnvironment() || !isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    WorkItem wItemGenDdp =
        WorkItemBuilder.getWorkItemGenerateDeltaDataPlane(
            _currContainerName, _currTestrig, _currEnv, _currDeltaTestrig, _currDeltaEnv);

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
    Command command = delta ? Command.GET_DELTA : Command.GET;
    if (!isValidArgument(options, parameters, 0, 1, Integer.MAX_VALUE, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true) || (delta && !isSetDeltaEnvironment())) {
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
            : delta ? Command.GET_ANALYSIS_ANSWERS_DELTA : Command.GET_ANALYSIS_ANSWERS;
    if (!isValidArgument(options, parameters, 0, 1, 1, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    String analysisName = parameters.get(0);

    String baseTestrig;
    String baseEnvironment;
    String deltaTestrig;
    String deltaEnvironment;
    if (differential) {
      baseTestrig = _currTestrig;
      baseEnvironment = _currEnv;
      deltaTestrig = _currDeltaTestrig;
      deltaEnvironment = _currDeltaEnv;
    } else if (delta) {
      baseTestrig = _currDeltaTestrig;
      baseEnvironment = _currDeltaEnv;
      deltaTestrig = null;
      deltaEnvironment = null;
    } else {
      baseTestrig = _currTestrig;
      baseEnvironment = _currEnv;
      deltaTestrig = null;
      deltaEnvironment = null;
    }
    String answer =
        _workHelper.getAnalysisAnswers(
            _currContainerName,
            baseTestrig,
            baseEnvironment,
            deltaTestrig,
            deltaEnvironment,
            analysisName);

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
            : delta ? Command.GET_ANSWER_DELTA : Command.GET_ANSWER;
    if (!isValidArgument(options, parameters, 0, 1, 1, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    String questionName = parameters.get(0);

    String baseTestrig;
    String baseEnvironment;
    String deltaTestrig;
    String deltaEnvironment;
    if (differential) {
      baseTestrig = _currTestrig;
      baseEnvironment = _currEnv;
      deltaTestrig = _currDeltaTestrig;
      deltaEnvironment = _currDeltaEnv;
    } else if (delta) {
      baseTestrig = _currDeltaTestrig;
      baseEnvironment = _currDeltaEnv;
      deltaTestrig = null;
      deltaEnvironment = null;
    } else {
      baseTestrig = _currTestrig;
      baseEnvironment = _currEnv;
      deltaTestrig = null;
      deltaEnvironment = null;
    }
    String answerString =
        _workHelper.getAnswer(
            _currContainerName,
            baseTestrig,
            baseEnvironment,
            deltaTestrig,
            deltaEnvironment,
            questionName);

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
    String configContent = _workHelper.getConFiguration(containerName, testrigName, configName);
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
  private boolean getContainer(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.GET_CONTAINER)) {
      return false;
    }
    String containerName = parameters.get(0);
    Container container = _workHelper.getContainer(containerName);
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
    Command command = delta ? Command.GET_OBJECT_DELTA : Command.GET_OBJECT;
    if (!isValidArgument(options, parameters, 0, 1, 1, command)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true) || (delta && !isSetDeltaEnvironment())) {
      return false;
    }

    String testrig = delta ? _currDeltaTestrig : _currTestrig;
    String objectName = parameters.get(0);
    String tmpPath = _workHelper.getObject(_currContainerName, testrig, objectName);
    String objectString = CommonUtil.readFile(Paths.get(tmpPath));

    logOutput(outWriter, objectString + "\n");

    return true;
  }

  private boolean getQuestion(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.GET_QUESTION)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }

    String questionName = parameters.get(0);

    String questionFileName =
        String.format(
            "%s/%s/%s",
            BfConsts.RELPATH_QUESTIONS_DIR, questionName, BfConsts.RELPATH_QUESTION_FILE);

    String downloadedQuestionFile =
        _workHelper.getObject(_currContainerName, _currTestrig, questionFileName);
    if (downloadedQuestionFile == null) {
      _logger.errorf("Failed to get question file %s\n", questionFileName);
      return false;
    }

    String questionString = CommonUtil.readFile(Paths.get(downloadedQuestionFile));
    _logger.outputf("Question:\n%s\n", questionString);

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

  private boolean initContainer(List<String> options, List<String> parameters) {
    if (options.contains("-setname")) {
      if (!isValidArgument(options, parameters, 1, 1, 1, Command.INIT_CONTAINER)) {
        return false;
      }
      _currContainerName = _workHelper.initContainer(parameters.get(0), null);
    } else {
      if (!isValidArgument(options, parameters, 0, 0, 1, Command.INIT_CONTAINER)) {
        return false;
      }
      String containerPrefix = parameters.isEmpty() ? DEFAULT_CONTAINER_PREFIX : parameters.get(0);
      _currContainerName = _workHelper.initContainer(null, containerPrefix);
    }
    if (_currContainerName == null) {
      _logger.errorf("Could not init container\n");
      return false;
    }
    _logger.output("Active container is set");
    _logger.infof(" to  %s\n", _currContainerName);
    _logger.output("\n");
    return true;
  }

  private boolean initEnvironment(String paramsLine, FileWriter outWriter) {
    InitEnvironmentParams params = parseInitEnvironmentParams(paramsLine);
    String newEnvName;
    String paramsLocation = params.getSourcePath();
    String paramsName = params.getNewEnvironmentName();
    String paramsPrefix = params.getNewEnvironmentPrefix();
    String testrigName = params.getDoDelta() ? _currDeltaTestrig : _currTestrig;
    if (paramsName != null) {
      newEnvName = paramsName;
    } else if (paramsPrefix != null) {
      newEnvName = paramsPrefix + UUID.randomUUID();
    } else {
      newEnvName = DEFAULT_DELTA_ENV_PREFIX + UUID.randomUUID();
    }
    String paramsBaseEnv = params.getSourceEnvironmentName();
    String baseEnvName =
        paramsBaseEnv != null ? paramsBaseEnv : BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME;
    String fileToSend;
    SortedSet<String> paramsNodeBlacklist = params.getNodeBlacklist();
    SortedSet<NodeInterfacePair> paramsInterfaceBlacklist = params.getInterfaceBlacklist();
    SortedSet<Edge> paramsEdgeBlacklist = params.getEdgeBlacklist();

    if (paramsLocation == null
        || Files.isDirectory(Paths.get(paramsLocation))
        || !paramsNodeBlacklist.isEmpty()
        || !paramsInterfaceBlacklist.isEmpty()
        || !paramsEdgeBlacklist.isEmpty()) {
      Path tempFile = CommonUtil.createTempFile("batfish_client_tmp_env_", ".zip");
      fileToSend = tempFile.toString();
      if (paramsLocation != null
          && Files.isDirectory(Paths.get(paramsLocation))
          && paramsNodeBlacklist.isEmpty()
          && paramsInterfaceBlacklist.isEmpty()
          && paramsEdgeBlacklist.isEmpty()) {
        ZipUtility.zipFiles(Paths.get(paramsLocation), tempFile);
      } else {
        Path tempDir = CommonUtil.createTempDirectory("batfish_client_tmp_env_");
        if (paramsLocation != null) {
          if (Files.isDirectory(Paths.get(paramsLocation))) {
            CommonUtil.copyDirectory(Paths.get(paramsLocation), tempDir);
          } else if (Files.isRegularFile(Paths.get(paramsLocation))) {
            UnzipUtility.unzip(Paths.get(paramsLocation), tempDir);
          } else {
            throw new BatfishException(
                "Invalid environment directory or zip: '" + paramsLocation + "'");
          }
        }
        if (!paramsNodeBlacklist.isEmpty()) {
          String nodeBlacklistText;
          try {
            nodeBlacklistText = BatfishObjectMapper.writePrettyString(paramsNodeBlacklist);
          } catch (JsonProcessingException e) {
            throw new BatfishException("Failed to write node blacklist to string", e);
          }
          Path nodeBlacklistFilePath = tempDir.resolve(BfConsts.RELPATH_NODE_BLACKLIST_FILE);
          CommonUtil.writeFile(nodeBlacklistFilePath, nodeBlacklistText);
        }
        if (!paramsInterfaceBlacklist.isEmpty()) {
          String interfaceBlacklistText;
          try {
            interfaceBlacklistText =
                BatfishObjectMapper.writePrettyString(paramsInterfaceBlacklist);
          } catch (JsonProcessingException e) {
            throw new BatfishException("Failed to write interface blacklist to string", e);
          }
          Path interfaceBlacklistFilePath =
              tempDir.resolve(BfConsts.RELPATH_INTERFACE_BLACKLIST_FILE);
          CommonUtil.writeFile(interfaceBlacklistFilePath, interfaceBlacklistText);
        }
        if (!paramsEdgeBlacklist.isEmpty()) {
          String edgeBlacklistText;
          try {
            edgeBlacklistText = BatfishObjectMapper.writePrettyString(paramsEdgeBlacklist);
          } catch (JsonProcessingException e) {
            throw new BatfishException("Failed to write edge blacklist to string", e);
          }
          Path edgeBlacklistFilePath = tempDir.resolve(BfConsts.RELPATH_EDGE_BLACKLIST_FILE);
          CommonUtil.writeFile(edgeBlacklistFilePath, edgeBlacklistText);
        }
        ZipUtility.zipFiles(tempDir, tempFile);
      }
    } else if (Files.isRegularFile(Paths.get(paramsLocation))) {
      fileToSend = paramsLocation;
    } else {
      throw new BatfishException("Invalid environment directory or zip: '" + paramsLocation + "'");
    }
    if (!uploadEnv(fileToSend, testrigName, newEnvName, baseEnvName)) {
      return false;
    }

    _currDeltaEnv = newEnvName;
    _currDeltaTestrig = _currTestrig;

    _logger.output("Active delta testrig->environment is set");
    _logger.infof("to %s->%s\n", _currDeltaTestrig, _currDeltaEnv);
    _logger.output("\n");

    WorkItem wItemProcessEnv =
        WorkItemBuilder.getWorkItemProcessEnvironment(
            _currContainerName, _currDeltaTestrig, _currDeltaEnv);
    if (!execute(wItemProcessEnv, outWriter)) {
      return false;
    }

    return true;
  }

  private boolean initEnvironment(
      String[] words,
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, Integer.MAX_VALUE, Command.INIT_ENVIRONMENT)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }
    String paramsLine = String.join(" ", Arrays.copyOfRange(words, 1, words.length));
    return initEnvironment(paramsLine, outWriter);
  }

  private void initHelpers() {
    switch (_settings.getRunMode()) {
      case batch:
      case interactive:
        break;

      case gendatamodel:
      case genquestions:
      default:
        return;
    }

    _workHelper = new BfCoordWorkHelper(_logger, _settings);

    String poolMgr = _settings.getCoordinatorHost() + ":" + _settings.getCoordinatorPoolPort();
    _poolHelper = new BfCoordPoolHelper(poolMgr);

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

  private boolean initTestrig(
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta) {
    Command command = delta ? Command.INIT_DELTA_TESTRIG : Command.INIT_TESTRIG;
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
        (parameters.size() > 1) ? parameters.get(1) : DEFAULT_TESTRIG_PREFIX + UUID.randomUUID();

    // initialize the container if it hasn't been init'd before
    if (!isSetContainer(false)) {
      _currContainerName = _workHelper.initContainer(null, DEFAULT_CONTAINER_PREFIX);
      if (_currContainerName == null) {
        _logger.errorf("Could not init container\n");
        return false;
      }
      _logger.output("Init'ed and set active container");
      _logger.infof(" to %s\n", _currContainerName);
      _logger.output("\n");
    }

    if (!uploadTestrig(testrigLocation, testrigName, autoAnalyze)) {
      unsetTestrig(delta);
      return false;
    }
    _logger.output("Uploaded testrig.\n");

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
      _currEnv = DEFAULT_ENV_NAME;
      _logger.infof("Base testrig is now %s\n", _currTestrig);
    } else {
      _currDeltaTestrig = testrigName;
      _currDeltaEnv = DEFAULT_ENV_NAME;
      _logger.infof("Delta testrig is now %s\n", _currDeltaTestrig);
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
        _logger.errorf("Active container is not set\n");
      }
      return false;
    }

    return true;
  }

  private boolean isSetDeltaEnvironment() {
    if (!_settings.getSanityCheck()) {
      return true;
    }

    if (_currDeltaTestrig == null) {
      _logger.errorf("Active delta testrig is not set\n");
      return false;
    }

    if (_currDeltaEnv == null) {
      _logger.errorf("Active delta environment is not set\n");
      return false;
    }
    return true;
  }

  private boolean isSetTestrig() {
    if (!_settings.getSanityCheck()) {
      return true;
    }

    if (_currTestrig == null) {
      _logger.errorf("Active testrig is not set.\n");
      _logger.errorf(
          "Specify testrig on command line (-%s <testrigdir>) or use command (%s <testrigdir>)\n",
          Settings.ARG_TESTRIG_DIR, Command.INIT_TESTRIG);
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

  private boolean listContainers(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.LIST_CONTAINERS)) {
      return false;
    }
    String[] containerList = _workHelper.listContainers();
    _logger.outputf("Containers: %s\n", Arrays.toString(containerList));
    return true;
  }

  private boolean listEnvironments(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.LIST_ENVIRONMENTS)) {
      return false;
    }
    if (!isSetTestrig() || !isSetContainer(true)) {
      return false;
    }
    String[] environmentList = _workHelper.listEnvironments(_currContainerName, _currTestrig);
    _logger.outputf("Environments: %s\n", Arrays.toString(environmentList));

    return true;
  }

  private boolean listIncompleteWork(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.LIST_ENVIRONMENTS)) {
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

  private boolean listTestrigs(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 1, 0, 0, Command.LIST_TESTRIGS)) {
      return false;
    }

    boolean showMetadata = true;
    if (options.size() == 1) {
      if (options.get(0).equals("-nometadata")) {
        showMetadata = false;
      } else {
        _logger.errorf("Unknown option: %s\n", options.get(0));
        printUsage(Command.LIST_TESTRIGS);
        return false;
      }
    }

    JSONArray testrigArray = _workHelper.listTestrigs(_currContainerName);
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
              questionContent -> {
                updateLoadedQuestionsInfo(questionName, questionContent, destinationMap, ae);
              });
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

  static InitEnvironmentParams parseInitEnvironmentParams(String paramsLine) {
    String jsonParamsStr = "{ " + paramsLine + " }";
    InitEnvironmentParams parameters;
    try {
      parameters =
          BatfishObjectMapper.mapper()
              .readValue(
                  new JSONObject(jsonParamsStr).toString(),
                  new TypeReference<InitEnvironmentParams>() {});
      return parameters;
    } catch (JSONException | IOException e) {
      throw new BatfishException(
          "Failed to parse parameters. (Are all key-value pairs separated by commas? Are all "
              + "values valid JSON?)",
          e);
    }
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

  private boolean pollWork(
      @Nullable FileWriter outWriter, List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.POLL_WORK)) {
      return false;
    }
    UUID workId = UUID.fromString(parameters.get(0));
    return pollWork(workId, outWriter);
  }

  private boolean pollWork(UUID wItemId, @Nullable FileWriter outWriter) {

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

    boolean pollResult = pollWork(wItem.getId(), outWriter);
    if (!pollResult) {
      return false;
    }
    // get the answer
    String ansFileName = wItem.getId() + BfConsts.SUFFIX_ANSWER_JSON_FILE;
    String downloadedAnsFile =
        _workHelper.getObject(wItem.getContainerName(), wItem.getTestrigName(), ansFileName);
    if (downloadedAnsFile == null) {
      _logger.errorf("Failed to get answer file %s. (Was work killed?)\n", ansFileName);
    } else {
      String answerString = CommonUtil.readFile(Paths.get(downloadedAnsFile));

      // Check if we need to make things pretty
      // Don't if we are writing to FileWriter, because we need valid JSON in
      // that case
      String answerStringToPrint = answerString;
      if (outWriter == null && _settings.getPrettyPrintAnswers()) {
        Answer answer;
        try {
          answer = BatfishObjectMapper.mapper().readValue(answerString, Answer.class);
        } catch (IOException e) {
          throw new BatfishException(
              "Response does not appear to be valid JSON representation of "
                  + Answer.class.getSimpleName(),
              e);
        }
        answerStringToPrint = answer.prettyPrint();
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
          _workHelper.getObject(wItem.getContainerName(), wItem.getTestrigName(), logFileName);

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
    if (words.length > 0 && !validCommandUsage(words)) {
      return false;
    }
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
      case ANSWER_DELTA:
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
      case DEL_ANALYSIS:
        return delAnalysis(outWriter, options, parameters);
      case DEL_ANALYSIS_QUESTIONS:
        return delAnalysisQuestions(outWriter, options, parameters);
      case DEL_BATFISH_OPTION:
        return delBatfishOption(options, parameters);
      case DEL_CONTAINER:
        return delContainer(options, parameters);
      case DEL_ENVIRONMENT:
        return delEnvironment(options, parameters);
      case DEL_QUESTION:
        return delQuestion(options, parameters);
      case DEL_TESTRIG:
        return delTestrig(outWriter, options, parameters);
      case DIR:
        return dir(options, parameters);
      case ECHO:
        return echo(words);
      case GEN_DP:
        return generateDataplane(outWriter, options, parameters);
      case GEN_DELTA_DP:
        return generateDeltaDataplane(outWriter, options, parameters);
      case GET:
        return get(words, outWriter, options, parameters, false);
      case GET_CONFIGURATION:
        return getConfiguration(options, parameters);
      case GET_CONTAINER:
        return getContainer(options, parameters);
      case GET_DELTA:
        return get(words, outWriter, options, parameters, true);
      case GET_ANALYSIS_ANSWERS:
        return getAnalysisAnswers(outWriter, options, parameters, false, false);
      case GET_ANALYSIS_ANSWERS_DELTA:
        return getAnalysisAnswers(outWriter, options, parameters, true, false);
      case GET_ANALYSIS_ANSWERS_DIFFERENTIAL:
        return getAnalysisAnswers(outWriter, options, parameters, false, true);
      case GET_ANSWER:
        return getAnswer(outWriter, options, parameters, false, false);
      case GET_ANSWER_DELTA:
        return getAnswer(outWriter, options, parameters, true, false);
      case GET_ANSWER_DIFFERENTIAL:
        return getAnswer(outWriter, options, parameters, false, true);
      case GET_OBJECT:
        return getObject(outWriter, options, parameters, false);
      case GET_OBJECT_DELTA:
        return getObject(outWriter, options, parameters, false);
      case GET_QUESTION:
        return getQuestion(options, parameters);
      case GET_QUESTION_TEMPLATES:
        return getQuestionTemplates(options, parameters);
      case GET_WORK_STATUS:
        return getWorkStatus(outWriter, options, parameters);
      case HELP:
        return help(options, parameters);
      case INIT_ANALYSIS:
        return initOrAddAnalysis(outWriter, options, parameters, true);
      case INIT_CONTAINER:
        return initContainer(options, parameters);
      case INIT_DELTA_TESTRIG:
        return initTestrig(outWriter, options, parameters, true);
      case INIT_ENVIRONMENT:
        return initEnvironment(words, outWriter, options, parameters);
      case INIT_TESTRIG:
        return initTestrig(outWriter, options, parameters, false);
      case KILL_WORK:
        return killWork(options, parameters);
      case LIST_ANALYSES:
        return listAnalyses(outWriter, options, parameters);
      case LIST_CONTAINERS:
        return listContainers(options, parameters);
      case LIST_ENVIRONMENTS:
        return listEnvironments(options, parameters);
      case LIST_INCOMPLETE_WORK:
        return listIncompleteWork(options, parameters);
      case LIST_QUESTIONS:
        return listQuestions(options, parameters);
      case LIST_TESTRIGS:
        return listTestrigs(outWriter, options, parameters);
      case LOAD_QUESTIONS:
        return loadQuestions(outWriter, options, parameters, _bfq);
      case POLL_WORK:
        return pollWork(outWriter, options, parameters);
      case PROMPT:
        return prompt(options, parameters);
      case PWD:
        return pwd(options, parameters);
      case REINIT_DELTA_TESTRIG:
        return reinitTestrig(outWriter, options, parameters, true);
      case RUN_ANALYSIS:
        return runAnalysis(outWriter, options, parameters, false, false);
      case RUN_ANALYSIS_DELTA:
        return runAnalysis(outWriter, options, parameters, true, false);
      case RUN_ANALYSIS_DIFFERENTIAL:
        return runAnalysis(outWriter, options, parameters, false, true);
      case REINIT_TESTRIG:
        return reinitTestrig(outWriter, options, parameters, false);
      case SET_BACKGROUND_EXECUCTION:
        return setBackgroundExecution(options, parameters);
      case SET_BATFISH_LOGLEVEL:
        return setBatfishLogLevel(options, parameters);
      case SET_CONTAINER:
        return setContainer(options, parameters);
      case SET_DELTA_ENV:
        return setDeltaEnv(options, parameters);
      case SET_ENV:
        return setEnv(options, parameters);
      case SET_FIXED_WORKITEM_ID:
        return setFixedWorkItemId(options, parameters);
      case SET_DELTA_TESTRIG:
        return setDeltaTestrig(options, parameters);
      case SET_LOGLEVEL:
        return setLogLevel(options, parameters);
      case SET_PRETTY_PRINT:
        return setPrettyPrint(options, parameters);
      case SET_TESTRIG:
        return setTestrig(options, parameters);
      case SHOW_API_KEY:
        return showApiKey(options, parameters);
      case SHOW_BATFISH_LOGLEVEL:
        return showBatfishLogLevel(options, parameters);
      case SHOW_BATFISH_OPTIONS:
        return showBatfishOptions(options, parameters);
      case SHOW_CONTAINER:
        return showContainer(options, parameters);
      case SHOW_COORDINATOR_HOST:
        return showCoordinatorHost(options, parameters);
      case SHOW_DELTA_TESTRIG:
        return showDeltaTestrig(options, parameters);
      case SHOW_LOGLEVEL:
        return showLogLevel(options, parameters);
      case SHOW_TESTRIG:
        return showTestrig(options, parameters);
      case SHOW_VERSION:
        return showVersion(options, parameters);
      case SYNC_TESTRIGS_SYNC_NOW:
        return syncTestrigsSyncNow(options, parameters);
      case SYNC_TESTRIGS_UPDATE_SETTINGS:
        return syncTestrigsUpdateSettings(words, options, parameters);
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

  private boolean reinitTestrig(
      @Nullable FileWriter outWriter,
      List<String> options,
      List<String> parameters,
      boolean delta) {
    Command command = delta ? Command.REINIT_DELTA_TESTRIG : Command.REINIT_TESTRIG;
    if (!isValidArgument(options, parameters, 0, 0, 0, command)) {
      return false;
    }
    String testrig;
    if (!delta) {
      _logger.output("Reinitializing testrig. Parsing now.\n");
      testrig = _currTestrig;
    } else {
      _logger.output("Reinitializing delta testrig. Parsing now.\n");
      testrig = _currDeltaTestrig;
    }

    WorkItem wItemParse = WorkItemBuilder.getWorkItemParse(_currContainerName, testrig);

    if (!execute(wItemParse, outWriter)) {
      return false;
    }

    return true;
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
        && !processCommand(
            Command.SET_CONTAINER.commandName() + "  " + _settings.getContainerId())) {
      return;
    }

    // set testrig if dir or id is specified
    if (_settings.getTestrigDir() != null) {
      if (_settings.getTestrigId() != null) {
        System.err.println("org.batfish.client: Cannot supply both testrigDir and testrigId.");
        System.exit(1);
      }
      if (!processCommand(Command.INIT_TESTRIG.commandName() + " " + _settings.getTestrigDir())) {
        return;
      }
    }
    if (_settings.getTestrigId() != null
        && !processCommand(Command.SET_TESTRIG.commandName() + "  " + _settings.getTestrigId())) {
      return;
    }

    switch (_settings.getRunMode()) {
      case batch:
        {
          runBatchFile();
          break;
        }

      case gendatamodel:
        generateDatamodel();
        break;

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
            : delta ? Command.RUN_ANALYSIS_DELTA : Command.RUN_ANALYSIS;
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
            analysisName,
            _currContainerName,
            _currTestrig,
            _currEnv,
            _currDeltaTestrig,
            _currDeltaEnv,
            delta,
            differential);

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

  private boolean setContainer(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_CONTAINER)) {
      return false;
    }
    _currContainerName = parameters.get(0);
    _logger.outputf("Active container is now set to %s\n", _currContainerName);
    return true;
  }

  private boolean setDeltaEnv(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_DELTA_ENV)) {
      return false;
    }
    _currDeltaEnv = parameters.get(0);
    if (_currDeltaTestrig == null) {
      _currDeltaTestrig = _currTestrig;
    }
    _logger.outputf(
        "Active delta testrig->environment is now %s->%s\n", _currDeltaTestrig, _currDeltaEnv);
    return true;
  }

  private boolean setDeltaTestrig(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 2, Command.SET_DELTA_TESTRIG)) {
      return false;
    }
    _currDeltaTestrig = parameters.get(0);
    _currDeltaEnv = (parameters.size() > 1) ? parameters.get(1) : DEFAULT_ENV_NAME;
    _logger.outputf("Delta testrig->env is now %s->%s\n", _currDeltaTestrig, _currDeltaEnv);
    return true;
  }

  private boolean setEnv(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 1, Command.SET_ENV)) {
      return false;
    }
    if (!isSetTestrig()) {
      return false;
    }
    _currEnv = parameters.get(0);
    _logger.outputf("Base testrig->env is now %s->%s\n", _currTestrig, _currEnv);
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

  private boolean setTestrig(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 1, 2, Command.SET_TESTRIG)) {
      return false;
    }
    if (!isSetContainer(true)) {
      return false;
    }

    _currTestrig = parameters.get(0);
    _currEnv = (parameters.size() > 1) ? parameters.get(1) : DEFAULT_ENV_NAME;
    _logger.outputf("Base testrig->env is now %s->%s\n", _currTestrig, _currEnv);
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

  private boolean showContainer(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_CONTAINER)) {
      return false;
    }
    _logger.outputf("Current container is %s\n", _currContainerName);
    return true;
  }

  private boolean showCoordinatorHost(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_COORDINATOR_HOST)) {
      return false;
    }
    _logger.outputf("Current coordinator host is %s\n", _settings.getCoordinatorHost());
    return true;
  }

  private boolean showDeltaTestrig(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_DELTA_TESTRIG)) {
      return false;
    }
    if (!isSetDeltaEnvironment()) {
      return false;
    }
    _logger.outputf("Delta testrig->environment is %s->%s\n", _currDeltaTestrig, _currDeltaEnv);
    return true;
  }

  private boolean showLogLevel(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_LOGLEVEL)) {
      return false;
    }
    _logger.outputf("Current client log level is %s\n", _logger.getLogLevelStr());
    return true;
  }

  private boolean showTestrig(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 0, 0, 0, Command.SHOW_TESTRIG)) {
      return false;
    }
    if (!isSetTestrig()) {
      return false;
    }
    _logger.outputf("Base testrig->environment is %s->%s\n", _currTestrig, _currEnv);
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

  private boolean syncTestrigsSyncNow(List<String> options, List<String> parameters) {
    if (!isValidArgument(options, parameters, 1, 1, 1, Command.SYNC_TESTRIGS_SYNC_NOW)) {
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
        printUsage(Command.SYNC_TESTRIGS_SYNC_NOW);
        return false;
      }
    }

    String pluginId = parameters.get(0);

    return _workHelper.syncTestrigsSyncNow(pluginId, _currContainerName, force);
  }

  private boolean syncTestrigsUpdateSettings(
      String[] words, List<String> options, List<String> parameters) {
    if (!isValidArgument(
        options, parameters, 0, 1, Integer.MAX_VALUE, Command.SYNC_TESTRIGS_UPDATE_SETTINGS)) {
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

    return _workHelper.syncTestrigsUpdateSettings(pluginId, _currContainerName, settings);
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
    Path failedTestoutPath = Paths.get(referenceFile + ".testout");
    CommonUtil.deleteIfExists(failedTestoutPath);

    File testoutFile = Files.createTempFile("test", "out").toFile();
    testoutFile.deleteOnExit();
    FileWriter testoutWriter = new FileWriter(testoutFile);

    boolean testCommandSucceeded = processCommand(testCommand, testoutWriter);
    testoutWriter.close();

    String testOutput = CommonUtil.readFile(Paths.get(testoutFile.getAbsolutePath()));
    boolean testPassed = false;

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

        if (!missingReferenceFile) {
          String referenceOutput = CommonUtil.readFile(Paths.get(referenceFileName));
          if (referenceOutput.equals(testOutput)) {
            testPassed = true;
          }
        }
      } catch (JsonProcessingException e) {
        _logger.errorf(
            "Error deserializing answer %s: %s\n", testOutput, Throwables.getStackTraceAsString(e));
      } catch (Exception e) {
        _logger.errorf(
            "Exception in comparing test results: %s\n", Throwables.getStackTraceAsString(e));
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append("'" + testCommand[0]);
    for (int i = 1; i < testCommand.length; i++) {
      sb.append(" " + testCommand[i]);
    }
    sb.append("'");
    String testCommandText = sb.toString();

    _logger.outputf(
        "Test [%s]: %s %s: %s\n",
        comparisonMode,
        testCommandText,
        failingTest ? "results in error as expected" : "matches " + referenceFileName,
        testPassed ? "Pass" : "Fail");
    if (!testPassed) {
      CommonUtil.writeFile(failedTestoutPath, testOutput);
      _logger.outputf("Copied output to %s\n", failedTestoutPath);
    }
    return testPassed;
  }

  private void unsetTestrig(boolean doDelta) {
    if (doDelta) {
      _currDeltaTestrig = null;
      _currDeltaEnv = null;
      _logger.info("Delta testrig and environment are now unset\n");
    } else {
      _currTestrig = null;
      _currEnv = null;
      _logger.info("Base testrig and environment are now unset\n");
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

  private boolean uploadEnv(
      String fileOrDir, String testrigName, String newEnvName, String baseEnvName) {
    Path initialUploadTarget = Paths.get(fileOrDir);
    Path uploadTarget = initialUploadTarget;
    boolean createZip = Files.isDirectory(initialUploadTarget);
    if (createZip) {
      uploadTarget = CommonUtil.createTempFile("testrigOrEnv", ".zip");
      ZipUtility.zipFiles(initialUploadTarget.toAbsolutePath(), uploadTarget.toAbsolutePath());
    }
    try {
      boolean result =
          _workHelper.uploadEnvironment(
              _currContainerName, testrigName, baseEnvName, newEnvName, uploadTarget.toString());
      return result;
    } finally {
      if (createZip) {
        CommonUtil.delete(uploadTarget);
      }
    }
  }

  private boolean uploadTestrig(String fileOrDir, String testrigName, boolean autoAnalyze) {
    Path initialUploadTarget = Paths.get(fileOrDir);
    Path uploadTarget = initialUploadTarget;
    boolean createZip = Files.isDirectory(initialUploadTarget);
    if (createZip) {
      uploadTarget = CommonUtil.createTempFile("testrigOrEnv", "zip");
      ZipUtility.zipFiles(initialUploadTarget.toAbsolutePath(), uploadTarget.toAbsolutePath());
    }
    try {
      boolean result =
          _workHelper.uploadTestrig(
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

  private boolean validCommandUsage(String[] words) {
    return true;
  }
}
