package org.batfish.allinone;

import com.google.common.base.Strings;
import com.uber.jaeger.Configuration;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import io.opentracing.util.GlobalTracer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.batfish.allinone.config.Settings;
import org.batfish.client.Client;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;

public class AllInOne {

  private final String[] _args;
  private Client _client;
  private BatfishLogger _logger;
  private Settings _settings;

  public AllInOne(String[] args) {
    _args = args;
  }

  private static String[] getArgArrayFromString(String argString) {
    if (Strings.isNullOrEmpty(argString)) {
      return new String[0];
    }
    return argString.trim().split("\\s+");
  }

  public void run() {
    try {
      _settings = new Settings(_args);
    } catch (Exception e) {
      System.err.println("org.batfish.allinone: Initialization failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }

    String argString =
        String.format(
            "%s -%s %s -%s %s",
            _settings.getClientArgs(),
            org.batfish.client.config.Settings.ARG_LOG_LEVEL,
            _settings.getLogLevel(),
            org.batfish.client.config.Settings.ARG_RUN_MODE,
            _settings.getRunMode());

    if (_settings.getLogFile() != null) {
      argString +=
          String.format(
              " -%s %s", org.batfish.client.config.Settings.ARG_LOG_FILE, _settings.getLogFile());
    }

    if (_settings.getCommandFile() != null) {
      argString +=
          String.format(
              " -%s %s",
              org.batfish.client.config.Settings.ARG_COMMAND_FILE, _settings.getCommandFile());
    }

    if (_settings.getTestrigDir() != null) {
      argString +=
          String.format(
              " -%s %s",
              org.batfish.client.config.Settings.ARG_TESTRIG_DIR, _settings.getTestrigDir());
    }

    if (_settings.getTracingEnable() && !GlobalTracer.isRegistered()) {
      initTracer();
    }

    argString +=
        String.format(
            " -%s %s",
            org.batfish.client.config.Settings.ARG_TRACING_ENABLE, _settings.getTracingEnable());

    // if we are not running the client, we were like not specified a cmdfile.
    // lets do a dummy cmdfile do client initialization does not barf
    if (!_settings.getRunClient() && _settings.getCommandFile() == null) {
      argString +=
          String.format(
              " -%s %s", org.batfish.client.config.Settings.ARG_COMMAND_FILE, "dummy_allinone");
    }

    String[] initialArgArray = getArgArrayFromString(argString);
    List<String> clientArgs = new ArrayList<>(Arrays.asList(initialArgArray));
    final String[] argArray = clientArgs.toArray(new String[] {});

    try {
      _client = new Client(argArray);
      _logger = _client.getLogger();
      _logger.debugf("Started client with args: %s\n", Arrays.toString(argArray));
    } catch (Exception e) {
      System.err.printf(
          "Client initialization failed with args: %s\nExceptionMessage: %s\n",
          argString, e.getMessage());
      System.exit(1);
    }

    runCoordinator();

    runBatfish();

    if (_settings.getRunClient()) {
      _client.run(new LinkedList<String>());
      // The program does not terminate without it if the user misses the
      // quit command
      System.exit(0);
    } else {
      // sleep indefinitely, in chunks, since the client does not keep us
      // alive
      try {
        while (true) {
          Thread.sleep(10 * 60 * 1000); // 10 minutes
          _logger.info("allinone: still alive ....\n");
        }
      } catch (Exception ex) {
        String stackTrace = ExceptionUtils.getFullStackTrace(ex);
        System.err.println(stackTrace);
      }
    }
  }

  private void initTracer() {
    GlobalTracer.register(
        new Configuration(
                BfConsts.PROP_ALLINONE_SERVICE,
                new SamplerConfiguration(ProbabilisticSampler.TYPE, 1),
                new ReporterConfiguration(
                    false,
                    _settings.getTracingAgentHost(),
                    _settings.getTracingAgentPort(),
                    1000,
                    // flush internal in ms
                    10000)
                // max buffered Spans
                )
            .getTracer());
  }

  private void runBatfish() {

    String batfishArgs =
        String.format(
            "%s -%s -%s %s -%s %s",
            _settings.getBatfishArgs(),
            org.batfish.config.Settings.ARG_SERVICE_MODE,
            org.batfish.config.Settings.ARG_COORDINATOR_REGISTER,
            "true",
            org.batfish.config.Settings.ARG_TRACING_ENABLE,
            _settings.getTracingEnable());
    String[] initialArgArray = getArgArrayFromString(batfishArgs);
    List<String> args = new ArrayList<>(Arrays.asList(initialArgArray));
    final String[] argArray = args.toArray(new String[] {});
    _logger.debugf("Starting batfish worker with args: %s\n", Arrays.toString(argArray));
    Thread thread =
        new Thread("batfishThread") {
          @Override
          public void run() {
            try {
              org.batfish.main.Driver.main(argArray, _logger);
            } catch (Exception e) {
              _logger.errorf(
                  "Initialization of batfish failed with args: %s\nExceptionMessage: %s\n",
                  Arrays.toString(argArray), e.getMessage());
            }
          }
        };
    thread.start();
  }

  private void runCoordinator() {
    String coordinatorArgs =
        String.format(
            "%s -%s %s",
            _settings.getCoordinatorArgs(),
            org.batfish.coordinator.config.Settings.ARG_TRACING_ENABLE,
            _settings.getTracingEnable());
    String[] initialArgArray = getArgArrayFromString(coordinatorArgs);
    List<String> args = new ArrayList<>(Arrays.asList(initialArgArray));
    final String[] argArray = args.toArray(new String[] {});
    _logger.debugf("Starting coordinator with args: %s\n", Arrays.toString(argArray));

    Thread thread =
        new Thread("coordinatorThread") {
          @Override
          public void run() {
            try {
              org.batfish.coordinator.Main.main(argArray, _logger);
            } catch (Exception e) {
              _logger.errorf(
                  "Initialization of coordinator failed with args: %s\nExceptionMessage: %s\n",
                  Arrays.toString(argArray), e.getMessage());
            }
          }
        };

    thread.start();
  }
}
