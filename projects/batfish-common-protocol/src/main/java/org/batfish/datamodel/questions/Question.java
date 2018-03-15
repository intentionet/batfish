package org.batfish.datamodel.questions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.batfish.common.BatfishException;
import org.batfish.common.BfConsts;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.questions.Question.InstanceData.Variable;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public abstract class Question implements IQuestion {

  public static class InstanceData {

    public static class Variable {

      public enum Type {
        BOOLEAN("boolean", false),
        COMPARATOR("comparator", true),
        DOUBLE("double", false),
        FLOAT("float", false),
        INTEGER("integer", false),
        IP("ip", true),
        IP_PROTOCOL("ipProtocol", true),
        IP_WILDCARD("ipWildcard", true),
        JAVA_REGEX("javaRegex", true),
        JSON_PATH("jsonPath", true),
        JSON_PATH_REGEX("jsonPathRegex", true),
        LONG("long", false),
        PREFIX("prefix", true),
        PREFIX_RANGE("prefixRange", true),
        PROTOCOL("protocol", true),
        QUESTION("question", true),
        STRING("string", true),
        SUBRANGE("subrange", true);

        private static final Map<String, Type> MAP = initMap();

        @JsonCreator
        public static Type fromString(String name) {
          Type value = MAP.get(name.toLowerCase());
          if (value == null) {
            throw new BatfishException(
                "No " + Type.class.getSimpleName() + " with name: '" + name + "'");
          }
          return value;
        }

        private static Map<String, Type> initMap() {
          ImmutableMap.Builder<String, Type> map = ImmutableMap.builder();
          for (Type value : Type.values()) {
            String name = value._name.toLowerCase();
            map.put(name, value);
          }
          return map.build();
        }

        private final String _name;

        private final boolean _stringType;

        Type(String name, boolean stringType) {
          _name = name;
          _stringType = stringType;
        }

        @JsonValue
        public String getName() {
          return _name;
        }

        public boolean getStringType() {
          return _stringType;
        }
      }

      private SortedSet<String> _allowedValues;

      private String _description;

      private Integer _minElements;

      private Integer _minLength;

      private boolean _optional;

      private Type _type;

      private JsonNode _value;

      public Variable() {
        _allowedValues = new TreeSet<>();
      }

      @JsonProperty(BfConsts.PROP_ALLOWED_VALUES)
      public SortedSet<String> getAllowedValues() {
        return _allowedValues;
      }

      @JsonProperty(BfConsts.PROP_DESCRIPTION)
      public String getDescription() {
        return _description;
      }

      @JsonProperty(BfConsts.PROP_MIN_ELEMENTS)
      public Integer getMinElements() {
        return _minElements;
      }

      @JsonProperty(BfConsts.PROP_MIN_LENGTH)
      public Integer getMinLength() {
        return _minLength;
      }

      @JsonProperty(BfConsts.PROP_OPTIONAL)
      public boolean getOptional() {
        return _optional;
      }

      @JsonProperty(BfConsts.PROP_TYPE)
      public Type getType() {
        return _type;
      }

      @JsonProperty(BfConsts.PROP_VALUE)
      @JsonInclude(Include.NON_NULL)
      public JsonNode getValue() {
        return _value;
      }

      @JsonProperty(BfConsts.PROP_ALLOWED_VALUES)
      public void setAllowedValues(SortedSet<String> allowedValues) {
        _allowedValues = allowedValues;
      }

      @JsonProperty(BfConsts.PROP_DESCRIPTION)
      public void setDescription(String description) {
        _description = description;
      }

      @JsonProperty(BfConsts.PROP_MIN_ELEMENTS)
      public void setMinElements(Integer minElements) {
        _minElements = minElements;
      }

      @JsonProperty(BfConsts.PROP_MIN_LENGTH)
      public void setMinLength(Integer minLength) {
        _minLength = minLength;
      }

      @JsonProperty(BfConsts.PROP_OPTIONAL)
      public void setOptional(boolean optional) {
        _optional = optional;
      }

      @JsonProperty(BfConsts.PROP_TYPE)
      public void setType(Type type) {
        _type = type;
      }

      @JsonProperty(BfConsts.PROP_VALUE)
      public void setValue(JsonNode value) {
        if (value != null && value.isNull()) {
          _value = null;
        } else {
          _value = value;
        }
      }
    }

    private String _description;

    private String _instanceName;

    private String _longDescription;

    private SortedSet<String> _tags;

    private SortedMap<String, Variable> _variables;

    public InstanceData() {
      _tags = new TreeSet<>();
      _variables = new TreeMap<>();
    }

    @JsonProperty(BfConsts.PROP_DESCRIPTION)
    public String getDescription() {
      return _description;
    }

    @JsonProperty(BfConsts.PROP_INSTANCE_NAME)
    public String getInstanceName() {
      return _instanceName;
    }

    @JsonProperty(BfConsts.PROP_LONG_DESCRIPTION)
    public String getLongDescription() {
      return _longDescription;
    }

    @JsonProperty(BfConsts.PROP_TAGS)
    public SortedSet<String> getTags() {
      return _tags;
    }

    @JsonProperty(BfConsts.PROP_VARIABLES)
    public SortedMap<String, Variable> getVariables() {
      return _variables;
    }

    @JsonProperty(BfConsts.PROP_DESCRIPTION)
    public void setDescription(String description) {
      _description = description;
    }

    @JsonProperty(BfConsts.PROP_INSTANCE_NAME)
    public void setInstanceName(String instanceName) {
      _instanceName = instanceName;
    }

    @JsonProperty(BfConsts.PROP_LONG_DESCRIPTION)
    public void setLongDescription(String longDescription) {
      _longDescription = longDescription;
    }

    @JsonProperty(BfConsts.PROP_TAGS)
    public void setTags(SortedSet<String> tags) {
      _tags = tags;
    }

    @JsonProperty(BfConsts.PROP_VARIABLES)
    public void setVariables(SortedMap<String, Variable> variables) {
      _variables = variables;
    }
  }

  private boolean _differential;

  private InstanceData _instance;

  public Question() {
    _differential = false;
  }

  public Question configureTemplate(String exceptions, String assertion) {
    throw new UnsupportedOperationException(
        "configureTemplate is not supported for question type: " + this.getClass().getName());
  }

  /** Returns {@code true} iff this question requires a computed data plane as input. */
  @JsonIgnore
  public abstract boolean getDataPlane();

  @JsonProperty(BfConsts.PROP_DIFFERENTIAL)
  public boolean getDifferential() {
    return _differential;
  }

  @JsonProperty(BfConsts.PROP_INSTANCE)
  public InstanceData getInstance() {
    return _instance;
  }

  /**
   * Returns the short name of this question, used in place of the classname to identify this
   * question.
   */
  @JsonIgnore
  public abstract String getName();

  // by default, pretty printing is Json
  // override this function in derived classes to do something more meaningful
  public String prettyPrint() {
    try {
      return BatfishObjectMapper.writePrettyString(this);
    } catch (JsonProcessingException e) {
      throw new BatfishException("Failed to pretty-print question", e);
    }
  }

  protected String prettyPrintBase() {
    String retString = "";
    // for brevity, print only if the values are non-default
    if (_differential) {
      retString += String.format("differential=%s", _differential);
    }
    if (retString.isEmpty()) {
      return "";
    } else {
      return retString + ", ";
    }
  }

  public static Question parseQuestion(Path questionPath) {
    return parseQuestion(CommonUtil.readFile(questionPath));
  }

  public static Question parseQuestion(String rawQuestionText) {
    String questionText = Question.preprocessQuestion(rawQuestionText);
    try {
      Question question = BatfishObjectMapper.mapper().readValue(questionText, Question.class);
      return question;
    } catch (IOException e) {
      throw new BatfishException("Could not parse JSON question", e);
    }
  }

  private static String preprocessQuestion(String rawQuestionText) {
    try {
      JSONObject jobj = new JSONObject(rawQuestionText);
      if (jobj.has(BfConsts.PROP_INSTANCE) && !jobj.isNull(BfConsts.PROP_INSTANCE)) {
        String instanceDataStr = jobj.getString(BfConsts.PROP_INSTANCE);
        InstanceData instanceData =
            BatfishObjectMapper.mapper()
                .readValue(instanceDataStr, new TypeReference<InstanceData>() {});
        for (Entry<String, Variable> e : instanceData.getVariables().entrySet()) {
          String varName = e.getKey();
          Variable variable = e.getValue();
          JsonNode value = variable.getValue();
          if (value == null) {
            if (variable.getOptional()) {
              /*
               * Recursively look for all key, value pairs and remove keys
               * whose value is "${varName}." Is this fragile?
               * To be doubly sure, we do only for keys whole parent object is a questions, which
               * we judge by it having a key "class" whose value starts with "org.batfish.question"
               */
              recursivelyRemoveOptionalVar(jobj, varName);
            } else {
              // What to do here? For now, do nothing and assume that
              // later validation will handle it.
            }
            continue;
          }
          if (variable.getType() == Variable.Type.QUESTION) {
            if (variable.getMinElements() != null) {
              if (!value.isArray()) {
                throw new IllegalArgumentException("Expecting JSON array for array type");
              }
              JSONArray arr = new JSONArray();
              for (int i = 0; i < value.size(); i++) {
                String valueJsonString = new ObjectMapper().writeValueAsString(value.get(i));
                arr.put(i, new JSONObject(preprocessQuestion(valueJsonString)));
              }
              jobj.put(varName, arr);
            } else {
              String valueJsonString = new ObjectMapper().writeValueAsString(value);
              jobj.put(varName, new JSONObject(preprocessQuestion(valueJsonString)));
            }
          }
        }
        String questionText = jobj.toString();
        for (Entry<String, Variable> e : instanceData.getVariables().entrySet()) {
          String varName = e.getKey();
          Variable variable = e.getValue();
          JsonNode value = variable.getValue();
          String valueJsonString = new ObjectMapper().writeValueAsString(value);
          boolean stringType = variable.getType().getStringType();
          boolean setType = variable.getMinElements() != null;
          if (value != null) {
            String topLevelVarNameRegex = Pattern.quote("\"${" + varName + "}\"");
            String inlineVarNameRegex = Pattern.quote("${" + varName + "}");
            String topLevelReplacement = valueJsonString;
            String inlineReplacement;
            if (stringType && !setType) {
              inlineReplacement = valueJsonString.substring(1, valueJsonString.length() - 1);
            } else {
              String quotedValueJsonString = JSONObject.quote(valueJsonString);
              inlineReplacement =
                  quotedValueJsonString.substring(1, quotedValueJsonString.length() - 1);
            }
            String inlineReplacementRegex = Matcher.quoteReplacement(inlineReplacement);
            String topLevelReplacementRegex = Matcher.quoteReplacement(topLevelReplacement);
            questionText = questionText.replaceAll(topLevelVarNameRegex, topLevelReplacementRegex);
            questionText = questionText.replaceAll(inlineVarNameRegex, inlineReplacementRegex);
          }
        }
        return questionText;
      }
      return rawQuestionText;
    } catch (JSONException | IOException e) {
      throw new BatfishException(
          String.format("Could not convert raw question text [%s] to JSON", rawQuestionText), e);
    }
  }

  private static void recursivelyRemoveOptionalVar(JSONObject questionObject, String varName)
      throws JSONException {
    Iterator<?> iter = questionObject.keys();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      Object value = questionObject.get(key);
      if (value instanceof String) {
        if (value.equals("${" + varName + "}")) {
          iter.remove();
        }
      } else if (value instanceof JSONObject) {
        JSONObject childObject = (JSONObject) value;
        if (childObject.has("class")) {
          Object classValue = childObject.get("class");
          if (classValue instanceof String
              && ((String) classValue).startsWith("org.batfish.question")) {
            recursivelyRemoveOptionalVar(childObject, varName);
          }
        }
      }
    }
  }

  @JsonProperty(BfConsts.PROP_DIFFERENTIAL)
  public void setDifferential(boolean differential) {
    _differential = differential;
  }

  @JsonProperty(BfConsts.PROP_INSTANCE)
  public void setInstance(InstanceData instance) {
    _instance = instance;
  }

  @Override
  public String toFullJsonString() {
    try {
      return BatfishObjectMapper.verboseWriter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new BatfishException("Failed to convert question to full JSON string", e);
    }
  }

  @Override
  public String toJsonString() {
    try {
      return BatfishObjectMapper.writePrettyString(this);
    } catch (JsonProcessingException e) {
      throw new BatfishException("Failed to convert question to JSON string", e);
    }
  }
}
