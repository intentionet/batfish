package org.batfish.datamodel.acl;

import static org.batfish.datamodel.IpAccessListLine.accepting;
import static org.batfish.datamodel.IpAccessListLine.rejecting;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.datamodel.acl.DifferentialIpAccessList.RENAMER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.Prefix;
import org.junit.Test;

public class DifferentialIpAccessListTest {
  private static HeaderSpace createHeaderSpace(String ipSpaceName) {
    return HeaderSpace.builder().setDstIps(new IpSpaceReference(ipSpaceName)).build();
  }

  private static IpAccessList createAcl(
      String aclName, String aclReferenceName, String ipSpaceName) {
    return IpAccessList.builder()
        .setName(aclName)
        .setLines(
            ImmutableList.of(
                accepting(permittedByAcl(aclReferenceName)),
                accepting(new MatchHeaderSpace(createHeaderSpace(ipSpaceName)))))
        .build();
  }

  @Test
  public void testCreate() {
    String denyAclName = "deny acl";
    String denyAclReferenceName = "deny named acl";
    String denyIpSpace = "deny named ip space";

    String renamedDenyAclName = RENAMER.apply(denyAclName);
    String renamedDenyAclReferenceName = RENAMER.apply(denyAclReferenceName);
    String renamedDenyIpSpace = RENAMER.apply(denyIpSpace);

    IpAccessList denyAcl = createAcl(denyAclName, denyAclReferenceName, denyIpSpace);
    // denyAcl has a single named ACL that recursively references itself and also a named IpSpace
    Map<String, IpAccessList> denyNamedAcls =
        ImmutableMap.of(
            denyAclReferenceName,
            createAcl(denyAclReferenceName, denyAclReferenceName, denyIpSpace));
    // denyAcl has a single named IpSpace that recursively references itself
    Map<String, IpSpace> denyNamedIpSpaces =
        ImmutableMap.of(denyIpSpace, new IpSpaceReference(denyIpSpace));

    String permitAclName = "permit acl";
    String permitAclReferenceName = "permit named acl";
    String permitNamedIpSpace = "permit named ip space";
    List<IpAccessListLine> permitAclLines =
        ImmutableList.of(accepting(AclLineMatchExprs.matchDst(Prefix.parse("1.1.1.0/24"))));
    IpAccessList permitAcl =
        IpAccessList.builder().setName(permitAclName).setLines(permitAclLines).build();
    IpAccessList permitAclReferenceAcl =
        createAcl(permitAclReferenceName, permitAclReferenceName, permitNamedIpSpace);
    Map<String, IpAccessList> permitNamedAcls =
        ImmutableMap.of(permitAclReferenceName, permitAclReferenceAcl);
    Map<String, IpSpace> permitNamedIpSpaces =
        ImmutableMap.of(permitNamedIpSpace, new IpSpaceReference(permitNamedIpSpace));

    DifferentialIpAccessList differential =
        DifferentialIpAccessList.create(
            denyAcl,
            denyNamedAcls,
            denyNamedIpSpaces,
            permitAcl,
            permitNamedAcls,
            permitNamedIpSpaces);

    IpAccessList differentialAcl =
        IpAccessList.builder()
            .setName(DifferentialIpAccessList.DIFFERENTIAL_ACL_NAME)
            .setLines(
                ImmutableList.<IpAccessListLine>builder()
                    .add(rejecting(permittedByAcl(renamedDenyAclName)))
                    .addAll(permitAclLines)
                    .build())
            .build();

    assertThat(differential.getAcl(), equalTo(differentialAcl));

    /*
     * Test named ACLs
     */
    assertThat(differential.getNamedAcls().entrySet(), hasSize(3));
    // the deny ACL itself is present and renamed
    assertThat(
        differential.getNamedAcls(),
        hasEntry(
            renamedDenyAclName,
            createAcl(renamedDenyAclName, renamedDenyAclReferenceName, renamedDenyIpSpace)));
    // denyNamedAcls are present and renamed
    assertThat(
        differential.getNamedAcls(),
        hasEntry(
            renamedDenyAclReferenceName,
            createAcl(
                renamedDenyAclReferenceName, renamedDenyAclReferenceName, renamedDenyIpSpace)));
    // permitNamedAcls are present and not renamed
    assertThat(
        differential.getNamedAcls(), hasEntry(permitAclReferenceName, permitAclReferenceAcl));

    /*
     * Test named IpSpaces
     */
    assertThat(differential.getNamedIpSpaces().entrySet(), hasSize(2));
    // denyNamedIpSpaces are present and renamed
    assertThat(
        differential.getNamedIpSpaces(),
        hasEntry(renamedDenyIpSpace, new IpSpaceReference(renamedDenyIpSpace)));
    // permitNamedIpSpaces are present and not renamed
    assertThat(
        differential.getNamedIpSpaces(),
        hasEntry(permitNamedIpSpace, new IpSpaceReference(permitNamedIpSpace)));
  }
}
