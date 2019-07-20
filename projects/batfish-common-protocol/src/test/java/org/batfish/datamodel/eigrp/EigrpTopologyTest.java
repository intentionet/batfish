package org.batfish.datamodel.eigrp;

import static org.junit.Assert.assertEquals;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

/** Test of {@link EigrpTopology}. */
@ParametersAreNonnullByDefault
public final class EigrpTopologyTest {

  private static @Nonnull EigrpTopology nonTrivialTopology() {
    MutableNetwork<EigrpNeighborConfigId, EigrpEdge> network =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();
    EigrpNeighborConfigId n1 = new EigrpNeighborConfigId("a", "b", "c");
    EigrpNeighborConfigId n2 = new EigrpNeighborConfigId("d", "e", "f");
    network.addEdge(n1, n2, new EigrpEdge(n1, n2));
    return new EigrpTopology(network);
  }

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(new Object())
        .addEqualityGroup(
            EigrpTopology.EMPTY,
            EigrpTopology.EMPTY,
            new EigrpTopology(
                NetworkBuilder.directed()
                    .allowsParallelEdges(false)
                    .allowsSelfLoops(false)
                    .build()))
        .addEqualityGroup(nonTrivialTopology())
        .testEquals();
  }

  @Test
  public void testJacksonSerialization() throws IOException {
    assertEquals(
        nonTrivialTopology(), BatfishObjectMapper.clone(nonTrivialTopology(), EigrpTopology.class));
  }
}
