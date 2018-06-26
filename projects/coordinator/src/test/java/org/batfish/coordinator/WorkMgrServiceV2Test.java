package org.batfish.coordinator;

import static javax.ws.rs.core.Response.Status.MOVED_PERMANENTLY;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.glassfish.jersey.client.ClientProperties.FOLLOW_REDIRECTS;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.batfish.common.Container;
import org.batfish.common.CoordConsts;
import org.batfish.common.CoordConstsV2;
import org.batfish.common.Version;
import org.batfish.coordinator.authorizer.Authorizer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WorkMgrServiceV2Test extends WorkMgrServiceV2TestBase {
  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Before
  public void initContainerEnvironment() throws Exception {
    WorkMgrTestUtils.initWorkManager(_folder);
  }

  private WebTarget getContainersTarget() {
    return target(CoordConsts.SVC_CFG_WORK_MGR2).path(CoordConstsV2.RSC_CONTAINERS);
  }

  @Test
  public void getContainers() {
    Response response =
        getContainersTarget()
            .request()
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION, Version.getVersion())
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, CoordConsts.DEFAULT_API_KEY)
            .get();
    assertThat(response.getStatus(), equalTo(OK.getStatusCode()));
    assertThat(response.readEntity(new GenericType<List<Container>>() {}), empty());

    Main.getWorkMgr().initContainer("someContainer", null);
    response =
        getContainersTarget()
            .request()
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION, Version.getVersion())
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, CoordConsts.DEFAULT_API_KEY)
            .get();
    assertThat(response.getStatus(), equalTo(OK.getStatusCode()));
    assertThat(response.readEntity(new GenericType<List<Container>>() {}), hasSize(1));
  }

  @Test
  public void redirectContainer() {
    Response response =
        target(CoordConsts.SVC_CFG_WORK_MGR2)
            .path(CoordConstsV2.RSC_CONTAINER)
            .property(FOLLOW_REDIRECTS, false)
            .request()
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION, Version.getVersion())
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, CoordConsts.DEFAULT_API_KEY)
            .get();
    assertThat(response.getStatus(), equalTo(MOVED_PERMANENTLY.getStatusCode()));
    assertThat(response.getLocation().getPath(), equalTo("/v2/containers"));
  }

  /** Test that the ApiKey is extracted from the correct header */
  @Test
  public void correctApiKeyHeader() {
    // Set up by making a call to authorize container
    String containerName = "someContainer";
    String myKey = "ApiKey";
    Authorizer auth = new MapAuthorizer();
    Main.setAuthorizer(auth);
    auth.authorizeContainer(myKey, containerName);
    Main.getWorkMgr().initContainer(containerName, null);

    // Test that subsequent calls return 200 with correct API key
    Response resp =
        getContainersTarget()
            .path(containerName)
            .request()
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION, Version.getVersion())
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, myKey)
            .get();
    assertThat(resp.getStatus(), equalTo(OK.getStatusCode()));
    assertThat(
        resp.readEntity(Container.class),
        equalTo(Container.of(containerName, Collections.emptySortedSet())));

    // Test that subsequent calls return 401 unauthorized with wrong API key
    resp =
        getContainersTarget()
            .path(containerName)
            .request()
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_VERSION, Version.getVersion())
            .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, "wrongKey")
            .get();
    assertThat(resp.getStatus(), equalTo(UNAUTHORIZED.getStatusCode()));
    assertThat(resp.readEntity(String.class), equalTo("Authorizer: 'wrongKey' is NOT a valid key"));
  }
}
