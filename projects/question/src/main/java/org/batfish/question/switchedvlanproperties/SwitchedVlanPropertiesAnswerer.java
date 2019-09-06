package org.batfish.question.switchedvlanproperties;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Multiset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.InterfaceSpecifier;
import org.batfish.specifier.SpecifierContext;

public final class SwitchedVlanPropertiesAnswerer extends Answerer {

  @VisibleForTesting static final String COL_INTERFACES = "Interfaces";
  @VisibleForTesting static final String COL_NODE = "Node";
  @VisibleForTesting static final String COL_VLAN_ID = "VLAN_ID";
  @VisibleForTesting static final String COL_VXLAN_VNI = "VXLAN_VNI";

  private static void addVlanVnis(
      Configuration c,
      IntegerSpace vlans,
      Map<Integer, ImmutableSet.Builder<NodeInterfacePair>> switchedVlanInterfaces,
      ImmutableMap.Builder<Integer, Integer> vlanVnisBuilder) {
    c.getVrfs()
        .values()
        .forEach(
            vrf ->
                vrf.getVniSettings()
                    .values()
                    .forEach(
                        vniSettings ->
                            tryAddVlanVni(
                                vniSettings, vlans, vlanVnisBuilder, switchedVlanInterfaces)));
  }

  @VisibleForTesting
  static void computeNodeVlanProperties(
      SpecifierContext ctxt,
      Configuration c,
      InterfaceSpecifier interfacesSpecifier,
      boolean excludeShutInterfaces,
      IntegerSpace vlans,
      Map<Integer, ImmutableSet.Builder<NodeInterfacePair>> switchedVlanInterfaces,
      ImmutableMap.Builder<Integer, Integer> vlanVnisBuilder) {
    addVlanVnis(c, vlans, switchedVlanInterfaces, vlanVnisBuilder);
    Set<NodeInterfacePair> specifiedInterfaces =
        interfacesSpecifier.resolve(ImmutableSet.of(c.getHostname()), ctxt);
    for (Interface iface : c.getAllInterfaces().values()) {
      tryAddInterfaceToVlans(
          specifiedInterfaces, excludeShutInterfaces, vlans, switchedVlanInterfaces, iface);
    }
  }

  /**
   * Creates {@link ColumnMetadata}s that the answer should have.
   *
   * @return The {@link List} of {@link ColumnMetadata}s
   */
  public static List<ColumnMetadata> createColumnMetadata() {
    return ImmutableList.<ColumnMetadata>builder()
        .add(new ColumnMetadata(COL_NODE, Schema.NODE, COL_NODE, true, false))
        .add(new ColumnMetadata(COL_VLAN_ID, Schema.INTEGER, COL_VLAN_ID, true, false))
        .add(
            new ColumnMetadata(
                COL_INTERFACES,
                Schema.set(Schema.INTERFACE),
                "Switched interfaces carrying traffic for this VLAN",
                false,
                true))
        .add(
            new ColumnMetadata(
                COL_VXLAN_VNI,
                Schema.INTEGER,
                "VXLAN VNI with which this VLAN is associated",
                false,
                true))
        .build();
  }

  @VisibleForTesting
  static Row createRow(
      Map<String, ColumnMetadata> columns,
      String node,
      Integer vlan,
      Set<NodeInterfacePair> switchedVlanInterfaces,
      Map<Integer, Integer> vlanVnis) {
    return Row.builder(columns)
        .put(COL_NODE, new Node(node))
        .put(COL_VLAN_ID, vlan)
        .put(COL_INTERFACES, switchedVlanInterfaces)
        .put(COL_VXLAN_VNI, vlanVnis.get(vlan))
        .build();
  }

  /**
   * Creates a {@link TableMetadata} object from the question.
   *
   * @param question The question
   * @return The resulting {@link TableMetadata} object
   */
  @VisibleForTesting
  static TableMetadata createTableMetadata(SwitchedVlanPropertiesQuestion question) {
    String textDesc = String.format("Properties of VLAN ${%s}.", COL_VLAN_ID);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(createColumnMetadata(), textDesc);
  }

  /** Returns the name of the column that contains the value of property {@code property} */
  public static String getColumnName(String property) {
    return property;
  }

  /**
   * Gets properties of switched vlans.
   *
   * @param ctxt context in which to apply {@code interfacesSpecifier}
   * @param configurations configuration to use in extractions
   * @param nodes the set of nodes to consider
   * @param interfaceSpecifier Specifies which interfaces to consider
   * @param columns a map from column name to {@link ColumnMetadata}
   * @return A multiset of {@link Row}s where each row corresponds to a node/vlan pair and columns
   *     correspond to property values.
   */
  public static Multiset<Row> getProperties(
      SpecifierContext ctxt,
      Map<String, Configuration> configurations,
      Set<String> nodes,
      InterfaceSpecifier interfaceSpecifier,
      boolean excludeShutInterfaces,
      IntegerSpace vlans,
      Map<String, ColumnMetadata> columns) {
    Multiset<Row> rows = HashMultiset.create();
    for (String node : nodes) {
      Map<Integer, ImmutableSet.Builder<NodeInterfacePair>> switchedVlanInterfaces =
          new HashMap<>();
      ImmutableMap.Builder<Integer, Integer> vlanVnisBuilder = ImmutableMap.builder();
      computeNodeVlanProperties(
          ctxt,
          configurations.get(node),
          interfaceSpecifier,
          excludeShutInterfaces,
          vlans,
          switchedVlanInterfaces,
          vlanVnisBuilder);
      populateNodeRows(node, switchedVlanInterfaces, vlanVnisBuilder, columns, rows);
    }
    return rows;
  }

  @VisibleForTesting
  static void populateNodeRows(
      String node,
      Map<Integer, ImmutableSet.Builder<NodeInterfacePair>> switchedVlanInterfaces,
      ImmutableMap.Builder<Integer, Integer> vlanVnisBuilder,
      Map<String, ColumnMetadata> columns,
      Collection<Row> rows) {
    Map<Integer, Integer> vlanVnis = vlanVnisBuilder.build();
    switchedVlanInterfaces.forEach(
        (vlan, interfaces) ->
            rows.add(createRow(columns, node, vlan, interfaces.build(), vlanVnis)));
  }

  @VisibleForTesting
  static void tryAddInterfaceToVlan(
      Map<Integer, ImmutableSet.Builder<NodeInterfacePair>> switchedVlanInterfaces,
      NodeInterfacePair iface,
      @Nullable Integer vlan,
      IntegerSpace vlans) {
    if (vlan != null && vlans.contains(vlan)) {
      switchedVlanInterfaces.computeIfAbsent(vlan, v -> ImmutableSet.builder()).add(iface);
    }
  }

  @VisibleForTesting
  static void tryAddInterfaceToVlans(
      Set<NodeInterfacePair> specifiedInterfaces,
      boolean excludeShutInterfaces,
      IntegerSpace vlans,
      Map<Integer, Builder<NodeInterfacePair>> switchedVlanInterfaces,
      Interface iface) {
    NodeInterfacePair ifacePair = NodeInterfacePair.of(iface);
    if (!specifiedInterfaces.contains(ifacePair)
        || (excludeShutInterfaces && !iface.getActive())
        || (iface.getInterfaceType() != InterfaceType.VLAN
            && !Boolean.TRUE.equals(iface.getSwitchport()))) {
      return;
    }
    if (iface.getInterfaceType() == InterfaceType.VLAN) {
      // Add VLAN associated with IRB-type interface
      tryAddInterfaceToVlan(switchedVlanInterfaces, ifacePair, iface.getVlan(), vlans);
    } else if (iface.getSwitchportMode() == SwitchportMode.ACCESS) {
      // Add access VLAN when in ACCESS mode
      tryAddInterfaceToVlan(switchedVlanInterfaces, ifacePair, iface.getAccessVlan(), vlans);
    } else if (iface.getSwitchportMode() == SwitchportMode.TRUNK) {
      // Add allowed VLANs when in TRUNK mode
      iface.getAllowedVlans().stream()
          .forEach(
              allowedVlan ->
                  tryAddInterfaceToVlan(switchedVlanInterfaces, ifacePair, allowedVlan, vlans));
    }
  }

  @VisibleForTesting
  static void tryAddVlanVni(
      VniSettings vniSettings,
      IntegerSpace vlans,
      ImmutableMap.Builder<Integer, Integer> vlanVnisBuilder,
      Map<Integer, ImmutableSet.Builder<NodeInterfacePair>> switchedVlanInterfaces) {
    if (vniSettings.getVlan() != null && vlans.contains(vniSettings.getVlan())) {
      int vlan = vniSettings.getVlan();
      vlanVnisBuilder.put(vlan, vniSettings.getVni());
      switchedVlanInterfaces.computeIfAbsent(vlan, v -> ImmutableSet.builder());
    }
  }

  public SwitchedVlanPropertiesAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public TableAnswerElement answer() {
    SwitchedVlanPropertiesQuestion question = (SwitchedVlanPropertiesQuestion) _question;
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Set<String> nodes = question.getNodesSpecifier().resolve(_batfish.specifierContext());
    TableMetadata tableMetadata = createTableMetadata(question);
    TableAnswerElement answer = new TableAnswerElement(tableMetadata);
    Multiset<Row> propertyRows =
        getProperties(
            _batfish.specifierContext(),
            configurations,
            nodes,
            question.getInterfacesSpecifier(),
            question.getExcludeShutInterfaces(),
            question.getVlans(),
            tableMetadata.toColumnMap());
    answer.postProcessAnswer(question, propertyRows);
    return answer;
  }
}
