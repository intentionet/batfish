package org.batfish.datamodel.acl;

import static org.batfish.datamodel.IpAccessListLine.acceptingHeaderSpace;
import static org.batfish.datamodel.IpAccessListLine.rejectingHeaderSpace;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.junit.Before;
import org.junit.Test;

public class CanonicalAclTest {

  private Configuration _c;
  private Configuration _c2;

  private IpAccessList.Builder _aclb;
  private IpAccessList.Builder _aclb2;

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _c = cb.build();
    _c2 = cb.build();
    _aclb = nf.aclBuilder().setOwner(_c);
    _aclb2 = nf.aclBuilder().setOwner(_c2);
  }

  @Test
  public void testIdenticalAclsWithIdenticalDependenciesEqual() {
    // acl1 and acl2 are identical acls on different configs that both reference referencedAcl
    IpAccessList acl1 =
        _aclb
            .setName("acl1")
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.accepting()
                        .setMatchCondition(new PermittedByAcl("referencedAcl"))
                        .build()))
            .build();
    IpAccessList acl2 =
        _aclb2
            .setName("acl2")
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.accepting()
                        .setMatchCondition(new PermittedByAcl("referencedAcl"))
                        .build()))
            .build();
    IpAccessList referencedAcl1 =
        _aclb
            .setName("referencedAcl")
            .setLines(
                ImmutableList.of(
                    rejectingHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace())
                            .build())))
            .build();
    IpAccessList referencedAcl2 =
        _aclb2
            .setName("referencedAcl")
            .setLines(
                ImmutableList.of(
                    rejectingHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace())
                            .build())))
            .build();

    // Canonical acls for acl1 and acl2 should match
    CanonicalAcl canonicalAcl1 =
        new CanonicalAcl(
            "acl1", acl1, ImmutableMap.of("referencedAcl", referencedAcl1), _c.getName());
    CanonicalAcl canonicalAcl2 =
        new CanonicalAcl(
            "acl2", acl2, ImmutableMap.of("referencedAcl", referencedAcl2), _c2.getName());

    assertThat(canonicalAcl1.equals(canonicalAcl2), equalTo(true));
  }

  @Test
  public void testDifferentAclsNotEqual() {
    // acl1 and acl2 are different.
    IpAccessList acl1 =
        _aclb
            .setName("acl1")
            .setLines(
                ImmutableList.of(
                    acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(Prefix.parse("10.10.10.10/8").toIpSpace())
                            .build())))
            .build();
    IpAccessList acl2 =
        _aclb2
            .setName("acl2")
            .setLines(
                ImmutableList.of(
                    rejectingHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace())
                            .build())))
            .build();

    // Canonical acls for acl1 and acl2 shouldn't match since they are different
    CanonicalAcl canonicalAcl1 = new CanonicalAcl("acl1", acl1, ImmutableMap.of(), _c.getName());
    CanonicalAcl canonicalAcl2 = new CanonicalAcl("acl2", acl2, ImmutableMap.of(), _c2.getName());

    assertThat(canonicalAcl1.equals(canonicalAcl2), equalTo(false));
  }

  @Test
  public void testAclsWithDifferentDependenciesNotEqual() {
    // acl1 and acl2 are identical acls on different configs that both reference referencedAcl, but
    // the two versions of referencedAcl are different
    IpAccessList acl1 =
        _aclb
            .setName("acl1")
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.accepting()
                        .setMatchCondition(new PermittedByAcl("referencedAcl"))
                        .build()))
            .build();
    IpAccessList acl2 =
        _aclb2
            .setName("acl2")
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.accepting()
                        .setMatchCondition(new PermittedByAcl("referencedAcl"))
                        .build()))
            .build();
    IpAccessList referencedAcl1 =
        _aclb
            .setName("referencedAcl")
            .setLines(
                ImmutableList.of(
                    rejectingHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace())
                            .build())))
            .build();
    IpAccessList referencedAcl2 =
        _aclb2
            .setName("referencedAcl")
            .setLines(
                ImmutableList.of(
                    acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(Prefix.parse("10.10.10.10/8").toIpSpace())
                            .build())))
            .build();

    // Canonical acls for acl1 and acl2 shouldn't match since references are different
    CanonicalAcl canonicalAcl1 =
        new CanonicalAcl(
            "acl1", acl1, ImmutableMap.of("referencedAcl", referencedAcl1), _c.getName());
    CanonicalAcl canonicalAcl2 =
        new CanonicalAcl(
            "acl2", acl2, ImmutableMap.of("referencedAcl", referencedAcl2), _c2.getName());

    assertThat(canonicalAcl1.equals(canonicalAcl2), equalTo(false));
  }
}
