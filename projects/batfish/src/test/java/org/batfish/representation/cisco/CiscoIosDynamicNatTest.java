package org.batfish.representation.cisco;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.util.List;
import org.batfish.representation.cisco.CiscoIosNat.RuleAction;
import org.junit.Test;

/** Tests of {@link CiscoIosDynamicNat}. */
public class CiscoIosDynamicNatTest {

  private static CiscoIosDynamicNat baseNat() {
    CiscoIosDynamicNat n1 = new CiscoIosDynamicNat();
    n1.setAction(RuleAction.SOURCE_INSIDE);
    n1.setAclName("acl");
    n1.setNatPool("pool");
    return new CiscoIosDynamicNat();
  }

  @Test
  public void testEquals() {
    EqualsTester et =
        new EqualsTester().addEqualityGroup(Boolean.FALSE).addEqualityGroup(baseNat(), baseNat());
    {
      CiscoIosDynamicNat diffAction = baseNat();
      diffAction.setAction(RuleAction.DESTINATION_INSIDE);
      et.addEqualityGroup(diffAction);
    }
    {
      CiscoIosDynamicNat diffName = baseNat();
      diffName.setAclName("diffacl");
      et.addEqualityGroup(diffName);
    }
    {
      CiscoIosDynamicNat diffPool = baseNat();
      diffPool.setNatPool("diffpool");
      et.addEqualityGroup(diffPool);
    }
    {
      CiscoIosDynamicNat ifaceNat = baseNat();
      ifaceNat.setNatPool(null);
      ifaceNat.setInterface("iface");
      et.addEqualityGroup(ifaceNat);
    }
    {
      CiscoIosDynamicNat diffIface = baseNat();
      diffIface.setNatPool(null);
      diffIface.setInterface("diffIface");
      et.addEqualityGroup(diffIface);
    }
    et.testEquals();
  }

  @Test
  public void testNatCompare() {
    // If both NATs have null ACLs, compare should give 0
    assertThat(new CiscoIosDynamicNat().compareTo(new CiscoIosDynamicNat()), equalTo(0));

    List<CiscoIosDynamicNat> ordered =
        ImmutableList.of(
            // Numeric ACLs come first, sorted numerically
            natWithAcl("1"),
            natWithAcl("3"),
            natWithAcl("20"),
            // Other ACLs are sorted lexicographically, case sensitive
            natWithAcl("Z"),
            natWithAcl("a"),
            natWithAcl("b"),
            new CiscoIosDynamicNat());
    for (int i = 0; i < ordered.size() - 1; i++) {
      for (int j = i + 1; j < ordered.size(); j++) {
        assertThat(ordered.get(i).compareTo(ordered.get(j)), lessThan(0));
        assertThat(ordered.get(j).compareTo(ordered.get(i)), greaterThan(0));
      }
    }
  }

  private static CiscoIosDynamicNat natWithAcl(String aclName) {
    CiscoIosDynamicNat nat = new CiscoIosDynamicNat();
    nat.setAclName(aclName);
    return nat;
  }
}
