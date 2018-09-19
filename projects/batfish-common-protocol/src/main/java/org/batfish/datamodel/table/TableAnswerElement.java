package org.batfish.datamodel.table;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.batfish.common.BatfishException;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.datamodel.questions.Assertion;
import org.batfish.datamodel.questions.Exclusion;
import org.batfish.datamodel.questions.Question;

/** Holds tabular answers. */
public final class TableAnswerElement extends AnswerElement {

  private static final String PROP_EXCLUDED_ROWS = "excludedRows";

  private static final String PROP_METADATA = "metadata";

  private static final String PROP_ROWS = "rows";

  private List<ExcludedRows> _excludedRows;

  private Rows _rows;

  private List<Row> _rowsList;

  private TableMetadata _tableMetadata;

  @JsonCreator
  public TableAnswerElement(@Nonnull @JsonProperty(PROP_METADATA) TableMetadata tableMetadata) {
    _tableMetadata = tableMetadata;
    _rows = new Rows();
    _rowsList = new LinkedList<>();
    _excludedRows = new LinkedList<>();
  }

  /**
   * Adds a new row to data rows
   *
   * @param row The row to add
   */
  public TableAnswerElement addRow(Row row) {
    _rows.add(row);
    _rowsList.add(row);
    return this;
  }

  /**
   * Adds a new row to excluded data rows
   *
   * @param row The row to add
   */
  public void addExcludedRow(Row row, String exclusionName) {
    for (ExcludedRows exRows : _excludedRows) {
      if (exRows.getExclusionName().equals(exclusionName)) {
        exRows.addRow(row);
        return;
      }
    }
    // no matching exclusionName found; create a new one
    ExcludedRows rows = new ExcludedRows(exclusionName);
    rows.addRow(row);
    _excludedRows.add(rows);
  }

  /** Computes the summary of this table, given the assertion */
  public AnswerSummary computeSummary(Assertion assertion) {
    String notes = "Found " + _rows.size() + " results";
    return computeSummary(assertion, notes);
  }

  /** Computes the summary of this table, given the assertion and notes */
  public AnswerSummary computeSummary(Assertion assertion, String notes) {
    int numPassed = 0;
    int numFailed = 0;
    if (assertion != null) {
      if (evaluateAssertion(assertion)) {
        numPassed = 1;
      } else {
        numFailed = 1;
      }
    }
    return new AnswerSummary(notes, numFailed, numPassed, _rows.size());
  }

  /**
   * Evaluates the assertion over the rows
   *
   * @return The results of the evaluation
   */
  public boolean evaluateAssertion(Assertion assertion) {
    if (assertion == null) {
      throw new IllegalArgumentException("Provided assertion object cannot be null");
    }
    switch (assertion.getType()) {
      case countequals:
        return _rows.size() == assertion.getExpect().asInt();
      case countlessthan:
        return _rows.size() < assertion.getExpect().asInt();
      case countmorethan:
        return _rows.size() > assertion.getExpect().asInt();
      case equals:
        Rows expectedEntries;
        try {
          expectedEntries =
              BatfishObjectMapper.mapper().readValue(assertion.getExpect().toString(), Rows.class);
        } catch (IOException e) {
          throw new BatfishException("Could not recover Rows object from expect", e);
        }
        return _rows.equals(expectedEntries);
      default:
        throw new BatfishException("Unhandled assertion type: " + assertion.getType());
    }
  }

  @JsonProperty(PROP_EXCLUDED_ROWS)
  public List<ExcludedRows> getExcludedRows() {
    return _excludedRows;
  }

  @JsonProperty(PROP_METADATA)
  public TableMetadata getMetadata() {
    return _tableMetadata;
  }

  @JsonIgnore
  public Rows getRows() {
    return _rows;
  }

  @JsonProperty(PROP_ROWS)
  public List<Row> getRowsList() {
    return ImmutableList.copyOf(_rowsList);
  }

  /**
   * Given an initial set of rows produced by an {@link org.batfish.common.Answerer}, this procedure
   * processes exclusions, assertions, and summary to update this object.
   *
   * @param question The question that generated the initial set of rows
   * @param initialSet The initial set of rows
   */
  public void postProcessAnswer(Question question, Multiset<Row> initialSet) {
    initialSet.forEach(
        initialRow -> {
          // exclude or not?
          Exclusion exclusion = Exclusion.covered(initialRow, question.getExclusions());
          if (exclusion != null) {
            addExcludedRow(initialRow, exclusion.getName());
          } else {
            addRow(initialRow);
          }
        });

    setSummary(computeSummary(question.getAssertion()));
  }

  @JsonProperty(PROP_EXCLUDED_ROWS)
  private void setExcludedRows(List<ExcludedRows> excludedRows) {
    _excludedRows = excludedRows == null ? new LinkedList<>() : excludedRows;
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
}
