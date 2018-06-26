package org.batfish.coordinator;

import static org.batfish.common.CoordConstsV2.HTTP_HEADER_BATFISH_VERSION;

import com.google.common.base.Strings;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import org.batfish.common.Version;

/**
 * This filter verifies that the client's version is compatible with the version of Batfish service.
 */
@PreMatching
@Provider
public class VersionCompatibilityFilter implements ContainerRequestFilter {

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String clientVersion = requestContext.getHeaderString(HTTP_HEADER_BATFISH_VERSION);
    if (Strings.isNullOrEmpty(clientVersion)) {
      requestContext.abortWith(
          Response.status(Status.BAD_REQUEST)
              .entity(
                  String.format(
                      "HTTP header %s should contain a client version",
                      HTTP_HEADER_BATFISH_VERSION))
              .type(MediaType.APPLICATION_JSON)
              .build());
    } else if (!Version.isCompatibleVersion("Service", "Client", clientVersion)) {
      requestContext.abortWith(
          Response.status(Status.BAD_REQUEST)
              .entity(
                  String.format(
                      "Client version '%s' is not compatible with server version '%s'",
                      clientVersion, Version.getVersion()))
              .type(MediaType.APPLICATION_JSON)
              .build());
    }
  }
}
