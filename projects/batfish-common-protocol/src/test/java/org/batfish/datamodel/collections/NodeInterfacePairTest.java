package org.batfish.datamodel.collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

public class NodeInterfacePairTest {
  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            NodeInterfacePair.of("host", "iface"), NodeInterfacePair.of("host", "iface"))
        .addEqualityGroup(NodeInterfacePair.of("otherHost", "iface"))
        .addEqualityGroup(NodeInterfacePair.of("host", "otherIface"))
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testCompareTo() {
    List<NodeInterfacePair> ordered =
        ImmutableList.of(
            NodeInterfacePair.of("a", "a"),
            NodeInterfacePair.of("a", "b"),
            NodeInterfacePair.of("b", "a"),
            NodeInterfacePair.of("b", "b"));
    for (int i = 0; i < ordered.size(); i++) {
      for (int j = 0; j < ordered.size(); j++) {
        assertThat(
            Integer.signum(ordered.get(i).compareTo(ordered.get(j))),
            equalTo(Integer.signum(i - j)));
      }
    }
  }

  @Test
  public void testJavaSerialization() {
    NodeInterfacePair nip = NodeInterfacePair.of("host", "iface");
    assertThat(SerializationUtils.clone(nip), equalTo(nip));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    NodeInterfacePair nip = NodeInterfacePair.of("host", "iface");
    assertThat(BatfishObjectMapper.clone(nip, NodeInterfacePair.class), equalTo(nip));
  }
}
