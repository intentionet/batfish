package org.batfish.coordinator;

import com.google.common.base.Strings;
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
      WebApplicationException e = (WebApplicationException) exception;
      Response r = e.getResponse();
      if (r.hasEntity()) {
        return r;
      } else if (!Strings.isNullOrEmpty(e.getMessage())) {
        return Response.fromResponse(r).entity(e.getMessage()).build();
      } else {
        String stackTrace = Throwables.getStackTraceAsString(exception);
        return Response.fromResponse(r).entity(stackTrace).build();
      }
    }

    exception.printStackTrace();
    String stackTrace = Throwables.getStackTraceAsString(exception);
    return Response.status(500).entity(stackTrace).build();
  }
}
