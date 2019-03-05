package org.batfish.question.ospfproperties;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.NameNodeSpecifier;
import org.junit.Test;

public class OspfPropertiesAnswererTest {

  @Test
  public void getProperties() {
    OspfProcess ospf1 = OspfProcess.builder().setReferenceBandwidth(1e8).build();
    ospf1.setExportPolicy("my-policy");
    ospf1.setReferenceBandwidth(42.0);
    ospf1.setProcessId("uber-proc");

    Vrf vrf1 = new Vrf("vrf1");
    vrf1.setOspfProcess(ospf1);

    Configuration conf1 = new Configuration("node1", ConfigurationFormat.CISCO_IOS);
    conf1.setVrfs(ImmutableMap.of("vrf1", vrf1));

    String property1 = OspfPropertySpecifier.EXPORT_POLICY;
    String property2 = OspfPropertySpecifier.REFERENCE_BANDWIDTH;

    OspfPropertiesQuestion question =
        new OspfPropertiesQuestion(null, new OspfPropertySpecifier(property1 + "|" + property2));

    TableMetadata metadata = OspfPropertiesAnswerer.createTableMetadata(question);

    MockSpecifierContext ctxt =
        MockSpecifierContext.builder().setConfigs(ImmutableMap.of("node1", conf1)).build();
    Multiset<Row> propertyRows =
        OspfPropertiesAnswerer.getProperties(
            question.getProperties(), ctxt, new NameNodeSpecifier("node1"), metadata.toColumnMap());

    // we should have exactly one row1 with two properties
    Row expectedRow =
        Row.builder()
            .put(OspfPropertiesAnswerer.COL_NODE, new Node("node1"))
            .put(OspfPropertiesAnswerer.COL_VRF, "vrf1")
            .put(OspfPropertiesAnswerer.COL_PROCESS_ID, "uber-proc")
            .put(property2, 42.0)
            .put(property1, "my-policy")
            .build();

    assertThat(propertyRows, equalTo(ImmutableMultiset.of(expectedRow)));
  }
}
