package org.batfish.coordinator;

import javax.ws.rs.core.Application;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

public class WorkMgrServiceV2TestBase extends JerseyTest {

  @Override
  public Application configure() {
    forceSet(TestProperties.CONTAINER_PORT, "0");
    return new ResourceConfig(WorkMgrServiceV2.class)
        .register(ServiceObjectMapper.class)
        .register(ExceptionMapper.class)
        .register(JacksonFeature.class)
        .register(ApiKeyAuthenticationFilter.class)
        .register(VersionCompatibilityFilter.class)
        .register(CrossDomainFilter.class);
  }

  @Override
  protected void configureClient(ClientConfig config) {
    config.register(ServiceObjectMapper.class);
  }
}
