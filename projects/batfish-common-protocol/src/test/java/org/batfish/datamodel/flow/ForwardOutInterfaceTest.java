package org.batfish.datamodel.flow;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import com.google.common.testing.EqualsTester;
import java.io.IOException;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.util.BatfishObjectMapper;
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

  @Test
  public void testSerialization() throws IOException {
    ForwardOutInterface f = new ForwardOutInterface("a", null);
    SessionAction castedClone = BatfishObjectMapper.clone(f, SessionAction.class);
    assertThat(castedClone, instanceOf(ForwardOutInterface.class));

    ForwardOutInterface clone = (ForwardOutInterface) castedClone;
    assertThat(clone, equalTo(f));

    f = new ForwardOutInterface("b", NodeInterfacePair.of("a", "b"));
    clone = BatfishObjectMapper.clone(f, ForwardOutInterface.class);
    assertThat(clone, equalTo(f));
  }
}
