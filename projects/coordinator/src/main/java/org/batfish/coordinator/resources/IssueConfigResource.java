package org.batfish.coordinator.resources;

import java.io.IOException;
import java.util.Optional;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.batfish.coordinator.Main;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.answers.MinorIssueConfig;

/**
 * The {@link IssueConfigResource} is a resource for servicing client API calls for Issue
 * configuration. It is a subresource of {@link IssueSettingsResource}.
 */
@Produces(MediaType.APPLICATION_JSON)
public class IssueConfigResource {

  private String _major;
  private String _minor;
  private String _network;

  public IssueConfigResource(String network, String major, String minor) {
    _network = network;
    _major = major;
    _minor = minor;
  }

  /** Deletes this network's major and minor config */
  @DELETE
  public Response delIssueConfig() {
    MajorIssueConfig majorConfig = Main.getWorkMgr().getMajorIssueConfig(_network, _major);
    Optional<MinorIssueConfig> minorConfig = majorConfig.getMinorIssueConfig(_minor);
    if (!minorConfig.isPresent()) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MajorIssueConfig result = majorConfig.delMinorIssueConfig(_minor);
    try {
      Main.getWorkMgr().putMajorIssueConfig(_network, _major, result);
      return Response.ok().build();
    } catch (IOException e) {
      throw new InternalServerErrorException("Could not delete the issue config");
    }
  }

  /** Gets {@link IssueConfigBean} for this network's major and minor config */
  @GET
  public Response getIssueConfig() {
    MajorIssueConfig majorConfig = Main.getWorkMgr().getMajorIssueConfig(_network, _major);
    Optional<MinorIssueConfig> minorConfig = majorConfig.getMinorIssueConfig(_minor);
    return minorConfig.isPresent()
        ? Response.ok(new IssueConfigBean(_major, minorConfig.get())).build()
        : Response.status(Status.NOT_FOUND).build();
  }
}
