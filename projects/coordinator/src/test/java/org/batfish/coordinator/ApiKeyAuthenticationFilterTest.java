package org.batfish.coordinator;

import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.batfish.common.CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import org.batfish.common.CoordConsts;
import org.batfish.coordinator.authorizer.Authorizer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link ApiKeyAuthenticationFilter}. */
public class ApiKeyAuthenticationFilterTest extends JerseyTest {

  @Path("/test")
  public static class TestService {
    @GET
    public Response get() {
      return Response.ok().build();
    }
  }

  private static class TestAuthorizer implements Authorizer {

    @Override
    public void authorizeContainer(String apiKey, String containerName) {}

    @Override
    public boolean isAccessibleContainer(String apiKey, String containerName, boolean logError) {
      return false;
    }

    @Override
    public boolean isValidWorkApiKey(String apiKey) {
      return apiKey.equals(CoordConsts.DEFAULT_API_KEY);
    }
  }

  @Before
  public void initAuthorizer() {
    Main.setAuthorizer(new TestAuthorizer());
  }

  @Override
  protected Application configure() {
    forceSet(TestProperties.CONTAINER_PORT, "0");
    return new ResourceConfig(TestService.class).register(ApiKeyAuthenticationFilter.class);
  }

  @Test
  public void testEmptyApiKey() {
    Response response = target("/test").request().header(HTTP_HEADER_BATFISH_APIKEY, "").get();
    assertThat(response.getStatus(), equalTo(UNAUTHORIZED.getStatusCode()));
    assertThat(
        response.readEntity(String.class),
        equalTo("HTTP header " + HTTP_HEADER_BATFISH_APIKEY + " should contain an API key"));
  }

  @Test
  public void testDefaultKeyWhenApiKeyIsMissing() {
    Response response = target("/test").request().get();
    assertThat(response.getStatus(), equalTo(OK.getStatusCode()));
  }

  @Test
  public void testUnauthorizedApiKey() {
    Response response = target("/test").request().header(HTTP_HEADER_BATFISH_APIKEY, "100").get();
    assertThat(response.getStatus(), equalTo(UNAUTHORIZED.getStatusCode()));
    String expectMessage = "Authorizer: '100' is NOT a valid key";
    assertThat(response.readEntity(String.class), equalTo(expectMessage));
  }

  @Test
  public void testValidApiKey() {
    Response response =
        target("/test")
            .request()
            .header(HTTP_HEADER_BATFISH_APIKEY, CoordConsts.DEFAULT_API_KEY)
            .get();
    assertThat(response.getStatus(), equalTo(OK.getStatusCode()));
  }
}
