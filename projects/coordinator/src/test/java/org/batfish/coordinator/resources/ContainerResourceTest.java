package org.batfish.coordinator.resources;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.batfish.common.Container;
import org.batfish.common.CoordConsts;
import org.batfish.common.CoordConstsV2;
import org.batfish.coordinator.Main;
import org.batfish.coordinator.WorkMgrServiceV2TestBase;
import org.batfish.coordinator.WorkMgrTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ContainerResourceTest extends WorkMgrServiceV2TestBase {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Before
  public void initContainerEnvironment() throws Exception {
    WorkMgrTestUtils.initWorkManager(_folder);
  }

  private WebTarget getContainerTarget(String container) {
    return target(CoordConsts.SVC_CFG_WORK_MGR2).path(CoordConstsV2.RSC_CONTAINERS).path(container);
  }

  @Test
  public void testGetContainer() {
    String containerName = "someContainer";
    Main.getWorkMgr().initContainer(containerName, null);
    Response response = getContainerTarget(containerName).request().get();
    assertThat(response.getStatus(), equalTo(OK.getStatusCode()));
    assertThat(
        response.readEntity(new GenericType<Container>() {}).getName(), equalTo(containerName));
  }

  @Test
  public void testDeleteContainer() {
    String containerName = "someContainer";
    Main.getWorkMgr().initContainer(containerName, null);
    Response response = getContainerTarget(containerName).request().delete();
    assertThat(response.getStatus(), equalTo(NO_CONTENT.getStatusCode()));
  }

  @Test
  public void deleteNonExistingContainer() {
    Response response = getContainerTarget("nonExistingContainer").request().delete();
    assertThat(response.getStatus(), equalTo(NOT_FOUND.getStatusCode()));
  }
}
