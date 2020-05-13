package org.batfish.client;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.batfish.common.BatfishException;

public enum Command {
  ADD_BATFISH_OPTION("add-batfish-option"),
  ANSWER("answer"),
  DEBUG_DELETE("debug-delete"),
  DEBUG_GET("debug-get"),
  DEBUG_POST("debug-post"),
  DEBUG_PUT("debug-put"),
  DEL_BATFISH_OPTION("del-batfish-option"),
  DEL_NETWORK("del-network"),
  EXIT("exit"),
  GEN_DP("generate-dataplane"),
  GET("get"),
  GET_POJO_TOPOLOGY("get-pojo-topology"),
  HELP("help"),
  INIT_NETWORK("init-network"),
  INIT_REFERENCE_SNAPSHOT("init-reference-snapshot"),
  INIT_SNAPSHOT("init-snapshot"),
  LOAD_QUESTIONS("load-questions"),
  QUIT("quit"),
  SET_BATFISH_LOGLEVEL("set-batfish-loglevel"),
  SET_LOGLEVEL("set-loglevel"),
  SET_NETWORK("set-network"),
  SET_REFERENCE_SNAPSHOT("set-reference-snapshot"),
  SET_SNAPSHOT("set-snapshot"),
  SHOW_API_KEY("show-api-key"),
  SHOW_BATFISH_LOGLEVEL("show-batfish-loglevel"),
  SHOW_BATFISH_OPTIONS("show-batfish-options"),
  SHOW_COORDINATOR_HOST("show-coordinator-host"),
  SHOW_LOGLEVEL("show-loglevel"),
  SHOW_NETWORK("show-network"),
  SHOW_REFERENCE_SNAPSHOT("show-reference-snapshot"),
  SHOW_SNAPSHOT("show-snapshot"),
  TEST("test"),
  VALIDATE_TEMPLATE("validate-template");

  public static class CommandUsage {
    @Nonnull private final String _usage;
    @Nonnull private final String _description;

    public CommandUsage(@Nonnull String usage, @Nonnull String description) {
      _usage = usage;
      _description = description;
    }

    @Nonnull
    public String getDescription() {
      return _description;
    }

    @Nonnull
    public String getUsage() {
      return _usage;
    }
  }

  public enum TestComparisonMode {
    COMPAREANSWER,
    COMPAREALL,
    COMPAREFAILURES,
    COMPARESUMMARY,
    JSON,
    RAW
  }

  private static final Map<String, Command> _nameMap = buildNameMap();

  private static final Map<Command, CommandUsage> _usageMap = buildUsageMap();

  private static Map<String, Command> buildNameMap() {
    ImmutableMap.Builder<String, Command> map = ImmutableMap.builder();
    for (Command value : Command.values()) {
      String name = value._name;
      map.put(name, value);
    }
    return map.build();
  }

  private static Map<Command, CommandUsage> buildUsageMap() {
    Map<Command, CommandUsage> descs = new TreeMap<>();
    descs.put(
        ADD_BATFISH_OPTION,
        new CommandUsage(
            "<option-key> [<option-value> [<option-value>] ... ]",
            "Additional options to pass to Batfish"));
    descs.put(
        ANSWER,
        new CommandUsage(
            "<template-name> [differential={true,false}] [questionName=name] [param1=value1 [param2=value2] ...]",
            "Answer the template by name for the current snapshot"));
    descs.put(
        DEL_BATFISH_OPTION,
        new CommandUsage("<option-key>", "Stop passing this option to Batfish"));
    descs.put(DEL_NETWORK, new CommandUsage("<network-name>", "Delete the specified network"));
    descs.put(EXIT, new CommandUsage("", "Terminate interactive client session"));
    descs.put(GEN_DP, new CommandUsage("", "Generate dataplane for the current snapshot"));
    descs.put(
        GET,
        new CommandUsage(
            "<question-type>  [param1=value1 [param2=value2] ...]",
            "Answer the question by type for the current snapshot"));
    descs.put(
        GET_POJO_TOPOLOGY, new CommandUsage("", "Get the POJO topology for the current snapshot"));
    descs.put(HELP, new CommandUsage("[command]", "Print the list of supported commands"));
    descs.put(
        INIT_NETWORK,
        new CommandUsage(
            "[-setname <network-name> | <network-name-prefix>]", "Initialize a new network"));
    descs.put(
        INIT_REFERENCE_SNAPSHOT,
        new CommandUsage(
            "[-autoanalyze] <snapshot zipfile or directory> [<snapshot-name>]",
            "Initialize the reference snapshot"));
    descs.put(
        INIT_SNAPSHOT,
        new CommandUsage(
            "[-autoanalyze] <snapshot zipfile or directory> [<snapshot-name>]",
            "Initialize the snapshot"));
    descs.put(
        LOAD_QUESTIONS,
        new CommandUsage(
            "[-loadremote] [path to local directory containing question json files]",
            "Load questions from local directory, -loadremote loads questions from coordinator, "
                + "if both are specified, questions from local directory overwrite the remote "
                + "questions"));
    descs.put(QUIT, new CommandUsage("", "Terminate interactive client session"));
    descs.put(
        SET_BATFISH_LOGLEVEL,
        new CommandUsage(
            "<debug|info|output|warn|error>", "Set the batfish loglevel. Default is warn"));
    descs.put(
        SET_LOGLEVEL,
        new CommandUsage(
            "<debug|info|output|warn|error>", "Set the client loglevel. Default is output"));
    descs.put(SET_NETWORK, new CommandUsage("<network-name>", "Set the current network"));
    descs.put(
        SET_REFERENCE_SNAPSHOT, new CommandUsage("<snapshot-name>", "Set the reference snapshot"));
    descs.put(SET_SNAPSHOT, new CommandUsage("<snapshot-name>", "Set the current snapshot"));
    descs.put(SHOW_API_KEY, new CommandUsage("", "Show API Key"));
    descs.put(SHOW_BATFISH_LOGLEVEL, new CommandUsage("", "Show current batfish loglevel"));
    descs.put(
        SHOW_BATFISH_OPTIONS,
        new CommandUsage("", "Show the additional options that will be sent to batfish"));
    descs.put(SHOW_COORDINATOR_HOST, new CommandUsage("", "Show coordinator host"));
    descs.put(SHOW_LOGLEVEL, new CommandUsage("", "Show current client loglevel"));
    descs.put(SHOW_NETWORK, new CommandUsage("", "Show active network"));
    descs.put(SHOW_REFERENCE_SNAPSHOT, new CommandUsage("", "Show reference snapshot"));
    descs.put(SHOW_SNAPSHOT, new CommandUsage("", "Show current snapshot"));
    descs.put(
        TEST,
        new CommandUsage(
            "["
                + Arrays.stream(TestComparisonMode.values())
                    .map(v -> '-' + v.toString())
                    .collect(Collectors.joining("|"))
                + "] <ref file> <command>",
            "Run the command and compare its output to the ref file (used for testing)"));
    descs.put(
        VALIDATE_TEMPLATE,
        new CommandUsage(
            "<template-file> [param1=value1 [param2=value2] ...]", "Validate the template"));
    return descs;
  }

  public static Command fromName(String name) {
    Command instance = _nameMap.get(name.toLowerCase());
    if (instance == null) {
      throw new BatfishException("Not a valid command: \"" + name + "\"");
    }
    return instance;
  }

  public static Map<String, Command> getNameMap() {
    return _nameMap;
  }

  public static Map<Command, CommandUsage> getUsageMap() {
    return _usageMap;
  }

  private final String _name;

  Command(String name) {
    _name = name;
  }

  public String commandName() {
    return _name;
  }
}
