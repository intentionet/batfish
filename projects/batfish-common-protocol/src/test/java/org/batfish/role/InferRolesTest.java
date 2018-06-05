package org.batfish.role;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Topology;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link InferRoles}. */
@RunWith(JUnit4.class)
public class InferRolesTest {
  private static final Set<String> EXAMPLE_NODES =
      ImmutableSet.of(
          "as1border1",
          "as1border2",
          "as1core1",
          "as2border1",
          "as2border2",
          "as2core1",
          "as2core2",
          "as2dept1",
          "as2dist1",
          "as2dist2",
          "as3border1",
          "as3border2",
          "as3core1",
          "host1",
          "host2");

  private static final Topology EXAMPLE_TOPOLOGY =
      new Topology(
          ImmutableSortedSet.of(
              new Edge("as1border1", "GigabitEthernet0/0", "as1core1", "GigabitEthernet1/0"),
              new Edge("as1border1", "GigabitEthernet1/0", "as2border1", "GigabitEthernet0/0"),
              new Edge("as1border2", "GigabitEthernet0/0", "as3border2", "GigabitEthernet0/0"),
              new Edge("as1border2", "GigabitEthernet1/0", "as1core1", "GigabitEthernet0/0"),
              new Edge("as1core1", "GigabitEthernet0/0", "as1border2", "GigabitEthernet1/0"),
              new Edge("as1core1", "GigabitEthernet1/0", "as1border1", "GigabitEthernet0/0"),
              new Edge("as2border1", "GigabitEthernet0/0", "as1border1", "GigabitEthernet1/0"),
              new Edge("as2border1", "GigabitEthernet1/0", "as2core1", "GigabitEthernet0/0"),
              new Edge("as2border1", "GigabitEthernet2/0", "as2core2", "GigabitEthernet1/0"),
              new Edge("as2border2", "GigabitEthernet0/0", "as3border1", "GigabitEthernet1/0"),
              new Edge("as2border2", "GigabitEthernet1/0", "as2core2", "GigabitEthernet0/0"),
              new Edge("as2border2", "GigabitEthernet2/0", "as2core1", "GigabitEthernet1/0"),
              new Edge("as2core1", "GigabitEthernet0/0", "as2border1", "GigabitEthernet1/0"),
              new Edge("as2core1", "GigabitEthernet1/0", "as2border2", "GigabitEthernet2/0"),
              new Edge("as2core1", "GigabitEthernet2/0", "as2dist1", "GigabitEthernet0/0"),
              new Edge("as2core1", "GigabitEthernet3/0", "as2dist2", "GigabitEthernet1/0"),
              new Edge("as2core2", "GigabitEthernet0/0", "as2border2", "GigabitEthernet1/0"),
              new Edge("as2core2", "GigabitEthernet1/0", "as2border1", "GigabitEthernet2/0"),
              new Edge("as2core2", "GigabitEthernet2/0", "as2dist2", "GigabitEthernet0/0"),
              new Edge("as2core2", "GigabitEthernet3/0", "as2dist1", "GigabitEthernet1/0"),
              new Edge("as2dept1", "GigabitEthernet0/0", "as2dist1", "GigabitEthernet2/0"),
              new Edge("as2dept1", "GigabitEthernet1/0", "as2dist2", "GigabitEthernet2/0"),
              new Edge("as2dept1", "GigabitEthernet2/0", "host1", "eth0"),
              new Edge("as2dept1", "GigabitEthernet3/0", "host2", "eth0"),
              new Edge("as2dist1", "GigabitEthernet0/0", "as2core1", "GigabitEthernet2/0"),
              new Edge("as2dist1", "GigabitEthernet1/0", "as2core2", "GigabitEthernet3/0"),
              new Edge("as2dist1", "GigabitEthernet2/0", "as2dept1", "GigabitEthernet0/0"),
              new Edge("as2dist2", "GigabitEthernet0/0", "as2core2", "GigabitEthernet2/0"),
              new Edge("as2dist2", "GigabitEthernet1/0", "as2core1", "GigabitEthernet3/0"),
              new Edge("as2dist2", "GigabitEthernet2/0", "as2dept1", "GigabitEthernet1/0"),
              new Edge("as3border1", "GigabitEthernet0/0", "as3core1", "GigabitEthernet1/0"),
              new Edge("as3border1", "GigabitEthernet1/0", "as2border2", "GigabitEthernet0/0"),
              new Edge("as3border2", "GigabitEthernet0/0", "as1border2", "GigabitEthernet0/0"),
              new Edge("as3border2", "GigabitEthernet1/0", "as3core1", "GigabitEthernet0/0"),
              new Edge("as3core1", "GigabitEthernet0/0", "as3border2", "GigabitEthernet1/0"),
              new Edge("as3core1", "GigabitEthernet1/0", "as3border1", "GigabitEthernet0/0"),
              new Edge("as3core1", "GigabitEthernet2/0", "as3core1", "GigabitEthernet3/0"),
              new Edge("as3core1", "GigabitEthernet3/0", "as3core1", "GigabitEthernet2/0"),
              new Edge("host1", "eth0", "as2dept1", "GigabitEthernet2/0"),
              new Edge("host2", "eth0", "as2dept1", "GigabitEthernet3/0")));

  private static Set<String> filterSet(Set<String> nodes, Predicate<String> filter) {
    return nodes.stream().filter(filter).collect(ImmutableSet.toImmutableSet());
  }

  @Test
  public void inferRolesOnExampleTopology() throws JsonProcessingException {
    SortedSet<NodeRoleDimension> roles =
        new InferRoles(EXAMPLE_NODES, EXAMPLE_TOPOLOGY).inferRoles();

    // TODO: this can fail occasionally.
    assertThat(BatfishObjectMapper.writePrettyString(roles), roles, hasSize(2));

    NodeRoleDimension d1 = roles.first();
    assertThat(
        d1.createRoleNodesMap(EXAMPLE_NODES),
        equalTo(
            ImmutableMap.of(
                "border",
                filterSet(EXAMPLE_NODES, s -> s.contains("border")),
                "core",
                filterSet(EXAMPLE_NODES, s -> s.contains("core")),
                "dept",
                filterSet(EXAMPLE_NODES, s -> s.contains("dept")),
                "dist",
                filterSet(EXAMPLE_NODES, s -> s.contains("dist")))));
    // TODO: why is host not here?

    NodeRoleDimension d2 = roles.last();
    assertThat(
        d2.createRoleNodesMap(EXAMPLE_NODES),
        equalTo(ImmutableMap.of("as", filterSet(EXAMPLE_NODES, s -> s.startsWith("as")))));
    // TODO: or here?

  }
}
