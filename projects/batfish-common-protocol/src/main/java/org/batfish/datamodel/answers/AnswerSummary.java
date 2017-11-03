package org.batfish.datamodel.answers;

import com.google.common.base.Strings;

public class AnswerSummary {

  private String _notes = "";

  private int _numFailed;

  private int _numPassed;

  private int _numResults;

  public AnswerSummary() {}

  public AnswerSummary(String notes, int numFailed, int numPassed, int numResults) {
    _notes = notes;
    _numFailed = numFailed;
    _numPassed = numPassed;
    _numResults = numResults;
  }

  public void combine(AnswerSummary other) {
    if (other != null) {
      _notes =
          (Strings.isNullOrEmpty(_notes)) ? other.getNotes() : _notes + "; " + other.getNotes();
      _numFailed += other.getNumFailed();
      _numPassed += other.getNumPassed();
      _numResults += other.getNumResults();
    }
  }

  public String getNotes() {
    return _notes;
  }

  public int getNumFailed() {
    return _numFailed;
  }

  public int getNumPassed() {
    return _numPassed;
  }

  public int getNumResults() {
    return _numResults;
  }

  public String prettyPrint() {
    String retString = "";
    if (_numPassed != 0 || _numFailed != 0) {
      retString += String.format("Assertions: %d passed, %d failed; ", _numPassed, _numFailed);
    }
    retString += String.format("%d (non-assertion) results; ", _numResults);
    if (!Strings.isNullOrEmpty(_notes)) {
      retString += String.format("Notes: %s", _notes);
    }
    return retString;
  }

  public void reset() {
    _notes = null;
    _numFailed = 0;
    _numPassed = 0;
    _numResults = 0;
  }

  public void setNotes(String notes) {
    this._notes = notes;
  }

  public void setNumFailed(int numFailed) {
    this._numFailed = numFailed;
  }

  public void setNumPassed(int numPassed) {
    this._numPassed = numPassed;
  }

  public void setNumResults(int numResults) {
    this._numResults = numResults;
  }
}
