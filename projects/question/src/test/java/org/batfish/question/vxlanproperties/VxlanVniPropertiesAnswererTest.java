package org.batfish.question.vxlanproperties;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.questions.VxlanVniPropertySpecifier.LOCAL_VTEP_IP;
import static org.batfish.datamodel.questions.VxlanVniPropertySpecifier.MULTICAST_GROUP;
import static org.batfish.datamodel.questions.VxlanVniPropertySpecifier.VLAN;
import static org.batfish.datamodel.questions.VxlanVniPropertySpecifier.VTEP_FLOOD_LIST;
import static org.batfish.datamodel.questions.VxlanVniPropertySpecifier.VXLAN_PORT;
import static org.batfish.question.vxlanproperties.VxlanVniPropertiesAnswerer.COL_NODE;
import static org.batfish.question.vxlanproperties.VxlanVniPropertiesAnswerer.COL_VNI;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MockDataPlane;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Rows;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.junit.Test;

/** Tests for {@link VxlanVniPropertiesAnswerer} */
public final class VxlanVniPropertiesAnswererTest {
  @Test
  public void testAnswer() {
    VxlanVniPropertiesAnswerer answerer =
        new VxlanVniPropertiesAnswerer(
            new VxlanVniPropertiesQuestion(null, "/.*/"),
            new VxlanVniPropertiesAnswererTest.TestBatfish());
    TableAnswerElement answer = answerer.answer();
    assertThat(
        answer.getRows(),
        equalTo(
            new Rows()
                .add(
                    Row.builder()
                        .put(COL_NODE, "hostname")
                        .put(COL_VNI, 10001)
                        .put(LOCAL_VTEP_IP, Ip.parse("1.2.3.4"))
                        .put(MULTICAST_GROUP, null)
                        .put(VLAN, 1)
                        .put(
                            VTEP_FLOOD_LIST,
                            ImmutableSet.of(Ip.parse("2.3.4.5"), Ip.parse("2.3.4.6")))
                        .put(VXLAN_PORT, 4242)
                        .build())
                .add(
                    Row.builder()
                        .put(COL_NODE, "hostname")
                        .put(COL_VNI, 10002)
                        .put(LOCAL_VTEP_IP, Ip.parse("1.2.3.4"))
                        .put(MULTICAST_GROUP, Ip.parse("227.10.1.1"))
                        .put(VLAN, 2)
                        .put(VTEP_FLOOD_LIST, null)
                        .put(VXLAN_PORT, 4789)
                        .build())
                .add(
                    Row.builder()
                        .put(COL_NODE, "minimal")
                        .put(COL_VNI, 10001)
                        .put(LOCAL_VTEP_IP, null)
                        .put(MULTICAST_GROUP, null)
                        .put(VLAN, 1)
                        .put(VTEP_FLOOD_LIST, null)
                        .put(VXLAN_PORT, 1234)
                        .build())));
  }

  @Test
  public void testSpecifyNodes() {
    VxlanVniPropertiesAnswerer answerer =
        new VxlanVniPropertiesAnswerer(
            new VxlanVniPropertiesQuestion("minimal", "/.*/"),
            new VxlanVniPropertiesAnswererTest.TestBatfish());
    TableAnswerElement answer = answerer.answer();

    // Should have only the one VNI from the specified node
    assertThat(
        answer.getRows(),
        equalTo(
            new Rows()
                .add(
                    Row.builder()
                        .put(COL_NODE, "minimal")
                        .put(COL_VNI, 10001)
                        .put(LOCAL_VTEP_IP, null)
                        .put(MULTICAST_GROUP, null)
                        .put(VLAN, 1)
                        .put(VTEP_FLOOD_LIST, null)
                        .put(VXLAN_PORT, 1234)
                        .build())));
  }

  @Test
  public void testSpecifyProperties() {
    VxlanVniPropertiesAnswerer answerer =
        new VxlanVniPropertiesAnswerer(
            new VxlanVniPropertiesQuestion(null, VLAN),
            new VxlanVniPropertiesAnswererTest.TestBatfish());
    TableAnswerElement answer = answerer.answer();

    // Should have the mandatory properties (Node, VNI) as well as the requested property: VLAN
    assertThat(
        answer.getRows(),
        equalTo(
            new Rows()
                .add(
                    Row.builder()
                        .put(COL_NODE, "hostname")
                        .put(COL_VNI, 10001)
                        .put(VLAN, 1)
                        .build())
                .add(
                    Row.builder()
                        .put(COL_NODE, "hostname")
                        .put(COL_VNI, 10002)
                        .put(VLAN, 2)
                        .build())
                .add(
                    Row.builder()
                        .put(COL_NODE, "minimal")
                        .put(COL_VNI, 10001)
                        .put(VLAN, 1)
                        .build())));
  }

  private static class TestBatfish extends IBatfishTestAdapter {

    @Override
    public SortedMap<String, Configuration> loadConfigurations() {
      Configuration conf = new Configuration("hostname", ConfigurationFormat.ARISTA);
      conf.setVrfs(ImmutableMap.of(DEFAULT_VRF_NAME, new Vrf(DEFAULT_VRF_NAME)));
      Configuration confMinimal = new Configuration("minimal", ConfigurationFormat.ARISTA);
      confMinimal.setVrfs(ImmutableMap.of(DEFAULT_VRF_NAME, new Vrf(DEFAULT_VRF_NAME)));

      return ImmutableSortedMap.of("hostname", conf, "minimal", confMinimal);
    }

    @Override
    public SortedMap<String, Configuration> loadConfigurations(NetworkSnapshot snapshot) {
      return loadConfigurations();
    }

    @Override
    public DataPlane loadDataPlane() {
      SortedMap<String, Configuration> configs = loadConfigurations();
      HashBasedTable<String, String, Set<Layer2Vni>> vnis = HashBasedTable.create();
      vnis.put(
          "hostname",
          DEFAULT_VRF_NAME,
          ImmutableSet.of(
              Layer2Vni.builder()
                  .setVni(10001)
                  .setVlan(1)
                  .setSourceAddress(Ip.parse("1.2.3.4"))
                  .setUdpPort(4242)
                  .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                  .setBumTransportIps(
                      ImmutableSortedSet.of(Ip.parse("2.3.4.5"), Ip.parse("2.3.4.6")))
                  .build(),
              Layer2Vni.builder()
                  .setVni(10002)
                  .setVlan(2)
                  .setSourceAddress(Ip.parse("1.2.3.4"))
                  .setUdpPort(4789)
                  .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
                  .setBumTransportIps(ImmutableSortedSet.of(Ip.parse("227.10.1.1")))
                  .build()));
      vnis.put(
          "minimal",
          DEFAULT_VRF_NAME,
          ImmutableSet.of(
              Layer2Vni.builder()
                  .setVni(10001)
                  .setVlan(1)
                  .setUdpPort(1234)
                  .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
                  .build()));
      return MockDataPlane.builder().setConfigs(configs).setVniSettings(vnis).build();
    }
  }
}
