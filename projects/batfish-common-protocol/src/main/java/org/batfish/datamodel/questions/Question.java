package org.batfish.datamodel.questions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.common.BatfishException;
import org.batfish.common.BfConsts;
import org.batfish.common.util.BatfishObjectMapper;

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

        private static synchronized Map<String, Type> initMap() {
          Map<String, Type> map = new HashMap<>();
          for (Type value : Type.values()) {
            String name = value._name.toLowerCase();
            map.put(name, value);
          }
          return Collections.unmodifiableMap(map);
        }

        private final String _name;

        private final boolean _stringType;

        private Type(String name, boolean stringType) {
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

  @JsonIgnore
  public abstract String getName();

  @JsonIgnore
  public abstract boolean getTraffic();

  protected boolean isBaseParamKey(String paramKey) {
    switch (paramKey) {
      case BfConsts.PROP_DIFFERENTIAL:
        return true;
      default:
        return false;
    }
  }

  // by default, pretty printing is Json
  // override this function in derived classes to do something more meaningful
  public String prettyPrint() {
    ObjectMapper mapper = new BatfishObjectMapper();
    try {
      return mapper.writeValueAsString(this);
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
    if (retString == "") {
      return "";
    } else {
      return retString + ", ";
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
    ObjectMapper mapper = new BatfishObjectMapper();
    mapper.setSerializationInclusion(Include.ALWAYS);
    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new BatfishException("Failed to convert question to full JSON string", e);
    }
  }

  @Override
  public String toJsonString() {
    ObjectMapper mapper = new BatfishObjectMapper();
    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new BatfishException("Failed to convert question to JSON string", e);
    }
  }
}
