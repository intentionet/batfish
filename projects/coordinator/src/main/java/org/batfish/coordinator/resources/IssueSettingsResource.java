package org.batfish.coordinator.resources;

import java.io.IOException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.batfish.common.BatfishLogger;
import org.batfish.coordinator.Main;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.answers.MinorIssueConfig;

/**
 * The {@link IssueSettingsResource} is a resource for servicing client API calls for Issue
 * settings. It is a subresource of {@link NetworkSettingsResource}.
 */
@Produces(MediaType.APPLICATION_JSON)
public class IssueSettingsResource {

  private BatfishLogger _logger = Main.getLogger();
  private String _network;

  public IssueSettingsResource(String network) {
    _network = network;
  }

  /** Adds or updates a new {@link MinorIssueConfig} to the network's issue settings */
  @POST
  public Response addIssueConfig(IssueConfigBean bean) {
    _logger.infof("WMS2: addIssueConfig '%s'\n", _network);
    if (bean.major == null) {
      throw new BadRequestException("IssueConfigBean must have a major type");
    }
    if (bean.minor == null) {
      throw new BadRequestException("IssueConfigBean must have a minor type");
    }
    MajorIssueConfig issueConfig = Main.getWorkMgr().getMajorIssueConfig(_network, bean.major);
    MajorIssueConfig updatedConfig = issueConfig.put(bean.toMinorIssueConfig());
    try {
      Main.getWorkMgr().putMajorIssueConfig(_network, bean.major, updatedConfig);
      return Response.ok().build();
    } catch (IOException e) {
      throw new InternalServerErrorException("Could not add the issue config");
    }
  }

  /** Relocate the request to {@link ReferenceBookResource}. */
  @Path("/{major}/{minor}")
  public IssueConfigResource getIssueConfigResource(
      @PathParam("major") String major, @PathParam("minor") String minor) {
    return new IssueConfigResource(_network, major, minor);
  }

  /** Nothing to send today for a plan get call on this resource */
  @GET
  public Response getIssueSettings() {
    return Response.noContent().build();
  }
}
