package org.batfish.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Throwables;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.batfish.datamodel.answers.AnswerElement;

/**
 * Thrown as a fatal exception. When caught, Batfish should perform any necessary cleanup and
 * terminate gracefully with a non-zero exit status. A BatfishException should always contain a
 * detail message.
 */
public class BatfishException extends RuntimeException {

  public static class BatfishStackTrace extends AnswerElement implements Serializable {

    private static final String PROP_LINES = "answer";

    /** */
    private static final long serialVersionUID = 1L;

    private final transient BatfishException _exception;

    private final List<String> _lines;

    public BatfishStackTrace(BatfishException exception) {
      String stackTrace = Throwables.getStackTraceAsString(exception).replace("\t", "   ");
      _lines = Arrays.asList(stackTrace.split("\\n", -1));
      _exception = exception;
    }

    @JsonCreator
    public BatfishStackTrace(@JsonProperty(PROP_LINES) List<String> lines) {
      _lines = lines;
      _exception = null;
    }

    @JsonIgnore
    public BatfishException getException() {
      return _exception;
    }

    @JsonProperty(PROP_LINES)
    public List<String> getLineMap() {
      return _lines;
    }

    @Override
    public String prettyPrint() {
      return String.join("\n", _lines);
    }
  }

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a BatfishException with a detail message
   *
   * @param msg The detail message
   */
  public BatfishException(String msg) {
    super(msg);
  }

  /**
   * Constructs a BatfishException with a detail message and a cause
   *
   * @param msg The detail message
   * @param cause The cause of this exception
   */
  public BatfishException(String msg, Throwable cause) {
    super(msg, cause);
  }

  @JsonValue
  public BatfishStackTrace getBatfishStackTrace() {
    return new BatfishStackTrace(this);
  }
}
