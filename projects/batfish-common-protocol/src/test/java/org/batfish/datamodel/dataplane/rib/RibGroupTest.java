package org.batfish.datamodel.dataplane.rib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link org.batfish.datamodel.dataplane.rib.RibGroup} */
@RunWith(JUnit4.class)
public class RibGroupTest {

  @Test
  public void testEquals() {
    RibId rib1 = new RibId("hostname", "vrfname", "ribname");
    RibId rib2 = new RibId("hostname", "vrfname", "secondaryRib");
    RibId exportRib = new RibId("hostname", "vrfname", "exportRib");
    new EqualsTester()
        .addEqualityGroup(
            new RibGroup("name", ImmutableList.of(rib1), "policy1", exportRib),
            new RibGroup("name", ImmutableList.of(rib1), "policy1", exportRib))
        .addEqualityGroup(
            new RibGroup("differentName", ImmutableList.of(rib1), "policy1", exportRib))
        .addEqualityGroup(new RibGroup("name", ImmutableList.of(rib2), "policy2", exportRib))
        .addEqualityGroup(new RibGroup("name", ImmutableList.of(rib2), "policy1", rib1))
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    RibId rib1 = new RibId("hostname", "vrfname", "ribname");
    RibId exportRib = new RibId("hostname", "vrfname", "exportRib");
    RibGroup rg = new RibGroup("name", ImmutableList.of(rib1), "policy1", exportRib);
    assertThat(SerializationUtils.clone(rg), equalTo(rg));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    RibId rib1 = new RibId("hostname", "vrfname", "ribname");
    RibId exportRib = new RibId("hostname", "vrfname", "exportRib");
    RibGroup rg = new RibGroup("name", ImmutableList.of(rib1), "policy1", exportRib);
    assertThat(BatfishObjectMapper.clone(rg, RibGroup.class), equalTo(rg));
  }
}
