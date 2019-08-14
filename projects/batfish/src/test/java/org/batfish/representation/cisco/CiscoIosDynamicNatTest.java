package org.batfish.representation.cisco;

import com.google.common.testing.EqualsTester;
import org.batfish.representation.cisco.CiscoIosNat.RuleAction;
import org.junit.Test;

/** Tests of {@link CiscoIosDynamicNat}. */
public class CiscoIosDynamicNatTest {

  private static CiscoIosDynamicNat baseNat() {
    CiscoIosDynamicNat n1 = new CiscoIosDynamicNat();
    n1.setAction(RuleAction.SOURCE_INSIDE);
    n1.setAclName("acl");
    n1.setNatPool("pool");
    n1.setOverload(false);
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
      CiscoIosDynamicNat diffOverload = baseNat();
      diffOverload.setOverload(true);
      et.addEqualityGroup(diffOverload);
    }
    {
      CiscoIosDynamicNat diffPool = baseNat();
      diffPool.setNatPool("diffpool");
      et.addEqualityGroup(diffPool);
    }
    et.testEquals();
  }
}
