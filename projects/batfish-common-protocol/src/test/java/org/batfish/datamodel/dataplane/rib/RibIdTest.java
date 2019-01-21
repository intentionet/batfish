package org.batfish.datamodel.dataplane.rib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link org.batfish.datamodel.dataplane.rib.RibId} */
@RunWith(JUnit4.class)
public class RibIdTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(new RibId("h", "v", "r"), new RibId("h", "v", "r"))
        .addEqualityGroup(new RibId("h1", "v", "r"))
        .addEqualityGroup(new RibId("h", "v1", "r"))
        .addEqualityGroup(new RibId("h", "v", "r1"))
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    RibId ribId = new RibId("h", "v", "r");
    assertThat(SerializationUtils.clone(ribId), equalTo(ribId));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    RibId ribId = new RibId("h", "v", "r");
    assertThat(BatfishObjectMapper.clone(ribId, RibId.class), equalTo(ribId));
  }
}
