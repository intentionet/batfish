package org.batfish.datamodel.table;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Represents rows that have been excluded from {@link TableAnswerElement} because they were covered
 * by an exclusion.
 */
public class ExcludedRows {

  private static final String PROP_EXCLUSION_NAME = "exclusionName";

  private static final String PROP_ROWS = "rows";

  private @Nonnull final String _exclusionName;

  private @Nonnull Rows _rows;

  private List<Row> _rowsList;

  @JsonCreator
  public ExcludedRows(@Nonnull @JsonProperty(PROP_EXCLUSION_NAME) String exclusionName) {
    _exclusionName = exclusionName;
    _rows = new Rows();
    _rowsList = new LinkedList<>();
  }

  public void addRow(Row row) {
    _rows.add(row);
    _rowsList.add(row);
  }

  @JsonProperty(PROP_EXCLUSION_NAME)
  public String getExclusionName() {
    return _exclusionName;
  }

  @JsonIgnore
  public Rows getRows() {
    return _rows;
  }

  @JsonProperty(PROP_ROWS)
  private void setRowsList(List<Row> rows) {
    _rows = new Rows();
    if (rows == null) {
      _rowsList = new LinkedList<>();

    } else {
      _rowsList = rows;
    }
    _rowsList.forEach(_rows::add);
  }

  @JsonProperty(PROP_ROWS)
  public List<Row> getRowsList() {
    return ImmutableList.copyOf(_rowsList);
  }
}
