package org.batfish.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.main.Driver;

/**
 * Class to execute a list of jobs in a thread pool of adaptable size using {@link Executors}. The
 * execution can be forced to be sequential by setting the appropriate property in {@link Settings}
 */
public class BatfishJobExecutor {

  private final BatfishLogger _logger;

  private final Settings _settings;

  private int _finishedJobs;

  private int _totalJobs;

  private AtomicInteger _completed;

  private double _finishedPercent;

  private BatfishJobExecutor(Settings settings, BatfishLogger logger) {
    _settings = settings;
    _logger = logger;
  }

  /**
   * @param settings {@link Settings} containing the global settings for running the job
   * @param logger {@link BatfishLogger} used to log the jobs result or status
   * @param jobs {@link List} of jobs to be executed, jobs are of type {@link JobT}
   * @param output data structure to which the result of the job will be applied, it should be of
   *     generic type {@link OutputT}
   * @param answerElement {@link AnswerElement} containing the result of the job
   * @param haltOnProcessingError whether to halt on processing error
   * @param description description of the jobs submitted to the executor
   * @param <JobT> type of job executed in the executor
   * @param <AnswerElementT> type of {@link AnswerElement} to which {@link BatfishJobResult} will be
   *     applied
   * @param <JobResultT> type of {@link BatfishJobResult} which will contain the result of the job
   * @param <OutputT> type of data structure to which job result will be applied
   */
  public static <
          JobT extends BatfishJob<JobResultT>,
          AnswerElementT extends AnswerElement,
          JobResultT extends BatfishJobResult<OutputT, AnswerElementT>,
          OutputT>
      void runJobsInExecutor(
          Settings settings,
          BatfishLogger logger,
          List<JobT> jobs,
          OutputT output,
          AnswerElementT answerElement,
          boolean haltOnProcessingError,
          String description) {
    getBatfishJobExecutor(settings, logger)
        .executeJobs(jobs, output, answerElement, haltOnProcessingError, description);
  }

  static BatfishJobExecutor getBatfishJobExecutor(Settings settings, BatfishLogger logger) {
    return new BatfishJobExecutor(settings, logger);
  }

  /**
   * @param jobs jobs to be executed, of type {@link JobT}
   * @param output data structure to which the result of the job will be applied, it should be of
   *     generic type {@link OutputT}
   * @param answerElement {@link AnswerElement} containing the detail of the jobs executed
   * @param haltOnProcessingError whether to halt on processing error
   * @param description description of the jobs submitted to the executor
   * @param <JobT> type of job executed in the executor
   * @param <AnswerElementT> type of {@link AnswerElement} to which {@link BatfishJobResult} will be
   *     applied
   * @param <JobResultT> type of {@link BatfishJobResult} which will contain the result of the job
   * @param <OutputT> type of data structure to which job result will be applied
   */
  private <
          JobT extends BatfishJob<JobResultT>,
          AnswerElementT extends AnswerElement,
          JobResultT extends BatfishJobResult<OutputT, AnswerElementT>,
          OutputT>
      void executeJobs(
          List<JobT> jobs,
          OutputT output,
          AnswerElementT answerElement,
          boolean haltOnProcessingError,
          String description) {

    // Initializing executors
    ExecutorService pool = createExecutorService();
    ExecutorCompletionService<JobResultT> completionService = new ExecutorCompletionService<>(pool);

    if (!_settings.getSequential() && _settings.getShuffleJobs()) {
      Collections.shuffle(jobs);
    }

    for (JobT job : jobs) {
      completionService.submit(job);
    }

    initializeJobsStats(jobs, description);
    boolean processingError = false;
    List<BatfishException> failureCauses = new ArrayList<>();
    try {
      for (int i = 0; i < jobs.size(); i++) {

        JobResultT result = null;
        try {
          // getting the result of the job
          result = completionService.take().get();
        } catch (InterruptedException e) {
          throw new BatfishException("Job didn't finish", e);
        } catch (ExecutionException e) {
          throw new BatfishException("Error executing job", e);
        }

        markJobCompleted();
        boolean jobResultError =
            handleJobResult(result, output, answerElement, failureCauses, haltOnProcessingError);
        if (jobResultError) {
          processingError = true;
        }
      }
    } finally {
      pool.shutdown();
    }

    if (processingError) {
      handleProcessingError(jobs, failureCauses, haltOnProcessingError);
    } else if (!_logger.isActive(BatfishLogger.LEVEL_INFO)) {
      _logger.info("All jobs executed successfully\n");
    }
  }

  private ExecutorService createExecutorService() {
    if (_settings.getSequential()) {
      return Executors.newSingleThreadExecutor();
    }
    // if parallel processing is allowed
    int maxConcurrentThreads = Runtime.getRuntime().availableProcessors();
    int numConcurrentThreads = Math.min(maxConcurrentThreads, _settings.getJobs());
    return Executors.newFixedThreadPool(numConcurrentThreads);
  }

  <
          JobResultT extends BatfishJobResult<OutputT, AnswerElementT>,
          AnswerElementT extends AnswerElement,
          OutputT>
      String getFailureMessage(JobResultT result) {
    String time = CommonUtil.getTime(result.getElapsedTime());
    String failureMessage =
        String.format(
            "Failure running job after elapsed time: %s\n-----"
                + "BEGIN JOB LOG-----\n%s\n-----END JOB LOG-----",
            time, result.getHistory().toString(BatfishLogger.getLogLevel(_settings.getLogLevel())));
    return failureMessage;
  }

  <
          JobResultT extends BatfishJobResult<OutputT, AnswerElementT>,
          AnswerElementT extends AnswerElement,
          OutputT>
      String getSuccessMessage(JobResultT result) {
    String time = CommonUtil.getTime(result.getElapsedTime());
    String successMessage =
        String.format(
            "Job terminated successfully with result: %s after elapsed time: %s - %d/%d "
                + "(%.1f%%) complete\n",
            result, time, _finishedJobs, _totalJobs, _finishedPercent);
    return successMessage;
  }

  /**
   * Handles the result of a {@link BatfishJob}
   *
   * @param result result of Job
   * @param output data structure to which the result of the job will be applied, it should be of
   *     generic type {@link OutputT}
   * @param answerElement {@link AnswerElement} for the execution of jobs
   * @param failureCauses list of failure causes for all jobs
   * @param haltOnProcessingError whether there is a processing error
   * @param <JobResultT> type of {@link BatfishJobResult}
   * @param <OutputT> type of output
   * @param <AnswerElementT> type of {@link AnswerElement}
   * @return {@code true} if processing error
   */
  <
          JobResultT extends BatfishJobResult<OutputT, AnswerElementT>,
          OutputT,
          AnswerElementT extends AnswerElement>
      boolean handleJobResult(
          JobResultT result,
          OutputT output,
          AnswerElementT answerElement,
          List<BatfishException> failureCauses,
          boolean haltOnProcessingError) {
    Throwable failureCause = result.getFailureCause();
    if (failureCause == null) {
      result.applyTo(output, _logger, answerElement);
      _logger.info(getSuccessMessage(result));
      return false;
    }
    // if there were failures
    String failureMessage = getFailureMessage(result);
    BatfishException bfc = new BatfishException(failureMessage, failureCause);
    if (_settings.getExitOnFirstError()) {
      result.appendHistory(_logger);
      throw bfc;
    }
    // we keep the failure cause and proceed
    result.appendHistory(_logger);
    _logger.errorf("%s:\n\t%s", failureMessage, ExceptionUtils.getStackTrace(failureCause));
    failureCauses.add(bfc);
    if (!haltOnProcessingError) {
      result.applyTo(output, _logger, answerElement);
    }
    return true;
  }

  <JobT> void handleProcessingError(
      List<JobT> jobs, List<BatfishException> failureCauses, boolean haltOnProcessingError) {
    int numJobs = jobs.size();
    int numFailed = failureCauses.size();
    int numSucceeded = numJobs - numFailed;
    _logger.infof("%d jobs succeeded; %d jobs failed\n", numSucceeded, numFailed);
    if (haltOnProcessingError) {
      BatfishException e = new BatfishException(JOB_FAILURE_MESSAGE);
      failureCauses.forEach(e::addSuppressed);
      throw e;
    }
  }

  <JobT> void initializeJobsStats(List<JobT> jobs, String description) {
    _finishedJobs = 0;
    _totalJobs = jobs.size();
    _completed = Driver.newBatch(_settings, description, _totalJobs);
    _finishedPercent = 0;
  }

  void markJobCompleted() {
    _finishedJobs++;
    _completed.incrementAndGet();
    _finishedPercent = 100 * ((double) _finishedJobs) / _totalJobs;
  }

  // Visible only for testing.
  static final String JOB_FAILURE_MESSAGE = "Fatal exception due to failure of at least one job";
}
