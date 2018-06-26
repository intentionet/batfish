package org.batfish.coordinator;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.batfish.common.CoordConstsV2;

/** Allow the system to serve xhr level 2 from all cross domain site */
@PreMatching
@Priority(-1) /* Highest priority to set the CORS filter as the first one to run. */
@Provider
public class CrossDomainFilter implements ContainerRequestFilter, ContainerResponseFilter {
  /**
   * Recognizes a CORS preflight request, and return OK without any further downstream processing in
   * such a case.
   *
   * <p>Note that the response filter below will still apply the relevant headers to the response.
   */
  @Override
  public void filter(ContainerRequestContext creq) {
    if (HttpMethod.OPTIONS.equals(creq.getMethod())) {
      creq.abortWith(Response.ok().build());
    }
  }

  /**
   * Add the cross domain data to the output if needed
   *
   * @param creq The container request (input)
   * @param cres The container response (output)
   */
  @Override
  public void filter(ContainerRequestContext creq, ContainerResponseContext cres) {
    cres.getHeaders().add("Access-Control-Allow-Origin", "*");
    cres.getHeaders()
        .add(
            "Access-Control-Allow-Headers",
            String.join(
                /* delimiter */ ", ",
                "Host",
                "User-Agent",
                "Accept",
                "Authorization",
                "Accept-Language",
                "Accept-Encoding",
                "Content-Type",
                "Referer",
                "Content-Length",
                "Origin",
                "DNT",
                "Connection",
                CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY,
                CoordConstsV2.HTTP_HEADER_BATFISH_VERSION));

    cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
    cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    cres.getHeaders().add("Access-Control-Max-Age", "3600");
  }
}
