package org.batfish.coordinator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.UUID;
import java.util.zip.ZipException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Container;
import org.batfish.common.CoordConsts;
import org.batfish.common.Version;
import org.batfish.common.WorkItem;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.coordinator.config.Settings;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.FormDataParam;

/** */

/** */
@Path(CoordConsts.SVC_CFG_WORK_MGR)
public class WorkMgrService {

  BatfishLogger _logger = Main.getLogger();
  Settings _settings = Main.getSettings();

  private static JSONArray successResponse(Object entity) {
    return new JSONArray(Arrays.asList(CoordConsts.SVC_KEY_SUCCESS, entity));
  }

  private static JSONArray failureResponse(Object entity) {
    return new JSONArray(Arrays.asList(CoordConsts.SVC_KEY_FAILURE, entity));
  }

  /**
   * Check if an API key is valid
   *
   * @param apiKey The API key to check
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_CHECK_API_KEY)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray checkApiKey(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion) {
    try {
      _logger.info("WMS:checkApiKey " + apiKey + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");

      checkClientVersion(clientVersion);
      boolean valid = Main.getAuthorizer().isValidWorkApiKey(apiKey);
      return successResponse(new JSONObject().put(CoordConsts.SVC_KEY_API_KEY, valid));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:checkApiKey exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:checkApiKey exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  private void checkApiKeyValidity(String apiKey) throws Exception {
    if (!Main.getAuthorizer().isValidWorkApiKey(apiKey)) {
      throw new AccessControlException("Invalid API key: " + apiKey);
    }
  }

  private void checkClientVersion(String clientVersion) throws Exception {
    Version.checkCompatibleVersion("Service", "Client", clientVersion);
  }

  private void checkContainerAccessibility(String apiKey, String containerName) throws Exception {
    if (!Main.getAuthorizer().isAccessibleContainer(apiKey, containerName, true)) {
      throw new AccessControlException("container is not accessible by the api key");
    }
  }

  private void checkStringParam(String paramStr, String parameterName) {
    if (Strings.isNullOrEmpty(paramStr)) {
      throw new IllegalArgumentException(parameterName + " is missing or empty");
    }
  }

  /**
   * Configures an analysis for the container
   *
   * @param apiKey The API key of the requester
   * @param clientVersion The version of the client
   * @param containerName The name of the container to configure
   * @param newAnalysisStr The string representation of a new analysis to configure
   * @param analysisName The name of the analysis to configure
   * @param addQuestionsStream A stream providing the questions for the analysis
   * @param delQuestions A list of questions to delete from the analysis
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_CONFIGURE_ANALYSIS)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray configureAnalysis(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_NEW_ANALYSIS) String newAnalysisStr,
      @FormDataParam(CoordConsts.SVC_KEY_ANALYSIS_NAME) String analysisName,
      @FormDataParam(CoordConsts.SVC_KEY_FILE) InputStream addQuestionsStream,
      @FormDataParam(CoordConsts.SVC_KEY_DEL_ANALYSIS_QUESTIONS) String delQuestions) {
    try {
      _logger.info(
          "WMS:configureAnalysis "
              + apiKey
              + " "
              + containerName
              + " "
              + newAnalysisStr
              + " "
              + analysisName
              + " "
              + delQuestions
              + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(analysisName, "Analysis name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      Map<String, String> questionsToAdd = new HashMap<>();
      if (addQuestionsStream != null) {
        BatfishObjectMapper mapper = new BatfishObjectMapper();
        Map<String, Object> streamValue;
        try {
          streamValue =
              mapper.readValue(addQuestionsStream, new TypeReference<Map<String, Object>>() {});
          for (Entry<String, Object> entry : streamValue.entrySet()) {
            String textValue = mapper.writeValueAsString(entry.getValue());
            questionsToAdd.put(entry.getKey(), textValue);
          }
        } catch (IOException e) {
          throw new BatfishException("Failed to read question JSON from input stream", e);
        }
      }
      boolean newAnalysis = !Strings.isNullOrEmpty(newAnalysisStr);
      List<String> questionsToDelete = new ArrayList<>();
      if (!Strings.isNullOrEmpty(delQuestions)) {
        JSONArray delQuestionsArray = new JSONArray(delQuestions);
        for (int i = 0; i < delQuestionsArray.length(); i++) {
          questionsToDelete.add(delQuestionsArray.getString(i));
        }
      }

      Main.getWorkMgr()
          .configureAnalysis(
              containerName, newAnalysis, analysisName, questionsToAdd, questionsToDelete);

      return successResponse(new JSONObject().put("result", "successfully configured analysis"));

    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException
        | ZipException e) {
      _logger.error("WMS:configureAnalysis exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:configureAnalysis exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Delete an analysis from the container
   *
   * @param apiKey The API key of the requester
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the analysis resides
   * @param analysisName The name of the analysis to delete
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_DEL_ANALYSIS)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray delAnalysis(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_ANALYSIS_NAME) String analysisName) {
    try {
      _logger.info("WMS:delAnalysis " + apiKey + " " + containerName + " " + analysisName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(analysisName, "Analysis name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      Main.getWorkMgr().delAnalysis(containerName, analysisName);

      return successResponse(new JSONObject().put("result", "successfully configured analysis"));

    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException
        | ZipException e) {
      _logger.error("WMS:delAnalysis exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:delAnalysis exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Delete the specified container
   *
   * @param apiKey The API key of the requester
   * @param clientVersion The version of the client
   * @param containerName The name of the container to delete
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_DEL_CONTAINER)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray delContainer(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName) {
    try {
      _logger.info("WMS:delContainer " + containerName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      boolean status = Main.getWorkMgr().delContainer(containerName);

      return successResponse(new JSONObject().put("result", status));

    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:delContainer exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:delContainer exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Deletes the specified environment under the specified container and testrig
   *
   * @param apiKey The API key of the requester
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the environment and testrig reside
   * @param envName The name of the environment to delete
   * @param testrigName The name of the testrig in which the environment resides
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_DEL_ENVIRONMENT)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray delEnvironment(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_ENV_NAME) String envName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName) {
    try {
      _logger.info("WMS:delEnvironment " + containerName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");
      checkStringParam(envName, "Environment name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      Main.getWorkMgr().delEnvironment(containerName, testrigName, envName);

      return successResponse(new JSONObject().put("result", "true"));

    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:delEnvironment exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:delEnvironment exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Delete the specified question under the specified container, testrig
   *
   * @param apiKey The API key of the requester
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the question resides
   * @param questionName The name of the question to delete
   * @param testrigName The name of the testrig for which the question was asked
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_DEL_QUESTION)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray delQuestion(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_QUESTION_NAME) String questionName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName) {
    try {
      _logger.info("WMS:delQuestion " + containerName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");
      checkStringParam(questionName, "Question name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      Main.getWorkMgr().delTestrigQuestion(containerName, testrigName, questionName);

      return successResponse(new JSONObject().put("result", "true"));

    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:delQuestion exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:delQuestion exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Deletesthe specified testrig under the specified container
   *
   * @param apiKey The API key of the requester
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the testrig resides
   * @param testrigName The name of the testrig to delete
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_DEL_TESTRIG)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray delTestrig(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName) {
    try {
      _logger.info("WMS:delTestrig " + containerName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      Main.getWorkMgr().delTestrig(containerName, testrigName);

      return successResponse(new JSONObject().put("result", "true"));

    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:delTestrig exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:delTestrig exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Get answers for a previously run analysis
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the analysis resides
   * @param testrigName The name of the testrig on which the analysis was run
   * @param baseEnv The name of the base environment on which the analysis was run
   * @param deltaTestrig The name of the delta testrig on which the analysis was run
   * @param deltaEnv The name of the delta environment on which the analysis was run
   * @param analysisName The name of the analysis
   * @param prettyAnswer Whether or not to pretty&#8208;print the result
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_GET_ANALYSIS_ANSWERS)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getAnalysisAnswers(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName,
      @FormDataParam(CoordConsts.SVC_KEY_ENV_NAME) String baseEnv,
      @FormDataParam(CoordConsts.SVC_KEY_DELTA_TESTRIG_NAME) String deltaTestrig,
      @FormDataParam(CoordConsts.SVC_KEY_DELTA_ENV_NAME) String deltaEnv,
      @FormDataParam(CoordConsts.SVC_KEY_ANALYSIS_NAME) String analysisName,
      @FormDataParam(CoordConsts.SVC_KEY_PRETTY_ANSWER) String prettyAnswer) {
    try {
      _logger.info(
          "WMS:getAnalysisAnswers "
              + apiKey
              + " "
              + containerName
              + " "
              + testrigName
              + " "
              + analysisName
              + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Base testrig name");
      checkStringParam(baseEnv, "Base environment name");
      checkStringParam(analysisName, "Analysis name");
      checkStringParam(prettyAnswer, "Retrieve pretty-printed answers");
      boolean pretty = Boolean.parseBoolean(prettyAnswer);

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      Map<String, String> answers =
          Main.getWorkMgr()
              .getAnalysisAnswers(
                  containerName,
                  testrigName,
                  baseEnv,
                  deltaTestrig,
                  deltaEnv,
                  analysisName,
                  pretty);

      BatfishObjectMapper mapper = new BatfishObjectMapper();
      String answersStr = mapper.writeValueAsString(answers);

      return successResponse(new JSONObject().put(CoordConsts.SVC_KEY_ANSWERS, answersStr));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:getAnalysisAnswers exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:getAnalsysisAnswers exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Get answer to a previously asked question
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the question was asked
   * @param testrigName The name of the testrig on which the question was asked
   * @param baseEnv The name of the base environment on which the question was asked
   * @param deltaTestrig The name of the delta testrig on which the question was asked
   * @param deltaEnv The name of the delta environment on which the question was asked
   * @param questionName The name of the question
   * @param prettyAnswer Whether or not to pretty&#8208;print the result
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_GET_ANSWER)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getAnswer(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName,
      @FormDataParam(CoordConsts.SVC_KEY_ENV_NAME) String baseEnv,
      @FormDataParam(CoordConsts.SVC_KEY_DELTA_TESTRIG_NAME) String deltaTestrig,
      @FormDataParam(CoordConsts.SVC_KEY_DELTA_ENV_NAME) String deltaEnv,
      @FormDataParam(CoordConsts.SVC_KEY_QUESTION_NAME) String questionName,
      @FormDataParam(CoordConsts.SVC_KEY_PRETTY_ANSWER) String prettyAnswer) {
    try {
      _logger.info(
          "WMS:getAnswer "
              + apiKey
              + " "
              + containerName
              + " "
              + testrigName
              + " "
              + questionName
              + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Base testrig name");
      checkStringParam(baseEnv, "Base environment name");
      checkStringParam(questionName, "Question name");
      checkStringParam(prettyAnswer, "Retrieve pretty-printed answer");
      boolean pretty = Boolean.parseBoolean(prettyAnswer);

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      String answer =
          Main.getWorkMgr()
              .getAnswer(
                  containerName,
                  testrigName,
                  baseEnv,
                  deltaTestrig,
                  deltaEnv,
                  questionName,
                  pretty);

      return successResponse(new JSONObject().put(CoordConsts.SVC_KEY_ANSWER, answer));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:getAnswer exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:getAnswer exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Get content of the configuration file
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the question was asked
   * @param testrigName The name of the testrig in which the question was asked
   * @param configName The name of the configuration file in which the question was asked
   * @return A {@link Response Response} with an entity consists either a string of the file content
   *     of the configuration file {@code configName} or an error message if: the configuration file
   *     {@code configName} does not exist or the {@code apiKey} has no acess to the container
   *     {@code containerName}
   */
  @POST
  @Path(CoordConsts.SVC_RSC_GET_CONFIGURATION)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getConfiguration(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName,
      @FormDataParam(CoordConsts.SVC_KEY_CONFIGURATION_NAME) String configName) {
    try {
      _logger.info("WMS:getConfiguration " + containerName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);

      java.nio.file.Path containerDir =
          Main.getSettings().getContainersLocation().resolve(containerName).toAbsolutePath();
      if (containerDir == null || !Files.exists(containerDir)) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity("Container '" + containerName + "' not found")
            .type(MediaType.TEXT_PLAIN)
            .build();
      }

      checkContainerAccessibility(apiKey, containerName);

      String configContent =
          Main.getWorkMgr().getConfiguration(containerName, testrigName, configName);

      return Response.ok(configContent).build();
    } catch (AccessControlException e) {
      return Response.status(Status.FORBIDDEN)
          .entity(e.getMessage())
          .type(MediaType.TEXT_PLAIN)
          .build();
    } catch (BatfishException e) {
      return Response.status(Status.BAD_REQUEST)
          .entity(e.getMessage())
          .type(MediaType.TEXT_PLAIN)
          .build();
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:getConfiguration exception: " + stackTrace);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(e.getCause())
          .type(MediaType.TEXT_PLAIN)
          .build();
    }
  }

  /**
   * Get information of the container
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the question was asked
   * @return A {@link Response Response} with an entity consists either a json representation of the
   *     container {@code containerName} or an error message if: the container {@code containerName}
   *     does not exist or the {@code apiKey} has no acess to the container {@code containerName}
   */
  @POST
  @Path(CoordConsts.SVC_RSC_GET_CONTAINER)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getContainer(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName) {
    try {
      _logger.info("WMS:getContainer " + containerName + "\n");
      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);

      java.nio.file.Path containerDir =
          Main.getSettings().getContainersLocation().resolve(containerName).toAbsolutePath();
      if (containerDir == null || !Files.exists(containerDir)) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity("Container '" + containerName + "' not found")
            .type(MediaType.TEXT_PLAIN)
            .build();
      }

      checkContainerAccessibility(apiKey, containerName);
      Container container = Main.getWorkMgr().getContainer(containerDir);
      BatfishObjectMapper mapper = new BatfishObjectMapper();
      String containerString = mapper.writeValueAsString(container);
      return Response.ok(containerString).build();

    } catch (AccessControlException e) {
      return Response.status(Status.FORBIDDEN)
          .entity(e.getMessage())
          .type(MediaType.TEXT_PLAIN)
          .build();
    } catch (BatfishException e) {
      return Response.status(Status.BAD_REQUEST)
          .entity(e.getMessage())
          .type(MediaType.TEXT_PLAIN)
          .build();
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:getContainer exception: " + stackTrace);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(e.getCause())
          .type(MediaType.TEXT_PLAIN)
          .build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getInfo() {
    _logger.info("WMS:getInfo\n");
    try {
      JSONObject map = new JSONObject();
      map.put("Service name", "Batfish coordinator");
      map.put(CoordConsts.SVC_KEY_VERSION, Version.getVersion());
      map.put("APIs", "Enter ../application.wadl (relative to your URL) to see supported methods");

      return successResponse(map);
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:getInfo exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Fetches the specified object from the specified container, testrig
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The container in which the object resides
   * @param testrigName The testrig in which the object resides
   * @param objectName The name of the object
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_GET_OBJECT)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response getObject(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName,
      @FormDataParam(CoordConsts.SVC_KEY_OBJECT_NAME) String objectName) {
    try {
      _logger.info("WMS:getObject " + testrigName + " --> " + objectName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");
      checkStringParam(objectName, "Object name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      java.nio.file.Path file =
          Main.getWorkMgr().getTestrigObject(containerName, testrigName, objectName);

      if (file == null || !Files.exists(file)) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity("File not found")
            .type(MediaType.TEXT_PLAIN)
            .build();
      }

      String filename = file.getFileName().toString();
      return Response.ok(file.toFile(), MediaType.APPLICATION_OCTET_STREAM)
          .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
          .header(CoordConsts.SVC_FILENAME_HDR, filename)
          .build();
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(e.getMessage())
          .type(MediaType.TEXT_PLAIN)
          .build();
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:getObject exception: " + stackTrace);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(e.getCause())
          .type(MediaType.TEXT_PLAIN)
          .build();
    }
  }

  @POST
  @Path(CoordConsts.SVC_RSC_GET_QUESTION_TEMPLATES)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getQuestionTemplates(@FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey) {
    try {
      _logger.info("WMS:getQuestionTemplates " + apiKey + "\n");

      checkStringParam(apiKey, "API key");

      checkApiKeyValidity(apiKey);

      Map<String, String> questionTemplates = Main.getQuestionTemplates();

      if (questionTemplates == null) {
        return failureResponse("Question templates dir is not configured");
      } else {
        return successResponse(
            new JSONObject().put(CoordConsts.SVC_KEY_QUESTION_LIST, questionTemplates));
      }
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:getQuestionTemplates exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  @GET
  @Path(CoordConsts.SVC_RSC_GETSTATUS)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getStatus() {
    try {
      _logger.info("WMS:getStatus\n");
      JSONObject retObject = Main.getWorkMgr().getStatusJson();
      retObject.put("service-version", Version.getVersion());
      return successResponse(retObject);
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:getStatus exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Obtain the counts of completed and incomplete work items
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param workId The work ID to check
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_GET_WORKSTATUS)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getWorkStatus(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_WORKID) String workId) {
    try {
      _logger.info("WMS:getWorkStatus " + workId + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(workId, "work id");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);

      QueuedWork work = Main.getWorkMgr().getWork(UUID.fromString(workId));

      if (work == null) {
        return failureResponse("work with the specified id does not exist or is not inaccessible");
      }

      checkContainerAccessibility(apiKey, work.getWorkItem().getContainerName());

      BatfishObjectMapper mapper = new BatfishObjectMapper();
      String taskStr = mapper.writeValueAsString(work.getLastTaskCheckResult());

      return successResponse(
          new JSONObject()
              .put(CoordConsts.SVC_KEY_WORKSTATUS, work.getStatus().toString())
              .put(CoordConsts.SVC_KEY_TASKSTATUS, taskStr));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:getWorkStatus exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:getWorkStatus exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Initialize a new container
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container to initialize (overrides containerPrefix)
   * @param containerPrefix The prefix used to generate the container name (ignored if containerName
   *     is not empty)
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_INIT_CONTAINER)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray initContainer(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_PREFIX) String containerPrefix) {
    try {
      _logger.info("WMS:initContainer " + containerPrefix + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      if (containerName == null || containerName.equals("")) {
        checkStringParam(containerPrefix, "Container prefix");
      }

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);

      String outputContainerName = Main.getWorkMgr().initContainer(containerName, containerPrefix);

      Main.getAuthorizer().authorizeContainer(apiKey, outputContainerName);

      return successResponse(
          new JSONObject().put(CoordConsts.SVC_KEY_CONTAINER_NAME, outputContainerName));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:initContainer exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:initContainer exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * List the analyses under the specified container
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container whose analyses are to be listed
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_LIST_ANALYSES)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray listAnalyses(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName) {
    try {
      _logger.info("WMS:listAnalyses " + apiKey + " " + containerName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      JSONObject retObject = new JSONObject();

      for (String analysisName : Main.getWorkMgr().listAnalyses(containerName)) {

        JSONObject analysisJson = new JSONObject();

        for (String questionName :
            Main.getWorkMgr().listAnalysisQuestions(containerName, analysisName)) {
          String questionText =
              Main.getWorkMgr().getAnalysisQuestion(containerName, analysisName, questionName);

          analysisJson.put(questionName, new JSONObject(questionText));
        }

        retObject.put(analysisName, analysisJson);
      }

      return successResponse(new JSONObject().put(CoordConsts.SVC_KEY_ANALYSIS_LIST, retObject));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:listAnalyses exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:listAnalyses exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * List the containers that the specified API key can access
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_LIST_CONTAINERS)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray listContainers(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion) {
    try {
      _logger.info("WMS:listContainers " + apiKey + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);

      if (!_settings.getDefaultKeyListings() && apiKey.equals(CoordConsts.DEFAULT_API_KEY)) {
        throw new AccessControlException("Listing containers is not allowed with Default API key");
      }

      SortedSet<String> containerList = Main.getWorkMgr().listContainers(apiKey);

      return successResponse(
          new JSONObject().put(CoordConsts.SVC_KEY_CONTAINER_LIST, new JSONArray(containerList)));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:listContainers exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:listContainers exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Lists the environments under the specified container, testrig
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The container in which the testrig and environments reside
   * @param testrigName The name of the testrig whose environments are to be listed
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_LIST_ENVIRONMENTS)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray listEnvironments(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName) {
    try {
      _logger.info("WMS:listEnvironments " + apiKey + " " + containerName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      SortedSet<String> environmentList =
          Main.getWorkMgr().listEnvironments(containerName, testrigName);

      return successResponse(
          new JSONObject()
              .put(CoordConsts.SVC_KEY_ENVIRONMENT_LIST, new JSONArray(environmentList)));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:listEnvironments exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:listEnvironments exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * List the questions under the specified container, testrig
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the testrig and questions reside
   * @param testrigName The name of the testrig, the questions on which are to be listed
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_LIST_QUESTIONS)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray listTestrigQuestions(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName) {
    try {
      _logger.info("WMS:listTestrigQuestions " + apiKey + " " + containerName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      JSONObject retObject = new JSONObject();

      for (String questionName : Main.getWorkMgr().listQuestions(containerName, testrigName)) {
        String questionText =
            Main.getWorkMgr().getTestrigQuestion(containerName, testrigName, questionName);

        retObject.put(questionName, new JSONObject(questionText));
      }

      return successResponse(new JSONObject().put(CoordConsts.SVC_KEY_QUESTION_LIST, retObject));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:listTestrigQuestions exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:listTestrigQuestions exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * List the testrigs under the specified container
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container whose testrigs are to be listed
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_LIST_TESTRIGS)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray listTestrigs(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName) {
    try {
      _logger.info("WMS:listTestrigs " + apiKey + " " + containerName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      JSONArray retArray = new JSONArray();

      SortedSet<String> testrigList = Main.getWorkMgr().listTestrigs(containerName);

      for (String testrig : testrigList) {
        String testrigInfo = Main.getWorkMgr().getTestrigInfo(containerName, testrig);

        JSONObject jObject =
            new JSONObject()
                .put(CoordConsts.SVC_KEY_TESTRIG_NAME, testrig)
                .put(CoordConsts.SVC_KEY_TESTRIG_INFO, testrigInfo);

        retArray.put(jObject);
      }

      return successResponse(new JSONObject().put(CoordConsts.SVC_KEY_TESTRIG_LIST, retArray));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:listTestrigs exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:listTestrigs exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Upload a custom object under the specified container, testrig.
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container in which the testrig resides
   * @param testrigName The name of the testrig under which to upload the object
   * @param objectName The name of the object to upload
   * @param fileStream The stream from which the object is read
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_PUT_OBJECT)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray putObject(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName,
      @FormDataParam(CoordConsts.SVC_KEY_OBJECT_NAME) String objectName,
      @FormDataParam(CoordConsts.SVC_KEY_FILE) InputStream fileStream) {
    try {
      _logger.infof(
          "WMS:putObject %s %s %s / %s\n", apiKey, containerName, testrigName, objectName);

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");
      checkStringParam(objectName, "Object name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      Main.getWorkMgr().putObject(containerName, testrigName, objectName, fileStream);

      return successResponse(new JSONObject().put("result", "successfully uploaded custom object"));

    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:putObject exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:putObject exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Queue a new work item
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param workItemStr The work item to queue
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_QUEUE_WORK)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray queueWork(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_WORKITEM) String workItemStr) {
    try {
      _logger.info("WMS:queueWork " + apiKey + " " + workItemStr + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(workItemStr, "Workitem string");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);

      WorkItem workItem = WorkItem.fromJsonString(workItemStr);

      checkContainerAccessibility(apiKey, workItem.getContainerName());

      boolean result = Main.getWorkMgr().queueWork(workItem);

      return successResponse(new JSONObject().put("result", result));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:queueWork exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:queueWork exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Sync testrigs
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The container to sync testrigs for
   * @param pluginId The plugin id to use for syncing
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_SYNC_TESTRIGS_SYNC_NOW)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray syncTestrigsSyncNow(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_PLUGIN_ID) String pluginId,
      @FormDataParam(CoordConsts.SVC_KEY_FORCE) String forceStr) {
    try {
      _logger.info(
          "WMS:syncTestrigsSyncNow " + apiKey + " " + containerName + " " + pluginId + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(pluginId, "Plugin Id");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      boolean force = !Strings.isNullOrEmpty(forceStr) && Boolean.parseBoolean(forceStr);

      int numCommits = Main.getWorkMgr().syncTestrigsSyncNow(containerName, pluginId, force);

      return successResponse(new JSONObject().put("numCommits", numCommits));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:syncTestrigsSyncNow exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:syncTestrigsSyncNow exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Update settings for syncing testrigs
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The container to sync testrigs for
   * @param pluginId The plugin id to use for syncing
   * @param settingsStr The stringified version of settings
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_SYNC_TESTRIGS_UPDATE_SETTINGS)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray syncTestrigsUpdateSettings(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_PLUGIN_ID) String pluginId,
      @FormDataParam(CoordConsts.SVC_KEY_SETTINGS) String settingsStr) {
    try {
      _logger.info(
          "WMS:syncTestrigsUpdateSettings "
              + apiKey
              + " "
              + containerName
              + " "
              + pluginId
              + " "
              + settingsStr
              + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(pluginId, "Plugin Id");
      checkStringParam(settingsStr, "Settings");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      BatfishObjectMapper mapper = new BatfishObjectMapper();
      Map<String, String> settings =
          mapper.readValue(settingsStr, new TypeReference<Map<String, String>>() {});

      boolean result =
          Main.getWorkMgr().syncTestrigsUpdateSettings(containerName, pluginId, settings);

      return successResponse(new JSONObject().put("result", result));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:syncTestrigsUpdateSettings exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:syncTestrigsUpdateSettings exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  @GET
  @Path("test")
  @Produces(MediaType.TEXT_PLAIN)
  public String test() {
    try {
      _logger.info("WMS:test\n");
      JSONArray id = successResponse(Main.getWorkMgr().getStatusJson());

      return id.toString();

      // return Response.ok()
      // .entity(id)
      // // .header("Access-Control-Allow-Origin","*")
      // .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
      // .allow("OPTIONS")
      // .build();
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:test exception: " + stackTrace);
      // return Response.serverError().build();
      return "got error";
    }
  }

  /**
   * Uploads a new environment under the container, testrig
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container under which the testrig resides
   * @param testrigName The name of the testrig under which to upload the new environment
   * @param baseEnvName The base environment name from which the new environment initially inherits
   * @param envName The name of the new environment to create
   * @param fileStream The stream from which the contents of the new environment are read. These
   *     contents overwrite those inherited from any base environment.
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_UPLOAD_ENV)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray uploadEnvironment(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName,
      @FormDataParam(CoordConsts.SVC_KEY_BASE_ENV_NAME) String baseEnvName,
      @FormDataParam(CoordConsts.SVC_KEY_ENV_NAME) String envName,
      @FormDataParam(CoordConsts.SVC_KEY_ZIPFILE) InputStream fileStream) {
    try {
      _logger.info(
          "WMS:uploadEnvironment "
              + apiKey
              + " "
              + containerName
              + " "
              + testrigName
              + " / "
              + envName
              + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");
      checkStringParam(envName, "Environment name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      Main.getWorkMgr()
          .uploadEnvironment(containerName, testrigName, baseEnvName, envName, fileStream);

      return successResponse(new JSONObject().put("result", "successfully uploaded environment"));

    } catch (BatfishException e) {
      _logger.error("WMS:uploadEnvironment exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:uploadEnvironment exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Upload a new question under the specified container, testrig. A file containing the question
   * and a file containing the parameters must be provided.
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container under which the testrig resides
   * @param testrigName The name of the testrig about which to ask the question
   * @param qName The name of the question
   * @param fileStream The stream from which the question is read
   * @param paramFileStream The stream from which the parameters are read
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_UPLOAD_QUESTION)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray uploadQuestion(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName,
      @FormDataParam(CoordConsts.SVC_KEY_QUESTION_NAME) String qName,
      @FormDataParam(CoordConsts.SVC_KEY_FILE) InputStream fileStream,
      @FormDataParam(CoordConsts.SVC_KEY_FILE2) InputStream paramFileStream) {
    try {
      _logger.info(
          "WMS:uploadQuestion "
              + apiKey
              + " "
              + containerName
              + " "
              + testrigName
              + " / "
              + qName
              + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");
      checkStringParam(qName, "Question name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      Main.getWorkMgr()
          .uploadQuestion(containerName, testrigName, qName, fileStream, paramFileStream);

      return successResponse(new JSONObject().put("result", "successfully uploaded question"));

    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException e) {
      _logger.error("WMS:uploadQuestion exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:uploadQuestion exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }

  /**
   * Uploads a new testrig under the specified container
   *
   * @param apiKey The API key of the client
   * @param clientVersion The version of the client
   * @param containerName The name of the container under which to upload the new testrig
   * @param testrigName The name of the new testrig to create
   * @param fileStream The stream from which the new testrig is read
   * @return TODO: document JSON response
   */
  @POST
  @Path(CoordConsts.SVC_RSC_UPLOAD_TESTRIG)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray uploadTestrig(
      @FormDataParam(CoordConsts.SVC_KEY_API_KEY) String apiKey,
      @FormDataParam(CoordConsts.SVC_KEY_VERSION) String clientVersion,
      @FormDataParam(CoordConsts.SVC_KEY_CONTAINER_NAME) String containerName,
      @FormDataParam(CoordConsts.SVC_KEY_TESTRIG_NAME) String testrigName,
      @FormDataParam(CoordConsts.SVC_KEY_ZIPFILE) InputStream fileStream,
      @FormDataParam(CoordConsts.SVC_KEY_AUTO_ANALYZE_TESTRIG) String autoAnalyzeStr) {
    try {
      _logger.info("WMS:uploadTestrig " + apiKey + " " + containerName + " " + testrigName + "\n");

      checkStringParam(apiKey, "API key");
      checkStringParam(clientVersion, "Client version");
      checkStringParam(containerName, "Container name");
      checkStringParam(testrigName, "Testrig name");

      checkApiKeyValidity(apiKey);
      checkClientVersion(clientVersion);
      checkContainerAccessibility(apiKey, containerName);

      boolean autoAnalyze = false;
      if (!Strings.isNullOrEmpty(autoAnalyzeStr)) {
        autoAnalyze = Boolean.parseBoolean(autoAnalyzeStr);
      }

      Main.getWorkMgr().uploadTestrig(containerName, testrigName, fileStream, autoAnalyze);

      return successResponse(new JSONObject().put("result", "successfully uploaded testrig"));
    } catch (FileExistsException
        | FileNotFoundException
        | IllegalArgumentException
        | AccessControlException
        | ZipException e) {
      _logger.error("WMS:uploadTestrig exception: " + e.getMessage() + "\n");
      return failureResponse(e.getMessage());
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      _logger.error("WMS:uploadTestrig exception: " + stackTrace);
      return failureResponse(e.getMessage());
    }
  }
}
