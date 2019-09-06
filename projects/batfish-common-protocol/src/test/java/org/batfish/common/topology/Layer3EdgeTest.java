package org.batfish.common.topology;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

/** Test for {@link Layer3Edge} */
public class Layer3EdgeTest {
  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new Layer3Edge(
                NodeInterfacePair.of("node1", "interface1"),
                NodeInterfacePair.of("node2", "interface2"),
                ImmutableSortedSet.of(ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 32)),
                ImmutableSortedSet.of(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 32))),
            new Layer3Edge(
                NodeInterfacePair.of("node1", "interface1"),
                NodeInterfacePair.of("node2", "interface2"),
                ImmutableSortedSet.of(ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 32)),
                ImmutableSortedSet.of(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 32))))
        .addEqualityGroup(
            new Layer3Edge(
                NodeInterfacePair.of("diffnode1", "diffinterface1"),
                NodeInterfacePair.of("diffnode2", "diffinterface2"),
                ImmutableSortedSet.of(ConcreteInterfaceAddress.create(Ip.parse("3.3.3.3"), 32)),
                ImmutableSortedSet.of(ConcreteInterfaceAddress.create(Ip.parse("4.4.4.4"), 32))))
        .testEquals();
  }

  @Test
  public void testJsonSerialization() throws IOException {
    Layer3Edge layer3Edge =
        new Layer3Edge(
            NodeInterfacePair.of("node1", "interface1"),
            NodeInterfacePair.of("node2", "interface2"),
            ImmutableSortedSet.of(ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 32)),
            ImmutableSortedSet.of(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 32)));

    assertThat(BatfishObjectMapper.clone(layer3Edge, Layer3Edge.class), equalTo(layer3Edge));
  }
}
