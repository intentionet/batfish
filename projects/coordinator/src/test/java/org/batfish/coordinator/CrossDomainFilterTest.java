package org.batfish.coordinator;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.batfish.common.CoordConstsV2;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link CrossDomainFilter}. */
@RunWith(JUnit4.class)
public class CrossDomainFilterTest extends JerseyTest {

  @Path("/test")
  public static class TestService {
    public TestService() {
      throw new IllegalStateException("Should never reach here!");
    }

    @GET
    public Response get() {
      // Should be unreachable, as the test resource will crash on creation.
      return Response.ok().build();
    }
  }

  @Override
  protected Application configure() {
    forceSet(TestProperties.CONTAINER_PORT, "0");
    return new ResourceConfig(TestService.class).register(CrossDomainFilter.class);
  }

  @Test
  public void testGetRequestThrows() {
    Response response = target("/test").request().get();
    assertThat(response.getStatus(), equalTo(INTERNAL_SERVER_ERROR.getStatusCode()));
  }

  @Test
  public void testOptionsRequestSucceeds() {
    Response response = target("/test").request().options();

    assertThat(response.getStatus(), equalTo(OK.getStatusCode()));

    MultivaluedMap<String, String> headers = response.getStringHeaders();
    assertThat(headers, hasEntry(equalTo("Access-Control-Allow-Origin"), contains("*")));
    assertThat(
        headers,
        hasEntry(
            equalTo("Access-Control-Allow-Headers"),
            contains(
                allOf(
                    containsString(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY),
                    containsString(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION)))));
  }
}
