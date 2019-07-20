package org.batfish.datamodel.eigrp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

/** Tests of {@link EigrpNeighborConfigId} */
public class EigrpNeighborConfigIdTest {
  @Test
  public void testEquals() {
    EigrpNeighborConfigId ei = new EigrpNeighborConfigId("h", "i", "v");
    new EqualsTester()
        .addEqualityGroup(ei, ei, new EigrpNeighborConfigId("h", "i", "v"))
        .addEqualityGroup(new EigrpNeighborConfigId("h1", "i", "v"))
        .addEqualityGroup(new EigrpNeighborConfigId("h", "i1", "v"))
        .addEqualityGroup(new EigrpNeighborConfigId("h", "i", "v1"))
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    EigrpNeighborConfigId ei = new EigrpNeighborConfigId("h", "i", "v");
    assertThat(SerializationUtils.clone(ei), equalTo(ei));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    EigrpNeighborConfigId ei = new EigrpNeighborConfigId("h", "i", "v");
    assertThat(BatfishObjectMapper.clone(ei, EigrpNeighborConfigId.class), equalTo(ei));
  }
}
