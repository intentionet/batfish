package org.batfish.coordinator;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/** Allow the system to serve xhr level 2 from all cross domain site */
@Provider
@Priority(-1) /* Highest priority to set the CORS filter as the first one to run. */
public class CrossDomainFilter implements ContainerRequestFilter, ContainerResponseFilter {
  /**
   * Recognizes a CORS preflight request, and return OK without any further downstring processing in
   * such a case.
   */
  @Override
  public void filter(ContainerRequestContext creq) {
    if (HttpMethod.OPTIONS.equals(creq.getMethod())) {
      // Cache these responses.
      CacheControl control = new CacheControl();
      control.setMaxAge(3600);
      creq.abortWith(Response.ok().cacheControl(control).build());
    }
  }

  /**
   * Add the cross domain data to the output if needed
   *
   * @param creq The container request (input)
   * @param cres The container request (output)
   * @return The output request with cross domain if needed
   */
  @Override
  public void filter(ContainerRequestContext creq, ContainerResponseContext cres) {
    cres.getHeaders().add("Access-Control-Allow-Origin", "*");
    cres.getHeaders()
        .add(
            "Access-Control-Allow-Headers",
            "Host, User-Agent, Accept, Authorization, Accept-Language, Accept-Encoding, "
                + "Content-Type, Referer, Content-Length, Origin, DNT, Connection, "
                + "version, apikey");
    cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
    cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    cres.getHeaders().add("Access-Control-Max-Age", "3600");
  }
}
