package org.batfish.job;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BatfishLogger.BatfishLoggerHistory;
import org.batfish.common.CompositeBatfishException;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.answers.AnswerElement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests for {@link BatfishJobExecutor}. */
public class BatfishJobExecutorTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();
  BatfishLogger _logger;

  @Before
  public void setup() {
    _logger = new BatfishLogger("info", false);
  }

  @Test
  public void testExecuteJobsResults() {
    Settings settings = new Settings();
    settings.setHaltOnParseError(false);
    settings.setSequential(false);

    List<BfTestJob> jobs = new ArrayList<>();
    jobs.add(new BfTestJob(settings, "result1"));
    jobs.add(new BfTestJob(settings, "result2"));

    Set<String> output = new HashSet<>();
    BfTestAnswerElement ae = new BfTestAnswerElement();
    BatfishJobExecutor<BfTestJob, BfTestAnswerElement, BfTestResult, Set<String>> executor =
        new BatfishJobExecutor<>(
            settings, _logger, settings.getHaltOnParseError(), "Test Job Executor");
    executor.executeJobs(jobs, output, ae);

    // checking the outputs produced by the tasks
    assertEquals(output, Sets.newHashSet("result1", "result2"));
  }

  @Test
  public void testExecuteJobsTime() {
    Settings settings = new Settings();
    settings.setHaltOnParseError(false);

    List<BfTestJob> jobs = new ArrayList<>();
    jobs.add(new BfTestJob(settings, "result1"));
    jobs.add(new BfTestJob(settings, "result2"));

    Set<String> output = new HashSet<>();
    BfTestAnswerElement ae = new BfTestAnswerElement();
    BatfishJobExecutor<BfTestJob, BfTestAnswerElement, BfTestResult, Set<String>> executor =
        new BatfishJobExecutor<>(
            settings, _logger, settings.getHaltOnParseError(), "Test Job Executor");
    long startTime = System.currentTimeMillis();
    executor.executeJobs(jobs, output, ae);
    long elapsedTime = System.currentTimeMillis() - startTime;

    // tasks executed in parallel should take less than sequential time in most normal cases
    assertThat(elapsedTime, lessThanOrEqualTo(2000L));
  }

  @Test
  public void testHandleJobResultSuccess() {
    Settings settings = new Settings();
    settings.setHaltOnParseError(false);

    // initializing executor
    BatfishJobExecutor<BfTestJob, BfTestAnswerElement, BfTestResult, Set<String>> executor =
        new BatfishJobExecutor<>(
            settings, _logger, settings.getHaltOnParseError(), "Test Job Executor");
    executor.initializeJobsStats(Lists.newArrayList(new BfTestJob(settings, "result1")));

    // Simulating finishing of a job and handling the result
    BfTestResult bfTestResult = new BfTestResult(1000L, _logger.getHistory(), "result");
    Set<String> output = new HashSet<>();
    List<BatfishException> failureCauses = new ArrayList<>();
    BfTestAnswerElement ae = new BfTestAnswerElement();
    executor.updateJobsStats();
    executor.handleJobResult(bfTestResult, output, ae, failureCauses);

    // checking the log of the executor for the job finished
    assertEquals(
        _logger.getHistory().toString(400),
        String.format(
            "Job terminated successfully with result: %s after elapsed time: %s - %d/%d "
                + "(%.1f%%) complete\n",
            bfTestResult.toString(),
            CommonUtil.getTime(bfTestResult.getElapsedTime()),
            1,
            1,
            100.0));
  }

  @Test
  public void testHandleJobResultFailure() {
    Settings settings = new Settings();
    settings.setHaltOnParseError(false);

    // initializing executor
    BatfishJobExecutor<BfTestJob, BfTestAnswerElement, BfTestResult, Set<String>> executor =
        new BatfishJobExecutor<>(
            settings, _logger, settings.getHaltOnParseError(), "Test Job Executor");
    executor.initializeJobsStats(Lists.newArrayList(new BfTestJob(settings, "result1")));

    // Simulating failure of a job and handling the result
    BfTestResult bfTestResult =
        new BfTestResult(
            1000L, _logger.getHistory(), new BatfishException("Test Job Failure Message"));
    Set<String> output = new HashSet<>();
    List<BatfishException> failureCauses = new ArrayList<>();
    BfTestAnswerElement ae = new BfTestAnswerElement();
    executor.updateJobsStats();
    executor.handleJobResult(bfTestResult, output, ae, failureCauses);

    //checking that correct failure message is written in the log
    assertEquals(
        failureCauses.get(0).getMessage(),
        String.format(
            "Failure running job after elapsed time: %s\n-----"
                + "BEGIN JOB LOG-----\n\n-----END JOB LOG-----",
            CommonUtil.getTime(bfTestResult.getElapsedTime())));
  }

  @Test
  public void testHandleProcessingError() {
    Settings settings = new Settings();
    settings.setHaltOnParseError(true);

    // initializing executor
    BatfishJobExecutor<BfTestJob, BfTestAnswerElement, BfTestResult, Set<String>> executor =
        new BatfishJobExecutor<>(
            settings, _logger, settings.getHaltOnParseError(), "Test Job Executor");
    List<BatfishException> failureCauses = new ArrayList<>();

    // checking if the exception thrown has correct class and message
    _thrown.expect(CompositeBatfishException.class);
    _thrown.expectMessage("Fatal exception due to failure of at least one job");
    executor.handleProcessingError(
        Lists.newArrayList(new BfTestJob(settings, "result1")), failureCauses);
  }

  private class BfTestJob extends BatfishJob<BfTestResult> {
    private String _testValue;

    public BfTestJob(Settings settings, String testValue) {
      super(settings);
      _testValue = testValue;
    }

    @Override
    public BfTestResult call() throws Exception {
      long startTime = System.currentTimeMillis();
      long elapsedTime;
      try {
        TimeUnit.SECONDS.sleep(1);
        elapsedTime = System.currentTimeMillis() - startTime;
        return new BfTestResult(elapsedTime, _logger.getHistory(), _testValue);
      } catch (InterruptedException e) {
        throw new IllegalStateException("task interrupted", e);
      }
    }
  }

  private class BfTestResult extends BatfishJobResult<Set<String>, BfTestAnswerElement> {
    private String _result;

    public BfTestResult(long elapsedTime, BatfishLoggerHistory history, String result) {
      super(elapsedTime, history);
      _result = result;
    }

    public BfTestResult(long elapsedTime, BatfishLoggerHistory history, Throwable failureCause) {
      super(elapsedTime, history, failureCause);
    }

    @Override
    public void applyTo(
        Set<String> output, BatfishLogger logger, BfTestAnswerElement answerElement) {
      output.add(_result);
      answerElement.getOutputs().add(_result);
    }

    @Override
    public void appendHistory(BatfishLogger logger) {
      logger.append(_history, "");
    }
  }

  private class BfTestAnswerElement implements AnswerElement {
    private Set<String> _outputs;

    public BfTestAnswerElement() {
      _outputs = new HashSet<>();
    }

    public Set<String> getOutputs() {
      return _outputs;
    }
  }
}
