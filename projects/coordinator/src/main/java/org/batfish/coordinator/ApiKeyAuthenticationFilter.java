package org.batfish.coordinator;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.common.CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import org.batfish.common.CoordConsts;

/** This filter verifies the apiKey provided in request header is a valid work apikey. */
@PreMatching
@Provider
public class ApiKeyAuthenticationFilter implements ContainerRequestFilter {
  @Override
  public void filter(ContainerRequestContext requestContext) {
    String apiKey =
        firstNonNull(
            requestContext.getHeaderString(HTTP_HEADER_BATFISH_APIKEY),
            CoordConsts.DEFAULT_API_KEY);
    if (apiKey.isEmpty()) {
      requestContext.abortWith(
          Response.status(Status.UNAUTHORIZED)
              .entity(
                  String.format(
                      "HTTP header %s should contain an API key", HTTP_HEADER_BATFISH_APIKEY))
              .type(MediaType.APPLICATION_JSON)
              .build());
    } else if (!Main.getAuthorizer().isValidWorkApiKey(apiKey)) {
      requestContext.abortWith(
          Response.status(Status.UNAUTHORIZED)
              .entity(String.format("Authorizer: '%s' is NOT a valid key", apiKey))
              .type(MediaType.APPLICATION_JSON)
              .build());
    }
  }
}
