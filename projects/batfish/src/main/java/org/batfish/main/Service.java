package org.batfish.main;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.BfConsts.TaskStatus;
import org.batfish.common.Task;
import org.batfish.common.util.BatfishObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

@Path(BfConsts.SVC_BASE_RSC)
public class Service {

  BatfishLogger _logger = Driver.getMainLogger();

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getInfo() {
    return new JSONArray(
        Arrays.asList(
            BfConsts.SVC_SUCCESS_KEY,
            "Batfish service: enter ../application.wadl (relative to your URL) to see supported "
                + "methods"));
  }

  @GET
  @Path(BfConsts.SVC_GET_STATUS_RSC)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getStatus() {
    try {
      return new JSONArray(
          Arrays.asList(
              BfConsts.SVC_SUCCESS_KEY,
              (new JSONObject().put("idle", Driver.getIdle())).toString()));
    } catch (Exception e) {
      return new JSONArray(Arrays.asList(BfConsts.SVC_FAILURE_KEY, e.getMessage()));
    }
  }

  @GET
  @Path(BfConsts.SVC_GET_TASKSTATUS_RSC)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getTaskStatus(@QueryParam(BfConsts.SVC_TASKID_KEY) String taskId) {
    _logger.infof("BFS:getTaskStatus %s\n", taskId);
    try {

      if (taskId == null || taskId.equals("")) {
        return new JSONArray(Arrays.asList(BfConsts.SVC_FAILURE_KEY, "taskid not supplied"));
      }

      Task task = Driver.getTaskFromLog(taskId);
      if (task == null) {
        task = new Task(TaskStatus.Unknown);
      }
      String taskStr = BatfishObjectMapper.writePrettyString(task);
      return new JSONArray(Arrays.asList(BfConsts.SVC_SUCCESS_KEY, taskStr));
    } catch (Exception e) {
      return new JSONArray(Arrays.asList(BfConsts.SVC_FAILURE_KEY, e.getMessage()));
    }
  }

  @GET
  @Path(BfConsts.SVC_KILL_TASK_RSC)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray killTask(@QueryParam(BfConsts.SVC_TASKID_KEY) String taskId) {
    _logger.infof("BFS:killTask %s\n", taskId);
    try {
      if (Strings.isNullOrEmpty(taskId)) {
        return new JSONArray(Arrays.asList(BfConsts.SVC_FAILURE_KEY, "taskid not supplied"));
      }
      Task task = Driver.killTask(taskId);
      String taskStr = BatfishObjectMapper.writePrettyString(task);
      return new JSONArray(Arrays.asList(BfConsts.SVC_SUCCESS_KEY, taskStr));
    } catch (Exception e) {
      return new JSONArray(Arrays.asList(BfConsts.SVC_FAILURE_KEY, e.getMessage()));
    }
  }

  @GET
  @Path(BfConsts.SVC_RUN_TASK_RSC)
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray runTask(
      @QueryParam(BfConsts.SVC_TASKID_KEY) String taskId,
      @QueryParam(BfConsts.SVC_TASK_KEY) String task) {
    _logger.infof("BFS:runTask(%s,%s)\n", taskId, task);
    try {

      if (taskId == null || taskId.equals("")) {
        return new JSONArray(Arrays.asList(BfConsts.SVC_FAILURE_KEY, "taskid not supplied"));
      }

      if (task == null || task.equals("")) {
        return new JSONArray(Arrays.asList(BfConsts.SVC_FAILURE_KEY, "task not supplied"));
      }

      List<String> argsList = new ArrayList<>();

      JSONObject taskObj = new JSONObject(task);
      Iterator<?> keys = taskObj.keys();

      while (keys.hasNext()) {

        String key = (String) keys.next();
        String value = taskObj.getString(key);

        argsList.add("-" + key);

        if (value != null && !value.equals("")) {
          argsList.add(value);
        }
      }

      String[] args = argsList.toArray(new String[argsList.size()]);

      _logger.infof("Will run with args: %s\n", Arrays.toString(args));

      return new JSONArray(Driver.runBatfishThroughService(taskId, args));
    } catch (Exception e) {
      return new JSONArray(Arrays.asList(BfConsts.SVC_FAILURE_KEY, e.getMessage()));
    }
  }
}

// package org.batfish.main;
//
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
//
// import javax.ws.rs.core.Context;
// import javax.ws.rs.core.MediaType;
// import javax.ws.rs.core.MultivaluedMap;
// import javax.ws.rs.core.UriInfo;
// import javax.ws.rs.GET;
// import javax.ws.rs.Path;
// import javax.ws.rs.Produces;
// import javax.ws.rs.QueryParam;
//
// import org.codehaus.jettison.json.JSONArray;
// import org.codehaus.jettison.json.JSONObject;
// import org.batfish.common.BatfishConstants;
// import org.batfish.common.BatfishConstants.TaskkStatus;
//
// @Path(BatfishConstants.SERVICE_BASE_RESOURCE)
// public class Service {
//
// @GET
// @Produces(MediaType.APPLICATION_JSON)
// public JSONArray getInfo() {
// return new JSONArray(
// Arrays.asList(
// "",
// "Batfish service: enter ../application.wadl (relative to your URL) to see
// supported methods"));
// }
//
// @GET
// @Path(BatfishConstants.SERVICE_GETSTATUS_RESOURCE)
// @Produces(MediaType.APPLICATION_JSON)
// public JSONArray getStatus() {
// try {
// return new JSONArray(Arrays.asList("",
// (new JSONObject().put("idle", Driver.getIdle())).toString()));
// }
// catch (Exception e) {
// return new JSONArray(Arrays.asList("failure", e.getMessage()));
// }
// }
//
// @GET
// @Path(BatfishConstants.SERVICE_GETTASKSTATUS_RESOURCE)
// @Produces(MediaType.APPLICATION_JSON)
// public JSONArray getTaskStatus(
// @QueryParam(BatfishConstants.SERVICE_TASKID_KEY) String taskId) {
// try {
//
// if (taskId == null || taskId.equals("")) {
// return new JSONArray(
// Arrays.asList("failure", "taskid not supplied"));
// }
//
// Task task = Driver.getTaskkFromLog(taskId);
//
// if (task == null) {
// return new JSONArray(Arrays.asList(
// "",
// (new JSONObject().put("status",
// TaskkStatus.Unknown.toString()).toString())));
// }
//
// return new JSONArray(Arrays.asList(
// "",
// (new JSONObject().put("status", Driver.getTaskkFromLog(taskId)
// .getStatus().toString()).toString())));
// }
// catch (Exception e) {
// return new JSONArray(Arrays.asList("failure", e.getMessage()));
// }
// }
//
// @GET
// @Path(BatfishConstants.SERVICE_RUN_RESOURCE)
// @Produces(MediaType.APPLICATION_JSON)
// public JSONArray run(@Context UriInfo ui) {
// BatfishLogger logger = Driver.getMainLogger();
// try {
// MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
//
// List<String> argsList = new ArrayList<String>();
//
// String taskId = null;
//
// for (MultivaluedMap.Entry<String, List<String>> entry : queryParams
// .entrySet()) {
// logger.info(String.format("key = %s value = %s\n", entry.getKey(),
// entry.getValue()));
//
// // pull out the special key corresponding to taskid
// if (entry.getKey().equals(BatfishConstants.SERVICE_TASKID_KEY)) {
// taskId = entry.getValue().get(0);
// }
// else {
// argsList.add("-" + entry.getKey());
//
// for (String value : entry.getValue()) {
// // don't add empty values; occurs for options that have no
// // value
// if (!value.equals("")) {
// argsList.add(value);
// }
// }
// }
// }
//
// if (taskId == null) {
// return new JSONArray(Arrays.asList("failure",
// "TaskId was not supplied"));
// }
//
// String[] args = argsList.toArray(new String[argsList.size()]);
//
// logger.info(String.format("Will run with args: %s\n",
// Arrays.toString(args)));
//
// return new JSONArray(Driver.RunBatfishThroughService(taskId, args));
// }
// catch (Exception e) {
// return new JSONArray(Arrays.asList("failure", e.getMessage()));
// }
// }
// }
