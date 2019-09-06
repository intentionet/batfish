package org.batfish.datamodel.flow;

import com.google.common.testing.EqualsTester;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

/** Test of {@link ForwardOutInterface}. */
@ParametersAreNonnullByDefault
public final class ForwardOutInterfaceTest {

  @Test
  public void testEquals() {
    ForwardOutInterface f = new ForwardOutInterface("a", null);
    new EqualsTester()
        .addEqualityGroup(new Object())
        .addEqualityGroup(f, f, new ForwardOutInterface("a", null))
        .addEqualityGroup(new ForwardOutInterface("b", null))
        .addEqualityGroup(new ForwardOutInterface("a", NodeInterfacePair.of("a", "a")))
        .testEquals();
  }
}
