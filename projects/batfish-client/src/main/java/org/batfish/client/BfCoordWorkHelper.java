package org.batfish.client;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.batfish.client.config.Settings;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.CoordConsts;
import org.batfish.common.CoordConsts.WorkStatusCode;
import org.batfish.common.CoordConstsV2;
import org.batfish.common.WorkItem;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.version.BatfishVersion;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

public class BfCoordWorkHelper {

  private final String _coordWorkMgr;
  private final String _coordWorkMgrV2;
  private final BatfishLogger _logger;
  private final Settings _settings;
  private final Client _client;

  public BfCoordWorkHelper(BatfishLogger logger, Settings settings) {
    _logger = logger;
    _settings = settings;
    _coordWorkMgr = _settings.getCoordinatorHost() + ":" + _settings.getCoordinatorWorkPort();
    _coordWorkMgrV2 = _settings.getCoordinatorHost() + ":" + _settings.getCoordinatorWorkV2Port();
    try {
      _client = getClientBuilder().build();
    } catch (Exception e) {
      _logger.errorf("exception: ");
      _logger.error(Throwables.getStackTraceAsString(e) + "\n");
      throw new BatfishException("Failed to create HTTP client", e);
    }
  }

  private static void addFileMultiPart(MultiPart multiPart, String key, String filename) {
    multiPart.bodyPart(
        new FormDataBodyPart(key, new File(filename), MediaType.APPLICATION_OCTET_STREAM_TYPE));
  }

  private static void addTextMultiPart(MultiPart multiPart, String key, String value) {
    multiPart.bodyPart(new FormDataBodyPart(key, value, MediaType.TEXT_PLAIN_TYPE));
  }

  public boolean delNetwork(String networkName) {
    WebTarget webTarget = getTargetV2(Lists.newArrayList(CoordConstsV2.RSC_NETWORKS, networkName));

    try (Response response =
        webTarget
            .request(MediaType.APPLICATION_JSON)
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, _settings.getApiKey())
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION, BatfishVersion.getVersionStatic())
            .delete()) {

      if (response.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
        _logger.errorf("delNetwork: Did not get OK response. Got: %s\n", response.getStatus());
        _logger.error(response.readEntity(String.class) + "\n");
        return false;
      }
      return true;
    } catch (Exception e) {
      _logger.errorf("Exception in delNetwork from %s for %s\n", _coordWorkMgrV2, networkName);
      _logger.error(Throwables.getStackTraceAsString(e) + "\n");
      return false;
    }
  }

  private ClientBuilder getClientBuilder() {
    return CommonUtil.createHttpClientBuilder(
            _settings.getSslDisable(),
            _settings.getSslTrustAllCerts(),
            _settings.getSslKeystoreFile(),
            _settings.getSslKeystorePassword(),
            _settings.getSslTruststoreFile(),
            _settings.getSslTruststorePassword(),
            true)
        .register(MultiPartFeature.class);
  }

  /**
   * Returns the JSON log of a work item completed by the batfish worker corresponding to a given
   * work ID.
   */
  @Nullable
  public String getWorkJson(String networkName, String snapshotName, UUID workId) {
    WebTarget webTarget =
        getTargetV2(
            Arrays.asList(
                CoordConstsV2.RSC_NETWORKS,
                networkName,
                CoordConstsV2.RSC_SNAPSHOTS,
                snapshotName,
                CoordConstsV2.RSC_WORK_JSON,
                workId.toString()));
    try (Response response =
        webTarget
            .request(MediaType.TEXT_PLAIN)
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, _settings.getApiKey())
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION, BatfishVersion.getVersionStatic())
            .get()) {
      _logger.debug(response.getStatus() + " " + response.getStatusInfo() + " " + response + "\n");
      if (response.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
        _logger.debugf(
            "getWorkJson: Did not get an OK response for %s -> %s->%s\n",
            networkName, snapshotName, workId);
        return null;
      }
      return response.readEntity(String.class);
    } catch (Exception e) {
      _logger.errorf(
          "Exception in getWorkJson from %s using (%s, %s)\n", _coordWorkMgr, snapshotName, workId);
      _logger.error(Throwables.getStackTraceAsString(e) + "\n");
      return null;
    }
  }

  /** Returns the JSON-encoded POJO topology for a snapshot. */
  @Nullable
  public String getPojoTopology(String networkName, String snapshotName) {
    WebTarget webTarget =
        getTargetV2(
            Arrays.asList(
                CoordConstsV2.RSC_NETWORKS,
                networkName,
                CoordConstsV2.RSC_SNAPSHOTS,
                snapshotName,
                CoordConstsV2.RSC_POJO_TOPOLOGY));
    try (Response response =
        webTarget
            .request(MediaType.APPLICATION_JSON)
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, _settings.getApiKey())
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION, BatfishVersion.getVersionStatic())
            .get()) {
      _logger.debug(response.getStatus() + " " + response.getStatusInfo() + " " + response + "\n");
      if (response.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
        _logger.debugf(
            "getPojoTopology: Did not get an OK response for %s -> %s\n",
            networkName, snapshotName);
        return null;
      }
      return response.readEntity(String.class);
    } catch (Exception e) {
      _logger.errorf(
          "Exception in getPojoTopology from %s using %s\n", _coordWorkMgr, snapshotName);
      _logger.error(Throwables.getStackTraceAsString(e) + "\n");
      return null;
    }
  }

  private WebTarget getTarget(String resource) {
    String protocol = (_settings.getSslDisable()) ? "http" : "https";
    String urlString =
        String.format(
            "%s://%s%s/%s", protocol, _coordWorkMgr, CoordConsts.SVC_CFG_WORK_MGR, resource);
    return _client.target(urlString);
  }

  /**
   * Returns a {@link WebTarget webTarget} for BatFish service V2 by resolving each segment in
   * {@code resources} to base url of service V2.
   */
  public WebTarget getTargetV2(List<String> resources) {
    String protocol = (_settings.getSslDisable()) ? "http" : "https";
    String urlString =
        String.format("%s://%s%s", protocol, _coordWorkMgrV2, CoordConsts.SVC_CFG_WORK_MGR2);
    WebTarget target = _client.target(urlString);
    for (String resource : resources) {
      target = target.path(resource);
    }
    return target;
  }

  public static class WorkResult {
    @Nonnull private final WorkStatusCode _status;
    @Nonnull private final String _taskStr;

    WorkResult(@Nonnull WorkStatusCode status, @Nonnull String taskStr) {
      _status = status;
      _taskStr = taskStr;
    }

    @Nonnull
    public WorkStatusCode getStatus() {
      return _status;
    }

    @Nonnull
    public String getTaskStr() {
      return _taskStr;
    }
  }

  @Nullable
  WorkResult getWorkStatus(UUID workId) {
    try {
      WebTarget webTarget = getTarget(CoordConsts.SVC_RSC_GET_WORKSTATUS);

      @SuppressWarnings("PMD.CloseResource") // postData will close it
      MultiPart multiPart = new MultiPart();
      multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_API_KEY, _settings.getApiKey());
      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_WORKID, workId.toString());

      JSONObject jObj = postData(webTarget, multiPart);
      if (jObj == null) {
        return null;
      }

      if (!jObj.has(CoordConsts.SVC_KEY_WORKSTATUS)) {
        _logger.errorf("workstatus key not found in: %s\n", jObj);
        return null;
      }

      WorkStatusCode workStatus =
          WorkStatusCode.valueOf(jObj.getString(CoordConsts.SVC_KEY_WORKSTATUS));

      if (!jObj.has(CoordConsts.SVC_KEY_TASKSTATUS)) {
        _logger.errorf("taskstatus key not found in: %s\n", jObj);
      }
      String taskStr = jObj.getString(CoordConsts.SVC_KEY_TASKSTATUS);

      return new WorkResult(workStatus, taskStr);
    } catch (Exception e) {
      _logger.errorf("exception: ");
      _logger.error(Throwables.getStackTraceAsString(e) + "\n");
      return null;
    }
  }

  @Nullable
  public String initNetwork(@Nullable String networkName, @Nullable String networkPrefix) {
    try {
      WebTarget webTarget = getTarget(CoordConsts.SVC_RSC_INIT_NETWORK);

      @SuppressWarnings("PMD.CloseResource") // postData will close it
      MultiPart multiPart = new MultiPart();
      multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_API_KEY, _settings.getApiKey());
      if (networkName != null) {
        addTextMultiPart(multiPart, CoordConsts.SVC_KEY_NETWORK_NAME, networkName);
      } else {
        addTextMultiPart(multiPart, CoordConsts.SVC_KEY_NETWORK_PREFIX, networkPrefix);
      }

      JSONObject jObj = postData(webTarget, multiPart);
      if (jObj == null) {
        return null;
      }

      if (!jObj.has(CoordConsts.SVC_KEY_NETWORK_NAME)) {
        _logger.errorf("network name key not found in: %s\n", jObj);
        return null;
      }

      return jObj.getString(CoordConsts.SVC_KEY_NETWORK_NAME);
    } catch (Exception e) {
      _logger.errorf("exception: ");
      _logger.error(Throwables.getStackTraceAsString(e) + "\n");
      return null;
    }
  }

  public boolean isReachable(boolean chatty) {

    WebTarget webTarget = getTarget("");

    try (Response response = webTarget.request().get()) {
      _logger.info(response.getStatus() + " " + response.getStatusInfo() + " " + response + "\n");

      if (response.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
        _logger.errorf("GetObject: Did not get an OK response\n");
        return false;
      }

      return true;
    } catch (ProcessingException e) {
      if (e.getMessage().contains("ConnectException")) {
        if (chatty) {
          _logger.errorf("BF-Client: unable to connect to coordinator at %s\n", webTarget.getUri());
        }
        return false;
      }
      if (e.getMessage().contains("SSLHandshakeException")) {
        if (chatty) {
          _logger.errorf(
              "SSL handshake exception while connecting to coordinator (Is the coordinator using "
                  + "SSL and using keys that you trust?): %s\n",
              e.getMessage());
        }
        return false;
      }
      if (e.getMessage().contains("SocketException: Unexpected end of file")) {
        if (chatty) {
          _logger.errorf("SocketException while connecting to coordinator. (Are you using SSL?)\n");
        }
        return false;
      }
      throw e;
    }
  }

  @Nullable
  private JSONObject postData(WebTarget webTarget, MultiPart multiPart) throws Exception {
    try {

      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_VERSION, BatfishVersion.getVersionStatic());

      JSONArray array;
      try (Response response =
          webTarget
              .request(MediaType.APPLICATION_JSON)
              .post(Entity.entity(multiPart, multiPart.getMediaType()))) {
        _logger.debugf(
            response.getStatus() + " " + response.getStatusInfo() + " " + response + "\n");
        if (response.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
          System.err.print("PostData: Did not get an OK response\n");
          return null;
        }
        String sobj = response.readEntity(String.class);
        array = new JSONArray(sobj);
      }
      _logger.debugf("response: %s [%s] [%s]\n", array, array.get(0), array.get(1));

      if (!array.get(0).equals(CoordConsts.SVC_KEY_SUCCESS)) {
        _logger.errorf("Error in PostData: %s %s\n", array.get(0), array.get(1));
        return null;
      }

      return new JSONObject(array.get(1).toString());
    } catch (ProcessingException e) {
      if (e.getMessage().contains("ConnectException")) {
        _logger.errorf("unable to connect to coordinator at %s\n", _coordWorkMgr);
        return null;
      }
      if (e.getMessage().contains("SSLHandshakeException")) {
        _logger.errorf(
            "SSL handshake exception while connecting to coordinator (Is the coordinator using "
                + "SSL and using keys that you trust?)\n");
        return null;
      }
      if (e.getMessage().contains("SocketException: Unexpected end of file")) {
        _logger.errorf("SocketException while connecting to coordinator. (Are you using SSL?)\n");
        return null;
      }
      throw e;
    }
  }

  public boolean queueWork(WorkItem wItem) {

    try {
      WebTarget webTarget = getTarget(CoordConsts.SVC_RSC_QUEUE_WORK);

      @SuppressWarnings("PMD.CloseResource") // postData will close it
      MultiPart multiPart = new MultiPart();
      multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

      addTextMultiPart(
          multiPart, CoordConsts.SVC_KEY_WORKITEM, BatfishObjectMapper.writeString(wItem));
      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_API_KEY, _settings.getApiKey());

      JSONObject jObj = postData(webTarget, multiPart);
      return jObj != null;
    } catch (Exception e) {
      _logger.errorf("exception: ");
      _logger.error(Throwables.getStackTraceAsString(e) + "\n");
      return false;
    }
  }

  public boolean uploadQuestion(
      String networkName, String snapshotName, String qName, String qFileName) {
    try {
      WebTarget webTarget = getTarget(CoordConsts.SVC_RSC_UPLOAD_QUESTION);

      @SuppressWarnings("PMD.CloseResource") // postData will close it
      MultiPart multiPart = new MultiPart();
      multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_API_KEY, _settings.getApiKey());
      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_NETWORK_NAME, networkName);
      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_SNAPSHOT_NAME, snapshotName);
      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_QUESTION_NAME, qName);
      addFileMultiPart(multiPart, CoordConsts.SVC_KEY_FILE, qFileName);

      return postData(webTarget, multiPart) != null;
    } catch (Exception e) {
      if (e.getMessage().contains("FileNotFoundException")) {
        _logger.errorf("File not found: %s (question file)\n", qFileName);
      } else {
        _logger.errorf(
            "Exception when uploading question to %s using (%s, %s, %s): %s\n",
            _coordWorkMgr, snapshotName, qName, qFileName, Throwables.getStackTraceAsString(e));
      }
      return false;
    }
  }

  public boolean uploadSnapshot(
      String networkName, String snapshotName, String zipfileName, boolean autoAnalyze) {
    try {
      WebTarget webTarget = getTarget(CoordConsts.SVC_RSC_UPLOAD_SNAPSHOT);

      @SuppressWarnings("PMD.CloseResource") // postData will close it
      MultiPart multiPart = new MultiPart();
      multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_API_KEY, _settings.getApiKey());
      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_NETWORK_NAME, networkName);
      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_SNAPSHOT_NAME, snapshotName);
      addFileMultiPart(multiPart, CoordConsts.SVC_KEY_ZIPFILE, zipfileName);
      addTextMultiPart(multiPart, CoordConsts.SVC_KEY_AUTO_ANALYZE, String.valueOf(autoAnalyze));

      return postData(webTarget, multiPart) != null;
    } catch (Exception e) {
      if (e.getMessage().contains("FileNotFoundException")) {
        _logger.errorf("File not found: %s\n", zipfileName);
      } else {
        _logger.errorf(
            "Exception when uploading snapshot to %s using (%s, %s, %s): %s\n",
            _coordWorkMgr,
            networkName,
            snapshotName,
            zipfileName,
            Throwables.getStackTraceAsString(e));
      }
      return false;
    }
  }

  public boolean debugV2(
      FileWriter outWriter, String method, String urlTail, Object entity, MediaType mediaType)
      throws IOException {
    WebTarget webTarget = getTargetV2(Arrays.asList(urlTail.split("/", -1)));
    Invocation.Builder builder =
        webTarget
            .request(MediaType.APPLICATION_JSON)
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, _settings.getApiKey())
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION, BatfishVersion.getVersionStatic());
    try (Response response =
        entity != null
            ? builder.method(method, Entity.entity(entity, mediaType))
            : builder.method(method)) {
      if (response.hasEntity()) {
        String output = response.readEntity(String.class);
        if (outWriter != null) {
          outWriter.write(output);
        } else {
          _logger.outputf("%s\n", output);
        }
      }
      return response.getStatus() == Status.OK.getStatusCode();
    }
  }
}
