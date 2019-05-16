package org.batfish.datamodel;

import static org.batfish.datamodel.matchers.DataModelMatchers.hasIsisProcess;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasReferenceBandwidth;
import static org.batfish.datamodel.matchers.IsisProcessMatchers.hasNetAddress;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasName;
import static org.batfish.datamodel.matchers.VrfMatchers.hasOspfProcess;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.ospf.OspfProcess;
import org.junit.Test;

public class VrfTest {

  @Test
  public void testJsonSerialization() throws IOException {
    Vrf vrf = new Vrf("vrf");
    vrf.setOspfProcesses(
        ImmutableSortedMap.of(
            "ospf", OspfProcess.builder().setProcessId("ospf").setReferenceBandwidth(1d).build()));
    vrf.setBgpProcess(new BgpProcess(Ip.ZERO));
    IsoAddress isoAddress = new IsoAddress("49.0001.0100.0500.5005.00");
    vrf.setIsisProcess(IsisProcess.builder().setNetAddress(isoAddress).build());

    // Clone and compare (can't use equalTo because Vrf extends ComparableStructure, so any two VRFs
    // with the same name will be equal)
    Vrf clonedVrf = BatfishObjectMapper.clone(vrf, Vrf.class);
    assertThat(clonedVrf, hasName(equalTo("vrf")));
    assertThat(clonedVrf, hasOspfProcess("ospf", hasReferenceBandwidth(1d)));
    assertThat(clonedVrf, hasBgpProcess(notNullValue()));
    assertThat(clonedVrf, hasIsisProcess(hasNetAddress(equalTo(isoAddress))));
  }
}
