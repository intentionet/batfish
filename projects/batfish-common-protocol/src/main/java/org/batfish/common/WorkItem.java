package org.batfish.common;

import io.opentracing.ActiveSpan;
import io.opentracing.NoopActiveSpanSource.NoopActiveSpan;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class WorkItem {

  public static WorkItem fromJsonString(String jsonString) throws JSONException {

    JSONArray array = new JSONArray(jsonString);

    UUID id = UUID.fromString(array.get(0).toString());

    String containerName = array.get(1).toString();
    String testrigName = array.get(2).toString();

    HashMap<String, String> requestParams = new HashMap<>();
    HashMap<String, String> responseParams = new HashMap<>();

    JSONObject requestObject = new JSONObject(array.get(3).toString());
    JSONObject responseObject = new JSONObject(array.get(4).toString());

    populateHashMap(requestParams, requestObject);
    populateHashMap(responseParams, responseObject);

    return new WorkItem(id, containerName, testrigName, requestParams, responseParams);
  }

  private static void populateHashMap(HashMap<String, String> map, JSONObject jsonObject)
      throws JSONException {

    Iterator<?> keys = jsonObject.keys();

    while (keys.hasNext()) {
      String key = (String) keys.next();
      map.put(key, jsonObject.getString(key));
    }
  }

  private String _containerName;
  private UUID _id;
  private HashMap<String, String> _requestParams;
  private HashMap<String, String> _responseParams;
  private String _testrigName;
  private Map<String, String> _spanData; /* Map used by the TextMap carrier for SpanContext */

  public WorkItem(String containerName, String testrigName) {
    _id = UUID.randomUUID();
    _containerName = containerName;
    _testrigName = testrigName;
    _requestParams = new HashMap<>();
    _responseParams = new HashMap<>();
    _spanData = new HashMap<>();
  }

  public WorkItem(
      UUID id,
      String containerName,
      String testrigName,
      HashMap<String, String> reqParams,
      HashMap<String, String> resParams) {
    _id = id;
    _containerName = containerName;
    _testrigName = testrigName;
    _requestParams = reqParams;
    _responseParams = resParams;
    _spanData = new HashMap<>();
  }

  public void addRequestParam(String key, String value) {
    _requestParams.put(key, value);
  }

  public String getContainerName() {
    return _containerName;
  }

  public UUID getId() {
    return _id;
  }

  /**
   * Retrieves a {@link SpanContext} which was serialized earlier in the {@link WorkItem}
   *
   * @return {@link SpanContext} or null if no {@link SpanContext} was serialized in the {@link
   *     WorkItem}
   */
  public SpanContext getSourceSpan(Tracer tracer) {
    return tracer.extract(Builtin.TEXT_MAP, new TextMapExtractAdapter(_spanData));
  }

  public HashMap<String, String> getRequestParams() {
    return _requestParams;
  }

  public String getTestrigName() {
    return _testrigName;
  }

  /**
   * Gets the {@link ActiveSpan} from the current {@link Thread} and attaches it to the {@link
   * WorkItem} which can be fetched later using {@link WorkItem#getSourceSpan(Tracer)}
   */
  public void setSourceSpan(Tracer tracer, boolean isRegistered) {
    ActiveSpan sourceSpan = isRegistered ? tracer.activeSpan() : NoopActiveSpan.INSTANCE;
    tracer.inject(sourceSpan.context(), Builtin.TEXT_MAP, new TextMapInjectAdapter(_spanData));
  }

  public void setId(String idString) {
    _id = UUID.fromString(idString);
  }

  public String toJsonString() {
    JSONObject requestObject = new JSONObject(_requestParams);
    JSONObject responseObject = new JSONObject(_responseParams);
    JSONArray array =
        new JSONArray(
            Arrays.asList(
                _id,
                _containerName,
                _testrigName,
                requestObject.toString(),
                responseObject.toString()));
    return array.toString();
  }

  public JSONObject toTask() {
    return new JSONObject(_requestParams);
  }
}
