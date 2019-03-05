package org.batfish.client;

import static org.batfish.client.Command.ADD_ANALYSIS_QUESTIONS;
import static org.batfish.client.Command.ADD_BATFISH_OPTION;
import static org.batfish.client.Command.ANSWER;
import static org.batfish.client.Command.ANSWER_REFERENCE;
import static org.batfish.client.Command.CAT;
import static org.batfish.client.Command.CHECK_API_KEY;
import static org.batfish.client.Command.CLEAR_SCREEN;
import static org.batfish.client.Command.DEL_ANALYSIS;
import static org.batfish.client.Command.DEL_ANALYSIS_QUESTIONS;
import static org.batfish.client.Command.DEL_BATFISH_OPTION;
import static org.batfish.client.Command.DEL_NETWORK;
import static org.batfish.client.Command.DEL_QUESTION;
import static org.batfish.client.Command.DEL_SNAPSHOT;
import static org.batfish.client.Command.DIR;
import static org.batfish.client.Command.EXIT;
import static org.batfish.client.Command.GEN_DP;
import static org.batfish.client.Command.GEN_REFERENCE_DP;
import static org.batfish.client.Command.GET;
import static org.batfish.client.Command.GET_ANALYSIS_ANSWERS;
import static org.batfish.client.Command.GET_ANALYSIS_ANSWERS_DIFFERENTIAL;
import static org.batfish.client.Command.GET_ANALYSIS_ANSWERS_REFERENCE;
import static org.batfish.client.Command.GET_ANSWER;
import static org.batfish.client.Command.GET_ANSWER_DIFFERENTIAL;
import static org.batfish.client.Command.GET_ANSWER_REFERENCE;
import static org.batfish.client.Command.GET_CONFIGURATION;
import static org.batfish.client.Command.GET_REFERENCE;
import static org.batfish.client.Command.HELP;
import static org.batfish.client.Command.INIT_ANALYSIS;
import static org.batfish.client.Command.INIT_NETWORK;
import static org.batfish.client.Command.INIT_REFERENCE_SNAPSHOT;
import static org.batfish.client.Command.INIT_SNAPSHOT;
import static org.batfish.client.Command.LIST_ANALYSES;
import static org.batfish.client.Command.LIST_NETWORKS;
import static org.batfish.client.Command.LIST_QUESTIONS;
import static org.batfish.client.Command.LIST_SNAPSHOTS;
import static org.batfish.client.Command.LOAD_QUESTIONS;
import static org.batfish.client.Command.PROMPT;
import static org.batfish.client.Command.PWD;
import static org.batfish.client.Command.RUN_ANALYSIS;
import static org.batfish.client.Command.RUN_ANALYSIS_DIFFERENTIAL;
import static org.batfish.client.Command.RUN_ANALYSIS_REFERENCE;
import static org.batfish.client.Command.SET_BATFISH_LOGLEVEL;
import static org.batfish.client.Command.SET_LOGLEVEL;
import static org.batfish.client.Command.SET_NETWORK;
import static org.batfish.client.Command.SET_PRETTY_PRINT;
import static org.batfish.client.Command.SET_REFERENCE_SNAPSHOT;
import static org.batfish.client.Command.SET_SNAPSHOT;
import static org.batfish.client.Command.SHOW_API_KEY;
import static org.batfish.client.Command.SHOW_BATFISH_LOGLEVEL;
import static org.batfish.client.Command.SHOW_BATFISH_OPTIONS;
import static org.batfish.client.Command.SHOW_COORDINATOR_HOST;
import static org.batfish.client.Command.SHOW_LOGLEVEL;
import static org.batfish.client.Command.SHOW_NETWORK;
import static org.batfish.client.Command.SHOW_REFERENCE_SNAPSHOT;
import static org.batfish.client.Command.SHOW_SNAPSHOT;
import static org.batfish.client.Command.TEST;
import static org.batfish.client.Command.UPLOAD_CUSTOM_OBJECT;
import static org.batfish.common.CoordConsts.DEFAULT_API_KEY;
import static org.batfish.datamodel.questions.Variable.Type.ADDRESS_GROUP_AND_BOOK;
import static org.batfish.datamodel.questions.Variable.Type.BGP_SESSION_STATUS;
import static org.batfish.datamodel.questions.Variable.Type.BGP_SESSION_TYPE;
import static org.batfish.datamodel.questions.Variable.Type.BOOLEAN;
import static org.batfish.datamodel.questions.Variable.Type.COMPARATOR;
import static org.batfish.datamodel.questions.Variable.Type.DISPOSITION_SPEC;
import static org.batfish.datamodel.questions.Variable.Type.DOUBLE;
import static org.batfish.datamodel.questions.Variable.Type.FILTER;
import static org.batfish.datamodel.questions.Variable.Type.FILTER_SPEC;
import static org.batfish.datamodel.questions.Variable.Type.FLOAT;
import static org.batfish.datamodel.questions.Variable.Type.FLOW_STATE;
import static org.batfish.datamodel.questions.Variable.Type.INTEGER;
import static org.batfish.datamodel.questions.Variable.Type.INTERFACE;
import static org.batfish.datamodel.questions.Variable.Type.INTERFACES_SPEC;
import static org.batfish.datamodel.questions.Variable.Type.INTERFACE_GROUP_AND_BOOK;
import static org.batfish.datamodel.questions.Variable.Type.INTERFACE_NAME;
import static org.batfish.datamodel.questions.Variable.Type.INTERFACE_TYPE;
import static org.batfish.datamodel.questions.Variable.Type.IP;
import static org.batfish.datamodel.questions.Variable.Type.IPSEC_SESSION_STATUS;
import static org.batfish.datamodel.questions.Variable.Type.IP_PROTOCOL;
import static org.batfish.datamodel.questions.Variable.Type.IP_SPACE_SPEC;
import static org.batfish.datamodel.questions.Variable.Type.IP_WILDCARD;
import static org.batfish.datamodel.questions.Variable.Type.JAVA_REGEX;
import static org.batfish.datamodel.questions.Variable.Type.JSON_PATH;
import static org.batfish.datamodel.questions.Variable.Type.JSON_PATH_REGEX;
import static org.batfish.datamodel.questions.Variable.Type.LOCATION_SPEC;
import static org.batfish.datamodel.questions.Variable.Type.LONG;
import static org.batfish.datamodel.questions.Variable.Type.NODE_ROLE_DIMENSION;
import static org.batfish.datamodel.questions.Variable.Type.PREFIX;
import static org.batfish.datamodel.questions.Variable.Type.PREFIX_RANGE;
import static org.batfish.datamodel.questions.Variable.Type.PROTOCOL;
import static org.batfish.datamodel.questions.Variable.Type.ROUTING_PROTOCOL_SPEC;
import static org.batfish.datamodel.questions.Variable.Type.STRING;
import static org.batfish.datamodel.questions.Variable.Type.STRUCTURE_NAME;
import static org.batfish.datamodel.questions.Variable.Type.SUBRANGE;
import static org.batfish.datamodel.questions.Variable.Type.VRF;
import static org.batfish.datamodel.questions.Variable.Type.ZONE;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.algorithm.DiffException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.batfish.client.answer.LoadQuestionAnswerElement;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.Pair;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Protocol;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.questions.AllowedValue;
import org.batfish.datamodel.questions.Variable;
import org.batfish.datamodel.questions.Variable.Type;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link org.batfish.client.Client}. */
public final class ClientTest {

  private static final String NETWORK_NOT_SET = "Active network is not set\n";

  private static final String SNAPSHOT_NOT_SET =
      "Active snapshot is not set.\nSpecify snapshot on"
          + " command line (-snapshotdir <snapshotdir>) or use command (init-snapshot <snapshotdir>)\n";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();
  private ObjectMapper _mapper = BatfishObjectMapper.mapper();

  private void checkProcessCommandErrorMessage(
      Command command, String[] parameters, String expected) throws Exception {
    Client client = new Client(new String[] {"-runmode", "interactive"});
    File tempFile = _folder.newFile("writer");
    FileWriter writer = new FileWriter(tempFile);
    client._logger = new BatfishLogger("output", false);
    String[] args = ArrayUtils.addAll(new String[] {command.commandName()}, parameters);
    assertFalse(client.processCommand(args, writer));
    assertThat(client.getLogger().getHistory().toString(500), equalTo(expected));
    writer.close();
  }

  @Test
  public void checkTestInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(TEST, new String[] {}, parameters);
  }

  @Test
  public void testAddAnalysisQuestionInvalidParas() throws Exception {
    testInvalidInput(ADD_ANALYSIS_QUESTIONS, new String[] {}, new String[] {});
  }

  @Test
  public void testAddBatfishOptionInvalidParas() throws Exception {
    testInvalidInput(ADD_BATFISH_OPTION, new String[] {}, new String[] {});
  }

  @Test
  public void testAddBatfishOptionValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testProcessCommandWithValidInput(ADD_BATFISH_OPTION, parameters, "");
  }

  @Test
  public void testAnswerReferenceInvalidParas() throws Exception {
    testInvalidInput(ANSWER_REFERENCE, new String[] {}, new String[] {});
  }

  @Test
  public void testAnswerReferenceValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(ANSWER_REFERENCE, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testAnswerInvalidParas() throws Exception {
    testInvalidInput(ANSWER, new String[] {}, new String[] {});
  }

  @Test
  public void testAnswerValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(ANSWER, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testCatInvalidParas() throws Exception {
    Command command = CAT;
    String[] args = new String[] {command.commandName()};
    Pair<String, String> usage = Command.getUsageMap().get(command);
    String expected =
        String.format(
            "Invalid arguments: %s\n%s %s\n\t%s\n\n",
            Arrays.toString(args), command.commandName(), usage.getFirst(), usage.getSecond());
    checkProcessCommandErrorMessage(command, new String[] {}, expected);
  }

  @Test
  public void testCatValidParas() throws Exception {
    Path tempFilePath = _folder.newFile("temp").toPath();
    String[] parameters = new String[] {tempFilePath.toString()};
    testProcessCommandWithValidInput(CAT, parameters, "");
  }

  @Test
  public void testCheckApiKeyInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(CHECK_API_KEY, new String[] {}, parameters);
  }

  @Test
  public void testClearScreenInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(CLEAR_SCREEN, new String[] {}, parameters);
  }

  @Test
  public void testDefaultCase() throws Exception {
    Client client = new Client(new String[] {"-runmode", "interactive"});
    File tempFile = _folder.newFile("writer");
    FileWriter writer = new FileWriter(tempFile);
    client._logger = new BatfishLogger("output", false);
    String[] args = new String[] {"non-exist command"};
    String expected = "Command failed: Not a valid command: \"non-exist command\"\n";
    assertFalse(client.processCommand(args, writer));
    assertThat(client.getLogger().getHistory().toString(500), equalTo(expected));
  }

  @Test
  public void testDelAnalysisInvalidParas() throws Exception {
    testInvalidInput(DEL_ANALYSIS, new String[] {}, new String[] {});
  }

  @Test
  public void testDelAnalysisQuestionInvalidParas() throws Exception {
    testInvalidInput(DEL_ANALYSIS_QUESTIONS, new String[] {}, new String[] {});
  }

  @Test
  public void testDelAnalysisQuestionValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1", "parameter2"};
    checkProcessCommandErrorMessage(DEL_ANALYSIS_QUESTIONS, parameters, NETWORK_NOT_SET);
  }

  @Test
  public void testDelAnalysisValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(DEL_ANALYSIS, parameters, NETWORK_NOT_SET);
  }

  @Test
  public void testDelBatfishOptionInvalidParas() throws Exception {
    testInvalidInput(DEL_BATFISH_OPTION, new String[] {}, new String[] {});
  }

  @Test
  public void testDelBatfishOptionValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    String expected = "Batfish option parameter1 does not exist\n";
    checkProcessCommandErrorMessage(DEL_BATFISH_OPTION, parameters, expected);
  }

  @Test
  public void testDelNetworkInvalidParas() throws Exception {
    testInvalidInput(DEL_NETWORK, new String[] {}, new String[] {});
  }

  @Test
  public void testDelQuestionInvalidParas() throws Exception {
    testInvalidInput(DEL_QUESTION, new String[] {}, new String[] {});
  }

  @Test
  public void testDelQuestionValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(DEL_QUESTION, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testDelSnapshotInvalidParas() throws Exception {
    testInvalidInput(DEL_SNAPSHOT, new String[] {}, new String[] {});
  }

  @Test
  public void testDelSnapshotValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(DEL_SNAPSHOT, parameters, NETWORK_NOT_SET);
  }

  @Test
  public void testDirInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1", "parameter2"};
    testInvalidInput(DIR, new String[] {}, parameters);
  }

  @Test
  public void testDirValidParas() throws Exception {
    Path tempFilePath = _folder.newFolder("temp").toPath();
    String[] parameters = new String[] {tempFilePath.toString()};
    testProcessCommandWithValidInput(DIR, parameters, "");
  }

  @Test
  public void testEmptyJsonPath() throws IOException {
    JsonNode emptyPath = _mapper.readTree("\"\"");
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(
            String.format("Expecting a JSON dictionary for a Batfish %s", JSON_PATH.getName())));
    Client.validateJsonPath(emptyPath);
  }

  @Test
  public void testEmptyJsonPathRegex() {
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(String.format("A Batfish %s must start with \"/\"", JSON_PATH_REGEX.getName())));
    Client.validateJsonPathRegex("");
  }

  @Test
  public void testExitInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(EXIT, new String[] {}, parameters);
  }

  @Test
  public void testExitValidParas() throws Exception {
    testProcessCommandWithValidInput(EXIT, new String[] {}, "");
  }

  @Test
  public void testGenerateDataplaneReferenceInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(GEN_REFERENCE_DP, new String[] {}, parameters);
  }

  @Test
  public void testGenerateDataplaneReferenceValidParas() throws Exception {
    checkProcessCommandErrorMessage(
        GEN_REFERENCE_DP, new String[] {}, "Active delta snapshot is not set\n");
  }

  @Test
  public void testGenerateDataplaneInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(GEN_DP, new String[] {}, parameters);
  }

  @Test
  public void testGenerateDataplaneValidParas() throws Exception {
    checkProcessCommandErrorMessage(GEN_DP, new String[] {}, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testGetAnalysisAnswersReferenceValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(GET_ANALYSIS_ANSWERS_REFERENCE, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testGetAnalysisAnswersDifferentialValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(
        GET_ANALYSIS_ANSWERS_DIFFERENTIAL, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testGetAnalysisAnswersInvalidParas() throws Exception {
    testInvalidInput(GET_ANALYSIS_ANSWERS, new String[] {}, new String[] {});
  }

  @Test
  public void testGetAnalysisAnswersValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(GET_ANALYSIS_ANSWERS, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testGetAnswersReferenceValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(GET_ANSWER_REFERENCE, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testGetAnswersDifferentialValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(GET_ANSWER_DIFFERENTIAL, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testGetAnswersInvalidParas() throws Exception {
    testInvalidInput(GET_ANSWER, new String[] {}, new String[] {});
  }

  @Test
  public void testGetAnswersValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(GET_ANSWER, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testGetReferenceInvalidParas() throws Exception {
    testInvalidInput(GET_REFERENCE, new String[] {}, new String[] {});
  }

  @Test
  public void testGetReferenceValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(GET_REFERENCE, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testGetInvalidParas() throws Exception {
    testInvalidInput(GET, new String[] {}, new String[] {});
  }

  @Test
  public void testGetQuestionName() throws JSONException {
    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", "testQuestionName")
            .put("description", "test question description"));

    // test if question name is correct
    assertEquals("testQuestionName", Client.getQuestionName(testQuestion, "testquestion"));
  }

  @Test
  public void testGetQuestionNameInvalid1() throws JSONException {
    JSONObject testQuestion = new JSONObject();
    testQuestion.put("instance", new JSONObject().put("description", "test question description"));
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("question testquestion does not have instanceName field in instance");

    // check exception when no instanceName is present
    Client.getQuestionName(testQuestion, "testquestion");
  }

  @Test
  public void testGetQuestionNameInvalid2() {
    JSONObject testQuestion = new JSONObject();
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("question testquestion does not have instance field");

    // check exception when instance itself is not present
    Client.getQuestionName(testQuestion, "testquestion");
  }

  @Test
  public void testGetValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(GET, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testHelpInvalidParas() throws Exception {
    Command command = HELP;
    String[] parameters = new String[] {"-option1"};
    Pair<String, String> usage = Command.getUsageMap().get(command);
    String expected =
        String.format(
            "Invalid arguments: %s []\n%s %s\n\t%s\n\n",
            Arrays.toString(parameters),
            command.commandName(),
            usage.getFirst(),
            usage.getSecond());
    checkProcessCommandErrorMessage(command, parameters, expected);
  }

  @Test
  public void testHelpValidParas() throws Exception {
    String[] parameters = new String[] {"get"};
    Pair<String, String> usage = Command.getUsageMap().get(GET);
    String expected =
        String.format("%s %s\n\t%s\n\n", GET.commandName(), usage.getFirst(), usage.getSecond());
    testProcessCommandWithValidInput(HELP, parameters, expected);
  }

  @Test
  public void testInitAnalysisQuestionInvalidParas() throws Exception {
    testInvalidInput(INIT_ANALYSIS, new String[] {}, new String[] {});
  }

  @Test
  public void testInitNetworkEmptyParasWithOption() throws Exception {
    Command command = INIT_NETWORK;
    String[] args = new String[] {"-setname"};
    Pair<String, String> usage = Command.getUsageMap().get(command);
    String expected =
        String.format(
            "Invalid arguments: %s []\n%s %s\n\t%s\n\n",
            "[-setname]", command.commandName(), usage.getFirst(), usage.getSecond());
    checkProcessCommandErrorMessage(command, args, expected);
  }

  @Test
  public void testInitNetworkInvalidOptions() throws Exception {
    Command command = INIT_NETWORK;
    String invalidOption = "-setnetwork";
    String[] args = new String[] {invalidOption, "parameter1"};
    Pair<String, String> usage = Command.getUsageMap().get(command);
    String expected =
        String.format(
            "Invalid arguments: %s %s\n%s %s\n\t%s\n\n",
            "[-setnetwork]",
            "[parameter1]",
            command.commandName(),
            usage.getFirst(),
            usage.getSecond());
    checkProcessCommandErrorMessage(command, args, expected);
  }

  @Test
  public void testInitNetworkInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1", "parameter2", "parameter3"};
    testInvalidInput(INIT_NETWORK, new String[] {}, parameters);
  }

  @Test
  public void testInitSnapshotReferenceInvalidParas() throws Exception {
    testInvalidInput(INIT_REFERENCE_SNAPSHOT, new String[] {}, new String[] {});
  }

  @Test
  public void testInitSnapshotInvalidParas() throws Exception {
    testInvalidInput(INIT_SNAPSHOT, new String[] {}, new String[] {});
  }

  @Test
  public void testInvalidAddressGroupAndBook() throws IOException {
    String input = "\"addressGroup\""; // no book name
    Type expectedType = ADDRESS_GROUP_AND_BOOK;
    String expectedMessage =
        String.format(
            "A Batfish %s must be a JSON string with two comma-separated values",
            expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidBgpSessionStatusValue() throws IOException {
    String input = "5";
    Type expectedType = BGP_SESSION_STATUS;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidBgpSessionTypeValue() throws IOException {
    String input = "5";
    Type expectedType = BGP_SESSION_TYPE;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidBooleanValue() throws IOException {
    String input = "\"true\"";
    Type expectedType = BOOLEAN;
    String expectedMessage = String.format("It is not a valid JSON %s value", BOOLEAN.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidComparatorValue() throws IOException {
    String input = "\"=>\"";
    Type expectedType = COMPARATOR;
    String expectedMessage =
        "It is not a known comparator. Valid options " + "are: [==, <=, !=, <, >, >=]";
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidDispositionSpecifierValue() throws IOException {
    String input = "5";
    Type expectedType = DISPOSITION_SPEC;
    String expectedMessage =
        String.format("It is not a valid JSON %s value", DISPOSITION_SPEC.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidDoubleValue() throws IOException {
    String input = "\"string\"";
    Type expectedType = DOUBLE;
    String expectedMessage = String.format("It is not a valid JSON %s value", DOUBLE.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidFilterValue() throws IOException {
    String input = "5";
    Type expectedType = FILTER;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidFilterSpecValue() throws IOException {
    String input = "5";
    Type expectedType = FILTER_SPEC;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidFloatValue() throws IOException {
    String input = "\"string\"";
    Type expectedType = FLOAT;
    String expectedMessage = String.format("It is not a valid JSON %s value", FLOAT.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidFlowStateValue() throws IOException {
    String input = "5";
    Type expectedType = FLOW_STATE;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  private void testInvalidInput(Command command, String[] options, String[] parameters)
      throws Exception {
    Pair<String, String> usage = Command.getUsageMap().get(command);
    String expected =
        String.format(
            "Invalid arguments: %s %s\n%s %s\n\t%s\n\n",
            Arrays.toString(options),
            Arrays.toString(parameters),
            command.commandName(),
            usage.getFirst(),
            usage.getSecond());
    checkProcessCommandErrorMessage(command, ArrayUtils.addAll(options, parameters), expected);
  }

  @Test
  public void testInvalidIntegerValue() throws IOException {
    String input = "1.5";
    Type expectedType = INTEGER;
    String expectedMessage = String.format("It is not a valid JSON %s value", INTEGER.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidInterfaceGroupAndBook() throws IOException {
    String input = "\"interfaceGroup\""; // no book name
    Type expectedType = INTERFACE_GROUP_AND_BOOK;
    String expectedMessage =
        String.format(
            "A Batfish %s must be a JSON string with two comma-separated values",
            expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidInterfaceNameValue() throws IOException {
    String input = "5";
    Type expectedType = INTERFACE_NAME;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidInterfaceValue() throws IOException {
    String input = "5";
    Type expectedType = INTERFACE;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidInterfaceTypeValueNonSting() throws IOException {
    String input = "5";
    Type expectedType = INTERFACE_TYPE;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidInterfaceTypeValueBadEnum() throws IOException {
    String input = "\"XOXO\"";
    Type expectedType = INTERFACE_TYPE;
    String expectedMessage =
        String.format("No enum constant %s.XOXO", InterfaceType.class.getName());
    validateTypeWithInvalidInput(
        input, IllegalArgumentException.class, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidInterfacesSpecValue() throws IOException {
    String input = "5";
    Type expectedType = INTERFACE;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidInteriorJavaRegex() {
    String invalidJavaRegex = "/...{\\\\Q8\\\\E}/";
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(
            String.format(
                "Invalid %s at interior of %s", JAVA_REGEX.getName(), JSON_PATH_REGEX.getName())));
    Client.validateJsonPathRegex(invalidJavaRegex);
  }

  @Test
  public void testInvalidIpProtocolValue() throws IOException {
    String input = "\"invalid\"";
    Type expectedType = IP_PROTOCOL;
    String expectedMessage = String.format("Unknown %s string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidIPValue() throws IOException {
    validateTypeWithInvalidInput(
        "\"0.0.0\"", IllegalArgumentException.class, "Invalid IPv4 address: 0.0.0", IP);
  }

  @Test
  public void testInvalidIpSpaceSpecValue() throws IOException {
    String input = "5";
    Type expectedType = IP_SPACE_SPEC;
    String expectedMessage =
        String.format(
            "A Batfish %s must be a JSON string with IpSpaceSpec grammar", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidIpWildcardValue() throws IOException {
    String input = "\"10.168.5.5:10.168.100.$\"";
    String expectedMessage = "Invalid IPv4 address: 10.168.100.$";
    validateTypeWithInvalidInput(
        input, IllegalArgumentException.class, expectedMessage, IP_WILDCARD);
  }

  @Test
  public void testInvalidIpsecSessionStatusValue() throws IOException {
    String input = "5";
    Type expectedType = IPSEC_SESSION_STATUS;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidJavaRegexValue() throws IOException {
    String invalidJavaRegex = "\"...{\\\\Q8\\\\E}\"";
    Type expectedType = JAVA_REGEX;
    String expectedMessage = "It is not a valid Java regular expression";
    validateTypeWithInvalidInput(invalidJavaRegex, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidJsonPath() throws IOException {
    JsonNode invalidPath = _mapper.readTree("\"variable\" : \"I am variable\"");
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(
            String.format("Expecting a JSON dictionary for a Batfish %s", JSON_PATH.getName())));
    Client.validateJsonPath(invalidPath);
  }

  @Test
  public void testInvalidJsonPathRegexValue() throws IOException {
    String input = "\"/pathRegex\"";
    Type expectedType = JSON_PATH_REGEX;
    String expectedMessage =
        String.format("A Batfish %s must end in either \"/\" or \"/i\"", JSON_PATH_REGEX.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidJsonPathValue() throws IOException {
    String input = "{\"variable\" : \"I am variable\"}";
    Type expectedType = JSON_PATH;
    String expectedMessage = String.format("Missing 'path' element of %s", JSON_PATH.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidLocationSpec() throws IOException {
    String input = "5";
    Type expectedType = LOCATION_SPEC;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidLongValue() throws IOException {
    String input = "\"string\"";
    Type expectedType = LONG;
    String expectedMessage = String.format("It is not a valid JSON %s value", LONG.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidNodeRoleDimensionValue() throws IOException {
    String input = "5";
    Type expectedType = NODE_ROLE_DIMENSION;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidPrefixRangeValue() throws IOException {
    String input = "\"10.168.5.5/30:s10-50\"";
    String expectedMessage = "Invalid subrange start: \"s10\"";
    validateTypeWithInvalidInput(input, expectedMessage, PREFIX_RANGE);
  }

  @Test
  public void testInvalidPrefixValue() throws IOException {
    String input = "\"10.168.5.5/30/20\"";
    String expectedMessage = String.format("Invalid prefix string: %s", input);
    validateTypeWithInvalidInput(input, expectedMessage, PREFIX);
  }

  @Test
  public void testInvalidProtocolValue() throws IOException {
    String input = "\"missing\"";
    String expectedMessage =
        String.format(
            "No %s with name: '%s'",
            Protocol.class.getSimpleName(), _mapper.readTree(input).textValue());
    validateTypeWithInvalidInput(input, expectedMessage, PROTOCOL);
  }

  @Test
  public void testInvalidRoutingProtocolSpecValue() throws IOException {
    String input = "5";
    Type expectedType = ROUTING_PROTOCOL_SPEC;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidStructureNameValue() throws IOException {
    String input = "5";
    Type expectedType = STRUCTURE_NAME;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidSubRangeValue() throws IOException {
    String input = "\"10-s50\"";
    String expectedMessage = "Invalid subrange end: \"s50\"";
    validateTypeWithInvalidInput(input, expectedMessage, SUBRANGE);
  }

  @Test
  public void testInvalidVrfValue() throws IOException {
    String input = "5";
    Type expectedType = VRF;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testInvalidZoneValue() throws IOException {
    String input = "5";
    Type expectedType = ZONE;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testJsonPathNoPathAttribute() throws IOException {
    String invalidJsonPath = "{\"variable\" : \"I am variable\"}";
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(String.format("Missing 'path' element of %s", JSON_PATH.getName())));
    Client.validateJsonPath(_mapper.readTree(invalidJsonPath));
  }

  @Test
  public void testJsonPathNotBooleanSuffix() throws IOException {
    String invalidJsonPath = "{\"path\" : \"I am path.\", \"suffix\" : \"I " + "am suffix.\"}";
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(
            String.format("'suffix' element of %s must be a JSON boolean", JSON_PATH.getName())));
    Client.validateJsonPath(_mapper.readTree(invalidJsonPath));
  }

  @Test
  public void testListAnalysisInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(LIST_ANALYSES, new String[] {}, parameters);
  }

  @Test
  public void testListAnalysisValidParas() throws Exception {
    String[] parameters = new String[] {};
    checkProcessCommandErrorMessage(LIST_ANALYSES, parameters, NETWORK_NOT_SET);
  }

  @Test
  public void testListNetworksInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(LIST_NETWORKS, new String[] {}, parameters);
  }

  @Test
  public void testListQuestionsInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(LIST_QUESTIONS, new String[] {}, parameters);
  }

  @Test
  public void testListQuestionsValidParas() throws Exception {
    checkProcessCommandErrorMessage(LIST_QUESTIONS, new String[] {}, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testListSnapshotsInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(LIST_SNAPSHOTS, new String[] {}, parameters);
  }

  @Test
  public void testLoadQuestionFromFile() throws Exception {
    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", "testQuestionName")
            .put("description", "test question description"));
    Path questionJsonPath = _folder.newFile("testquestion.json").toPath();
    CommonUtil.writeFile(questionJsonPath, testQuestion.toString());
    JSONObject question = Client.loadQuestionFromFile(questionJsonPath);

    // checking if actual and loaded JSONs are same
    assertEquals(
        "testQuestionName",
        question.getJSONObject(BfConsts.PROP_INSTANCE).getString(BfConsts.PROP_INSTANCE_NAME));
    assertEquals(
        "test question description",
        question.getJSONObject(BfConsts.PROP_INSTANCE).getString(BfConsts.PROP_DESCRIPTION));
  }

  @Test
  public void testLoadQuestionFromText() throws Exception {
    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", "testQuestionName")
            .put("description", "test question description")
            .put(
                "variables",
                new JSONObject()
                    .put(
                        "var1",
                        new JSONObject()
                            .put("description", "test var1 description")
                            .put("longDescription", "test var1 long description"))));
    JSONObject question = Client.loadQuestionFromText(testQuestion.toString(), "testquestion");

    // checking if actual and loaded JSONs are same
    assertEquals(
        "testQuestionName",
        question.getJSONObject(BfConsts.PROP_INSTANCE).getString(BfConsts.PROP_INSTANCE_NAME));
    assertEquals(
        "test question description",
        question.getJSONObject(BfConsts.PROP_INSTANCE).getString(BfConsts.PROP_DESCRIPTION));
    assertEquals(
        "test var1 description",
        question
            .getJSONObject(BfConsts.PROP_INSTANCE)
            .getJSONObject(BfConsts.PROP_VARIABLES)
            .getJSONObject("var1")
            .getString(BfConsts.PROP_DESCRIPTION));
    assertEquals(
        "test var1 long description",
        question
            .getJSONObject(BfConsts.PROP_INSTANCE)
            .getJSONObject(BfConsts.PROP_VARIABLES)
            .getJSONObject("var1")
            .getString(BfConsts.PROP_LONG_DESCRIPTION));
  }

  @Test
  public void testLoadQuestionFromTextInvalid() {
    JSONObject testQuestion = new JSONObject();

    // checking if exception thrown for instance missing
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("Question in questionSource has no instance data");
    Client.loadQuestionFromText(testQuestion.toString(), "questionSource");
  }

  @Test
  public void testLoadQuestionsNames() throws Exception {
    Client client = new Client(new String[] {"-runmode", "interactive", "-prettyanswers", "false"});
    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", "testQuestionName")
            .put("description", "test question description"));
    Path questionJsonPath = _folder.newFile("testquestion.json").toPath();
    CommonUtil.writeFile(questionJsonPath, testQuestion.toString());
    client._logger = new BatfishLogger("output", false);
    client.processCommand(
        new String[] {LOAD_QUESTIONS.commandName(), questionJsonPath.getParent().toString()}, null);

    // Reading the answer written by load-questions
    Answer answerLoadQuestions =
        _mapper.readValue(
            client.getLogger().getHistory().toString(BatfishLogger.LEVEL_OUTPUT), Answer.class);
    LoadQuestionAnswerElement ae =
        (LoadQuestionAnswerElement) answerLoadQuestions.getAnswerElements().get(0);

    // Checking that question name in answer element matches instanceName in file
    assertEquals("testQuestionName", ae.getAdded().first());
  }

  @Test
  public void testLoadQuestionsFromDir() throws Exception {
    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", "testQuestionName")
            .put("description", "test question description"));
    Path questionJsonPath = _folder.newFile("testquestion.json").toPath();
    CommonUtil.writeFile(questionJsonPath, testQuestion.toString());
    Multimap<String, String> loadedQuestions =
        Client.loadQuestionsFromDir(questionJsonPath.toString());
    Multimap<String, String> expectedMap = HashMultimap.create();
    expectedMap.put("testQuestionName", testQuestion.toString());

    // checking if questions are loaded from disk correctly
    assertEquals(expectedMap, loadedQuestions);
  }

  @Test
  public void testLoadQuestionsFromServer() throws Exception {
    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", "testQuestionName")
            .put("description", "test question description"));
    JSONObject testJson = new JSONObject().put("testQuestion", testQuestion.toString());
    Multimap<String, String> loadedQuestions = Client.loadQuestionsFromServer(testJson);
    Multimap<String, String> expectedMap = HashMultimap.create();
    expectedMap.put("testQuestionName", testQuestion.toString());

    // checking if questions are loaded from json correctly
    assertEquals(expectedMap, loadedQuestions);
  }

  @Test
  public void testLoadQuestionsInvalidParas1() throws Exception {
    testInvalidInput(LOAD_QUESTIONS, new String[] {}, new String[] {"path1", "path2"});
  }

  @Test
  public void testLoadQuestionsInvalidParas2() throws Exception {
    testInvalidInput(
        LOAD_QUESTIONS,
        new String[] {"-loadremote", "-loadlocal"},
        new String[] {"param1", "param2"});
  }

  @Test
  public void testLoadQuestionsValidParas1() throws Exception {
    Path tempFilePath = _folder.newFolder("temp").toPath();
    String[] parameters = new String[] {tempFilePath.toString()};
    testProcessCommandWithValidInput(LOAD_QUESTIONS, parameters, "");
  }

  @Test
  public void testLoadQuestionsValidParas2() throws Exception {
    Path tempFilePath = _folder.newFolder("temp").toPath();
    String[] parameters = new String[] {"-loadremote", tempFilePath.toString()};
    testProcessCommandWithValidInput(LOAD_QUESTIONS, parameters, "");
  }

  @Test
  public void testLoadQuestionsValidParas3() throws Exception {
    String[] parameters = new String[] {"-loadremote"};
    testProcessCommandWithValidInput(LOAD_QUESTIONS, parameters, "");
  }

  @Test
  public void testLoadQuestionsValidParas4() throws Exception {
    String[] parameters = new String[] {};
    testProcessCommandWithValidInput(LOAD_QUESTIONS, parameters, "");
  }

  @Test
  public void testMergeQuestions1() {
    Multimap<String, String> sourceMap = HashMultimap.create();
    sourceMap.put("sourceQuestion", "sourcequestionvalue");
    sourceMap.put("destinationQuestion", "destinationquestionvalue");
    Map<String, String> destMap = new HashMap<>();
    destMap.put("destinationquestion", "destinationquestionvalue");
    LoadQuestionAnswerElement ae = new LoadQuestionAnswerElement();
    Client.mergeQuestions(sourceMap, destMap, ae);
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put("sourcequestion", "sourcequestionvalue");
    expectedMap.put("destinationquestion", "destinationquestionvalue");

    // Test the merging populates ae and destinationquestion get replaced
    assertThat(expectedMap.entrySet(), equalTo(destMap.entrySet()));
    assertEquals(Collections.singleton("destinationQuestion"), ae.getReplaced());
    assertEquals(Collections.singleton("sourceQuestion"), ae.getAdded());
    assertEquals(2, ae.getNumLoaded());
  }

  @Test
  public void testMergeQuestions2() {
    Multimap<String, String> sourceMap = HashMultimap.create();
    sourceMap.put("sourceQuestion", "sourcequestionvalue1");
    sourceMap.put("sourceQuestion", "sourcequestionvalue2");
    Map<String, String> destMap = new HashMap<>();
    LoadQuestionAnswerElement ae = new LoadQuestionAnswerElement();
    Client.mergeQuestions(sourceMap, destMap, ae);

    // Test the merging populates ae and sourcequestion get replaced
    assertEquals(Collections.singleton("sourceQuestion"), ae.getReplaced());
    assertEquals(Collections.singleton("sourceQuestion"), ae.getAdded());
    assertEquals(2, ae.getNumLoaded());
  }

  @Test
  public void testMissingNonOptionalParameterNoValue() {
    Map<String, Variable> variables = new HashMap<>();
    Variable integerVariable = new Variable();
    variables.put("integer", integerVariable);
    _thrown.expect(BatfishException.class);
    String errorMessage = "Missing parameter: integer";
    _thrown.expectMessage(equalTo(errorMessage));
    Client.checkVariableState(variables);
  }

  @Test
  public void testMissingOptionalParameterNoValue() {
    Map<String, Variable> variables = new HashMap<>();
    Variable integerVariable = new Variable();
    integerVariable.setOptional(true);
    Client.checkVariableState(variables);
  }

  @Test
  public void testNestedContainerPathValue() throws IOException {
    String invalidJsonPath = "{\"path\" : {\"innerVariable\" : \"content\"}}";
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(String.format("'path' element of %s must be a JSON string", JSON_PATH.getName())));
    Client.validateJsonPath(_mapper.readTree(invalidJsonPath));
  }

  @Test
  public void testNonStringIpProtocolValue() throws IOException {
    String input = "10";
    Type expectedType = IP_PROTOCOL;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testNonStringIpWildcardValue() throws IOException {
    String input = "10";
    Type expectedType = IP_WILDCARD;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testNonStringOrIntSubRangeValue() throws IOException {
    String input = "false";
    Type expectedType = SUBRANGE;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string or integer", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testNonStringPrefixRangelValue() throws IOException {
    String input = "10";
    Type expectedType = PREFIX_RANGE;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testNonStringPrefixValue() throws IOException {
    String input = "10";
    Type expectedType = PREFIX;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testNonStringProtocolValue() throws IOException {
    String input = "10";
    Type expectedType = PROTOCOL;
    String expectedMessage =
        String.format("A Batfish %s must be a JSON string", expectedType.getName());
    validateTypeWithInvalidInput(input, expectedMessage, expectedType);
  }

  @Test
  public void testNotStringPath() throws IOException {
    String invalidJsonPath = "{\"path\" : 1}";
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(String.format("'path' element of %s must be a JSON string", JSON_PATH.getName())));
    Client.validateJsonPath(_mapper.readTree(invalidJsonPath));
  }

  @Test
  public void testPathRegexInvalidEnd() {
    String invalidEnd = "/pathRegex";
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(
            String.format(
                "A Batfish %s must end in either \"/\" or \"/i\"", JSON_PATH_REGEX.getName())));
    Client.validateJsonPathRegex(invalidEnd);
  }

  @Test
  public void testPathRegexInvalidStart() {
    String invalidStart = "pathRegex";
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(String.format("A Batfish %s must start with \"/\"", JSON_PATH_REGEX.getName())));
    Client.validateJsonPathRegex(invalidStart);
  }

  @Test
  public void testPathRegexWithOnlySlash() {
    String jsonPathRegex = "/";
    Client.validateJsonPathRegex(jsonPathRegex);
  }

  private void testProcessCommandWithValidInput(
      Command command, String[] parameters, String expected) throws Exception {
    Client client = new Client(new String[] {"-runmode", "interactive"});
    File tempFile = _folder.newFile("writer");
    FileWriter writer = new FileWriter(tempFile);
    String[] args = ArrayUtils.addAll(new String[] {command.commandName()}, parameters);
    client._logger = new BatfishLogger("output", false);
    assertTrue(client.processCommand(args, writer));
    assertThat(client.getLogger().getHistory().toString(500), equalTo(expected));
    writer.close();
  }

  @Test
  public void testPromptInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(PROMPT, new String[] {}, parameters);
  }

  @Test
  public void testProvideNonOptionalParameterWithValue() throws IOException {
    Map<String, Variable> variables = new HashMap<>();
    Variable integerVariable = new Variable();
    integerVariable.setValue(_mapper.readTree("3"));
    variables.put("integer", integerVariable);
    Client.checkVariableState(variables);
  }

  @Test
  public void testProvideOptionalParameterWithValue() throws IOException {
    Map<String, Variable> variables = new HashMap<>();
    Variable integerVariable = new Variable();
    integerVariable.setOptional(true);
    integerVariable.setValue(_mapper.readTree("3"));
    variables.put("integer", integerVariable);
    Client.checkVariableState(variables);
  }

  @Test
  public void testPwdInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(PWD, new String[] {}, parameters);
  }

  @Test
  public void testPwdValidParas() throws Exception {
    testProcessCommandWithValidInput(
        PWD,
        new String[] {},
        String.format("working directory = %s\n", System.getProperty("user.dir")));
  }

  @Test
  public void testRunAnalysisReferenceValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(RUN_ANALYSIS_REFERENCE, parameters, NETWORK_NOT_SET);
  }

  @Test
  public void testRunAnalysisDifferentialValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(RUN_ANALYSIS_DIFFERENTIAL, parameters, NETWORK_NOT_SET);
  }

  @Test
  public void testRunAnalysisInvalidParas() throws Exception {
    testInvalidInput(RUN_ANALYSIS, new String[] {}, new String[] {});
  }

  @Test
  public void testRunAnalysisValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(RUN_ANALYSIS, parameters, NETWORK_NOT_SET);
  }

  @Test
  public void testSatisfiedMinElementInput() throws IOException {
    Map<String, JsonNode> parameters = new HashMap<>();
    Map<String, Variable> variables = new HashMap<>();
    String jsonArray =
        "[\"action1\", \"action2\", \"action3\", " + "\"action4\", \"action5\", \"action6\"]";
    parameters.put("actions", _mapper.readTree(jsonArray));
    Variable actionsVariable = new Variable();
    actionsVariable.setType(STRING);
    actionsVariable.setMinElements(5);
    variables.put("actions", actionsVariable);
    Client.validateAndSet(parameters, variables);
  }

  @Test
  public void testSatisfiedMinLengthValue() throws IOException {
    String longString = "\"long enough\"";
    Variable variable = new Variable();
    variable.setMinLength(8);
    variable.setType(STRING);
    Client.validateType(_mapper.readTree(longString), variable);
  }

  @Test
  public void testSetBatfishLogLevelInvalidParas() throws Exception {
    testInvalidInput(SET_BATFISH_LOGLEVEL, new String[] {}, new String[] {});
  }

  @Test
  public void testSetBatfishLogLevelValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(
        SET_BATFISH_LOGLEVEL, parameters, "Undefined loglevel value: parameter1\n");
  }

  @Test
  public void testSetNetworkInvalidParas() throws Exception {
    testInvalidInput(SET_NETWORK, new String[] {}, new String[] {});
  }

  @Test
  public void testSetNetworkValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testProcessCommandWithValidInput(
        SET_NETWORK, parameters, String.format("Active network is now set to %s\n", parameters[0]));
  }

  @Test
  public void testSetReferenceSnapshotInvalidParas() throws Exception {
    testInvalidInput(SET_REFERENCE_SNAPSHOT, new String[] {}, new String[] {});
  }

  @Test
  public void testSetReferenceSnapshotValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testProcessCommandWithValidInput(
        SET_REFERENCE_SNAPSHOT,
        parameters,
        String.format("Reference snapshot is now %s\n", parameters[0]));
  }

  @Test
  public void testSetLogLevelInvalidParas() throws Exception {
    testInvalidInput(SET_LOGLEVEL, new String[] {}, new String[] {});
  }

  @Test
  public void testSetLogLevelValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(
        SET_LOGLEVEL, parameters, "Undefined loglevel value: parameter1\n");
  }

  @Test
  public void testSetPrettyPrintInvalidParas() throws Exception {
    testInvalidInput(SET_PRETTY_PRINT, new String[] {}, new String[] {});
  }

  @Test
  public void testSetPrettyPrintValidParas() throws Exception {
    String[] parameters = new String[] {"true"};
    testProcessCommandWithValidInput(
        SET_PRETTY_PRINT, parameters, "Set pretty printing answers to true\n");
  }

  @Test
  public void testSetSnapshotInvalidParas() throws Exception {
    testInvalidInput(SET_SNAPSHOT, new String[] {}, new String[] {});
  }

  @Test
  public void testSetSnapshotValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    checkProcessCommandErrorMessage(SET_SNAPSHOT, parameters, NETWORK_NOT_SET);
  }

  @Test
  public void testShowApiKeyInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(SHOW_API_KEY, new String[] {}, parameters);
  }

  @Test
  public void testShowApiKeyValidParas() throws Exception {
    testProcessCommandWithValidInput(
        SHOW_API_KEY, new String[] {}, String.format("Current API Key is %s\n", DEFAULT_API_KEY));
  }

  @Test
  public void testShowBatfishLogLevelInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(SHOW_BATFISH_LOGLEVEL, new String[] {}, parameters);
  }

  @Test
  public void testShowBatfishLogLevelValidParas() throws Exception {
    testProcessCommandWithValidInput(
        SHOW_BATFISH_LOGLEVEL, new String[] {}, "Current batfish log level is warn\n");
  }

  @Test
  public void testShowBatfishOptionsInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(SHOW_BATFISH_OPTIONS, new String[] {}, parameters);
  }

  @Test
  public void testShowBatfishOptionsValidParas() throws Exception {
    testProcessCommandWithValidInput(
        SHOW_BATFISH_OPTIONS, new String[] {}, "There are 0 additional batfish options\n");
  }

  @Test
  public void testShowNetworkValidParas() throws Exception {
    testProcessCommandWithValidInput(SHOW_NETWORK, new String[] {}, "Current network is null\n");
  }

  @Test
  public void testShowNetworkInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(SHOW_NETWORK, new String[] {}, parameters);
  }

  @Test
  public void testShowCoordinatorHostInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(SHOW_COORDINATOR_HOST, new String[] {}, parameters);
  }

  @Test
  public void testShowCoordinatorHostValidParas() throws Exception {
    testProcessCommandWithValidInput(
        SHOW_COORDINATOR_HOST, new String[] {}, "Current coordinator host is localhost\n");
  }

  @Test
  public void testShowReferenceSnapshotInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(SHOW_REFERENCE_SNAPSHOT, new String[] {}, parameters);
  }

  @Test
  public void testShowReferenceSnapshotValidParas() throws Exception {
    checkProcessCommandErrorMessage(
        SHOW_REFERENCE_SNAPSHOT, new String[] {}, "Active delta snapshot is not set\n");
  }

  @Test
  public void testShowLogLevelInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(SHOW_LOGLEVEL, new String[] {}, parameters);
  }

  @Test
  public void testShowLogLevelValidParas() throws Exception {
    testProcessCommandWithValidInput(
        SHOW_LOGLEVEL, new String[] {}, "Current client log level is output\n");
  }

  @Test
  public void testShowSnapshotInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(SHOW_SNAPSHOT, new String[] {}, parameters);
  }

  @Test
  public void testShowSnapshotValidParas() throws Exception {
    String[] parameters = new String[] {};
    checkProcessCommandErrorMessage(SHOW_SNAPSHOT, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testShowVersionInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(SHOW_SNAPSHOT, new String[] {}, parameters);
  }

  @Test
  public void testGetConfigurationInvalidParas() throws Exception {
    String[] parameters = new String[] {"parameter1"};
    testInvalidInput(GET_CONFIGURATION, new String[] {}, parameters);
  }

  @Test
  public void testUnsatisfiedMinElementInput() throws IOException {
    Map<String, JsonNode> parameters = new HashMap<>();
    Map<String, Variable> variables = new HashMap<>();
    JsonNode jsonArray = _mapper.readTree("[\"action1\", \"action2\"]");
    parameters.put("actions", jsonArray);
    Variable actionsVariable = new Variable();
    actionsVariable.setType(STRING);
    actionsVariable.setMinElements(5);
    variables.put("actions", actionsVariable);
    _thrown.expect(BatfishException.class);
    String errorMessage =
        String.format(
            "Invalid value for parameter actions: %s. Expecting a "
                + "JSON array of at least 5 elements",
            jsonArray);
    _thrown.expectMessage(equalTo(errorMessage));
    Client.validateAndSet(parameters, variables);
  }

  @Test
  public void testUnsatisfiedMinLengthValue() throws IOException {
    String shortString = "\"short\"";
    Variable variable = new Variable();
    variable.setMinLength(8);
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(equalTo("Must be at least 8 characters in length"));
    Client.validateType(_mapper.readTree(shortString), variable);
  }

  @Test
  public void testUnStringInputWhenExpectString() throws IOException {
    String input = "10";
    String expectedMessage = String.format("A Batfish %s must be a JSON string", STRING.getName());
    validateTypeWithInvalidInput(input, expectedMessage, STRING);
  }

  @Test
  public void testUploadCustomObjectInvalidParas() throws Exception {
    testInvalidInput(UPLOAD_CUSTOM_OBJECT, new String[] {}, new String[] {});
  }

  @Test
  public void testUploadCustomObjectValidParas() throws Exception {
    String[] parameters = new String[] {"parameter1", "parameter2"};
    checkProcessCommandErrorMessage(UPLOAD_CUSTOM_OBJECT, parameters, SNAPSHOT_NOT_SET);
  }

  @Test
  public void testValidateInvalidNode() throws IOException {
    String parameterName = "boolean";
    JsonNode invalidNode = _mapper.readTree("\"I am string\"");
    Variable variable = new Variable();
    variable.setType(BOOLEAN);
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        equalTo(String.format("Invalid value for parameter %s: %s", parameterName, invalidNode)));
    Client.validateNode(invalidNode, variable, parameterName);
  }

  @Test
  public void testValidateNodeNotAllowedValue() throws IOException {
    String parameterName = "boolean";
    JsonNode invalidNode = _mapper.readTree("false");
    Variable variable = new Variable();
    variable.setType(BOOLEAN);
    List<AllowedValue> allowedValues = ImmutableList.of(new AllowedValue("true", "description"));
    variable.setValues(allowedValues);
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(
        String.format("Invalid value: false, allowed values are: %s", allowedValues));
    Client.validateNode(invalidNode, variable, parameterName);
  }

  @Test
  public void testValidateValidNode() throws IOException {
    String parameterName = "boolean";
    JsonNode invalidNode = _mapper.readTree("false");
    Variable variable = new Variable();
    variable.setType(BOOLEAN);
    List<AllowedValue> allowedValues = ImmutableList.of(new AllowedValue("false", "description"));
    variable.setValues(allowedValues);
    Client.validateNode(invalidNode, variable, parameterName);
  }

  @Test
  public void testValidateWithInvalidInput() throws IOException {
    Map<String, JsonNode> parameters = new HashMap<>();
    Map<String, Variable> variables = new HashMap<>();
    parameters.put("integer", _mapper.readTree("10"));
    Variable integerVariable = new Variable();
    integerVariable.setType(INTEGER);
    variables.put("integer", integerVariable);
    parameters.put("boolean", _mapper.readTree("\"true\""));
    Variable booleanVariable = new Variable();
    booleanVariable.setType(BOOLEAN);
    variables.put("boolean", booleanVariable);
    _thrown.expect(BatfishException.class);
    String errorMessage = "Invalid value for parameter boolean: \"true\"";
    _thrown.expectMessage(equalTo(errorMessage));
    Client.validateAndSet(parameters, variables);
  }

  @Test
  public void testValidateWithNullVariableInput() throws IOException {
    Map<String, JsonNode> parameters = new HashMap<>();
    Map<String, Variable> variables = new HashMap<>();
    parameters.put("integer", _mapper.readTree("10"));
    variables.put("integer", null);
    _thrown.expect(BatfishException.class);
    String errorMessage = "No variable named: 'integer' in supplied " + "question template";
    _thrown.expectMessage(equalTo(errorMessage));
    Client.validateAndSet(parameters, variables);
  }

  @Test
  public void testValidateWithValidInput() throws IOException {
    Map<String, JsonNode> parameters = new HashMap<>();
    Map<String, Variable> variables = new HashMap<>();
    parameters.put("integer", _mapper.readTree("10"));
    Variable integerVariable = new Variable();
    integerVariable.setType(INTEGER);
    variables.put("integer", integerVariable);
    parameters.put("boolean", _mapper.readTree("true"));
    Variable booleanVariable = new Variable();
    booleanVariable.setType(BOOLEAN);
    variables.put("boolean", booleanVariable);
    Client.validateAndSet(parameters, variables);
  }

  @Test
  public void testValidAddressGroupAndBook() throws IOException {
    JsonNode addressGroupNode = _mapper.readTree("\"addressGroup, referenceBook\"");
    Variable variable = new Variable();
    variable.setType(ADDRESS_GROUP_AND_BOOK);
    Client.validateType(addressGroupNode, variable);
  }

  @Test
  public void testValidBooleanValue() throws IOException {
    JsonNode booleanNode = _mapper.readTree("true");
    Variable variable = new Variable();
    variable.setType(BOOLEAN);
    Client.validateType(booleanNode, variable);
  }

  @Test
  public void testValidBgpSessionStatusValue() throws IOException {
    JsonNode sessionStatusNode = _mapper.readTree("\"sessionStatus\"");
    Variable variable = new Variable();
    variable.setType(BGP_SESSION_STATUS);
    Client.validateType(sessionStatusNode, variable);
  }

  @Test
  public void testValidBgpSessionTypeValue() throws IOException {
    JsonNode sessionTypeNode = _mapper.readTree("\"sessionType\"");
    Variable variable = new Variable();
    variable.setType(BGP_SESSION_TYPE);
    Client.validateType(sessionTypeNode, variable);
  }

  @Test
  public void testValidComparatorValue() throws IOException {
    JsonNode comparatorNode = _mapper.readTree("\">=\"");
    Variable variable = new Variable();
    variable.setType(COMPARATOR);
    Client.validateType(comparatorNode, variable);
  }

  @Test
  public void testValidDispositionSpecifierValue() throws IOException {
    JsonNode dispositionSpecNode = _mapper.readTree("\"success\"");
    Variable variable = new Variable();
    variable.setType(DISPOSITION_SPEC);
    Client.validateType(dispositionSpecNode, variable);
  }

  @Test
  public void testValidDoubleValue() throws IOException {
    JsonNode doubleNode = _mapper.readTree("15.0");
    Variable variable = new Variable();
    variable.setType(DOUBLE);
    Client.validateType(doubleNode, variable);
  }

  @Test
  public void testValidFilterValue() throws IOException {
    JsonNode filterNode = _mapper.readTree("\"filterName\"");
    Variable variable = new Variable();
    variable.setType(FILTER);
    Client.validateType(filterNode, variable);
  }

  @Test
  public void testValidFilterSpecValue() throws IOException {
    JsonNode filterNode = _mapper.readTree("\"@in(eth0)\"");
    Variable variable = new Variable();
    variable.setType(FILTER_SPEC);
    Client.validateType(filterNode, variable);
  }

  @Test
  public void testValidFloatValue() {
    Float floatValue = 15.0f;
    JsonNode floatNode = _mapper.valueToTree(floatValue);
    Variable variable = new Variable();
    variable.setType(FLOAT);
    Client.validateType(floatNode, variable);
  }

  @Test
  public void testValidFlowStateValue() throws IOException {
    JsonNode flowStateNode = _mapper.readTree("\"ESTABLISHED\"");
    Variable variable = new Variable();
    variable.setType(FLOW_STATE);
    Client.validateType(flowStateNode, variable);
  }

  @Test
  public void testValidIntegerValue() throws IOException {
    JsonNode integerNode = _mapper.readTree("15");
    Variable variable = new Variable();
    variable.setType(INTEGER);
    Client.validateType(integerNode, variable);
  }

  @Test
  public void testValidInterfaceGroupAndBook() throws IOException {
    JsonNode interfaceGroupNode = _mapper.readTree("\"interfaceGroup, referenceBook\"");
    Variable variable = new Variable();
    variable.setType(INTERFACE_GROUP_AND_BOOK);
    Client.validateType(interfaceGroupNode, variable);
  }

  @Test
  public void testValidInterfaceNameValue() throws IOException {
    JsonNode interfaceNode = _mapper.readTree("\"interfaceName\"");
    Variable variable = new Variable();
    variable.setType(INTERFACE);
    Client.validateType(interfaceNode, variable);
  }

  @Test
  public void testValidInterfaceValue() throws IOException {
    JsonNode interfaceNode = _mapper.readTree("\"interfaceName\"");
    Variable variable = new Variable();
    variable.setType(INTERFACE);
    Client.validateType(interfaceNode, variable);
  }

  @Test
  public void testValidInterfaceTypeValue() throws IOException {
    JsonNode interfaceTypeNode = _mapper.readTree("\"physical\"");
    Variable variable = new Variable();
    variable.setType(INTERFACE_TYPE);
    Client.validateType(interfaceTypeNode, variable);
  }

  @Test
  public void testValidInterfacesSpecValue() throws IOException {
    JsonNode interfacesSpecNode = _mapper.readTree("\"TYPE:LOOPBACK\"");
    Variable variable = new Variable();
    variable.setType(INTERFACES_SPEC);
    Client.validateType(interfacesSpecNode, variable);
  }

  @Test
  public void testValidIpProtocolValue() throws IOException {
    JsonNode ipProtocolNode = _mapper.readTree("\"visa\"");
    Variable variable = new Variable();
    variable.setType(IP_PROTOCOL);
    Client.validateType(ipProtocolNode, variable);
  }

  @Test
  public void testValidIpSpaceSpecValue() throws IOException {
    JsonNode addressGroupNode = _mapper.readTree("\"1.1.1.1\"");
    Variable variable = new Variable();
    variable.setType(IP_SPACE_SPEC);
    Client.validateType(addressGroupNode, variable);
  }

  @Test
  public void testValidIPValue() throws IOException {
    JsonNode ipNode = _mapper.readTree("\"0.0.0.0\"");
    Variable variable = new Variable();
    variable.setType(IP);
    Client.validateType(ipNode, variable);
  }

  @Test
  public void testValidIpWildcardValue() throws IOException {
    JsonNode ipWildcardNode = _mapper.readTree("\"10.168.5.5:10.168.100.100\"");
    Variable variable = new Variable();
    variable.setType(IP_WILDCARD);
    Client.validateType(ipWildcardNode, variable);
  }

  @Test
  public void testValidIpsecSessionStatusValue() throws IOException {
    JsonNode ipsecSessionStatusNode = _mapper.readTree("\"sessionStatus\"");
    Variable variable = new Variable();
    variable.setType(IPSEC_SESSION_STATUS);
    Client.validateType(ipsecSessionStatusNode, variable);
  }

  @Test
  public void testValidJavaRegexValue() throws IOException {
    JsonNode inputNode = _mapper.readTree("\".*\"");
    Variable variable = new Variable();
    variable.setType(JAVA_REGEX);
    Client.validateType(inputNode, variable);
  }

  @Test
  public void testValidJsonPath() throws IOException {
    String validJsonPath = "{\"path\" : \"I am path.\", \"suffix\" : true}";
    Client.validateJsonPath(_mapper.readTree(validJsonPath));
  }

  @Test
  public void testValidJsonPathRegexValue() throws IOException {
    JsonNode jsonPathRegexNode = _mapper.readTree("\"/.*/\"");
    Variable variable = new Variable();
    variable.setType(JSON_PATH_REGEX);
    Client.validateType(jsonPathRegexNode, variable);
  }

  @Test
  public void testValidJsonPathValue() throws IOException {
    JsonNode jsonPathNode = _mapper.readTree("{\"path\" : \"I am path.\", \"suffix\" : true}");
    Variable variable = new Variable();
    variable.setType(JSON_PATH);
    Client.validateType(jsonPathNode, variable);
  }

  @Test
  public void testValidLocationSpecValue() throws IOException {
    JsonNode location = _mapper.readTree("\"as1border1\"");
    Variable variable = new Variable();
    variable.setType(LOCATION_SPEC);
    Client.validateType(location, variable);
  }

  @Test
  public void testValidLongValue() {
    Long longValue = 15L;
    JsonNode floatNode = _mapper.valueToTree(longValue);
    Variable variable = new Variable();
    variable.setType(LONG);
    Client.validateType(floatNode, variable);
  }

  @Test
  public void testValidNodeRoleDimensionValue() throws IOException {
    JsonNode nodeRoleDimensionNode = _mapper.readTree("\"roleDimension\"");
    Variable variable = new Variable();
    variable.setType(NODE_ROLE_DIMENSION);
    Client.validateType(nodeRoleDimensionNode, variable);
  }

  @Test
  public void testValidPathRegex() {
    String jsonPathRegex = "/.*/";
    Client.validateJsonPathRegex(jsonPathRegex);
  }

  @Test
  public void testValidPrefixRangeValue() throws IOException {
    JsonNode prefixRangeNode = _mapper.readTree("\"10.168.5.5/30:10-50\"");
    Variable variable = new Variable();
    variable.setType(PREFIX_RANGE);
    Client.validateType(prefixRangeNode, variable);
  }

  @Test
  public void testValidPrefixValue() throws IOException {
    JsonNode prefixNode = _mapper.readTree("\"10.168.5.5/30\"");
    Variable variable = new Variable();
    variable.setType(PREFIX);
    Client.validateType(prefixNode, variable);
  }

  @Test
  public void testValidProtocolValue() throws IOException {
    JsonNode prefixRangeNode = _mapper.readTree("\"http\"");
    Variable variable = new Variable();
    variable.setType(PROTOCOL);
    Client.validateType(prefixRangeNode, variable);
  }

  @Test
  public void testValidRoutingProtocolSpecValue() throws IOException {
    JsonNode rpsNode = _mapper.readTree("\"all\"");
    Variable variable = new Variable();
    variable.setType(ROUTING_PROTOCOL_SPEC);
    Client.validateType(rpsNode, variable);
  }

  @Test
  public void testValidStructureNameValue() throws IOException {
    JsonNode structureNameNode = _mapper.readTree("\"structureName\"");
    Variable variable = new Variable();
    variable.setType(STRUCTURE_NAME);
    Client.validateType(structureNameNode, variable);
  }

  @Test
  public void testValidSubRangeIntegerValue() throws IOException {
    JsonNode subRangeNode = _mapper.readTree("10");
    Variable variable = new Variable();
    variable.setType(SUBRANGE);
    Client.validateType(subRangeNode, variable);
  }

  @Test
  public void testValidSubRangeStringValue() throws IOException {
    JsonNode subRangeNode = _mapper.readTree("\"10-50\"");
    Variable variable = new Variable();
    variable.setType(SUBRANGE);
    Client.validateType(subRangeNode, variable);
  }

  @Test
  public void testValidVrfValue() throws IOException {
    JsonNode vrfNode = _mapper.readTree("\"vrfName\"");
    Variable variable = new Variable();
    variable.setType(VRF);
    Client.validateType(vrfNode, variable);
  }

  @Test
  public void testValidZoneValue() throws IOException {
    JsonNode zoneNode = _mapper.readTree("\"zoneName\"");
    Variable variable = new Variable();
    variable.setType(ZONE);
    Client.validateType(zoneNode, variable);
  }

  private void validateTypeWithInvalidInput(
      String input, Class<? extends Throwable> expectedException, String expectedMessage, Type type)
      throws IOException {
    JsonNode node = _mapper.readTree(input);
    Variable variable = new Variable();
    variable.setType(type);
    _thrown.expect(expectedException);
    _thrown.expectMessage(equalTo(expectedMessage));
    Client.validateType(node, variable);
  }

  private void validateTypeWithInvalidInput(String input, String expectedMessage, Type type)
      throws IOException {
    validateTypeWithInvalidInput(input, BatfishException.class, expectedMessage, type);
  }

  @Test
  public void getPatch() throws DiffException {
    String expected = "1\n2\n3";
    String actual = "1\n2";

    assertThat(
        Client.getPatch(expected, actual, "expected.txt", "actual.txt"),
        equalTo(
            "--- expected.txt\n"
                + "+++ actual.txt\n"
                + "@@ -1,3 +1,2 @@\n"
                + " 1\n"
                + " 2\n"
                + "-3"));
  }
}
