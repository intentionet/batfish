package org.batfish.datamodel.table;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.answers.Schema;

@ParametersAreNonnullByDefault
public final class ColumnMetadata {
  private static final String PROP_DESCRIPTION = "description";
  private static final String PROP_IS_KEY = "isKey";
  private static final String PROP_IS_VALUE = "isValue";
  private static final String PROP_NAME = "name";
  private static final String PROP_SCHEMA = "schema";

  // must start with letters or underscore, and only contain {-,\w} ( i.e., [-a-zA-Z_0-9])
  public static final String COLUMN_NAME_PATTERN = "[a-zA-Z_][-\\w]*";
  private static final Pattern _COLUMN_NAME_PATTERN = Pattern.compile(COLUMN_NAME_PATTERN);

  @Nonnull private String _description;
  private boolean _isKey;
  private boolean _isValue;
  @Nonnull private String _name;
  @Nonnull private Schema _schema;

  public ColumnMetadata(String name, Schema schema, String description) {
    this(name, schema, description, null, null);
  }

  @JsonCreator
  private static ColumnMetadata jsonCreator(
      @Nullable @JsonProperty(PROP_NAME) String name,
      @Nullable @JsonProperty(PROP_SCHEMA) Schema schema,
      @Nullable @JsonProperty(PROP_DESCRIPTION) String description,
      @Nullable @JsonProperty(PROP_IS_KEY) Boolean isKey,
      @Nullable @JsonProperty(PROP_IS_VALUE) Boolean isValue) {
    checkArgument(name != null, "'name' cannot be null for ColumnMetadata");
    checkArgument(description != null, "'description' cannot be null for ColumnMetadata");
    checkArgument(schema != null, "'schema' cannot be null for ColumnMetadata");
    return new ColumnMetadata(name, schema, description, isKey, isValue);
  }

  public ColumnMetadata(
      String name,
      Schema schema,
      String description,
      @Nullable Boolean isKey,
      @Nullable Boolean isValue) {
    if (!isLegalColumnName(name)) {
      throw new IllegalArgumentException(
          String.format(
              "Illegal column name '%s'. Column names should match '%s",
              name, COLUMN_NAME_PATTERN.toString()));
    }
    _name = name;
    _schema = schema;
    _description = description;
    _isKey = firstNonNull(isKey, true);
    _isValue = firstNonNull(isValue, true);
  }

  public static boolean isLegalColumnName(String name) {
    return _COLUMN_NAME_PATTERN.matcher(name).matches();
  }

  @JsonProperty(PROP_DESCRIPTION)
  public String getDescription() {
    return _description;
  }

  @JsonProperty(PROP_IS_KEY)
  public boolean getIsKey() {
    return _isKey;
  }

  @JsonProperty(PROP_IS_VALUE)
  public boolean getIsValue() {
    return _isValue;
  }

  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  @JsonProperty(PROP_SCHEMA)
  public Schema getSchema() {
    return _schema;
  }
}
