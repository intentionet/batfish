package org.batfish.coordinator;

import com.google.common.base.Throwables;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {
  @Override
  public Response toResponse(Exception exception) {
    if (exception instanceof WebApplicationException) {
      // An exception that already maps directly to a standard HTTP Response, likely thrown
      // somewhere inside Jersey aka if the user typed a bad URI.
      return ((WebApplicationException) exception).getResponse();
    }

    exception.printStackTrace();
    String stackTrace = Throwables.getStackTraceAsString(exception);
    return Response.status(500).entity(stackTrace).build();
  }
}
