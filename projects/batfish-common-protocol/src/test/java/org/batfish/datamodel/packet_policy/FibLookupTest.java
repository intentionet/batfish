package org.batfish.datamodel.packet_policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

/** Tests of {@link FibLookup} */
public class FibLookupTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(new FibLookup("name"), new FibLookup("name"))
        .addEqualityGroup(new FibLookup("otherName"))
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    FibLookup fl = new FibLookup("name");
    assertThat(SerializationUtils.clone(fl), equalTo(fl));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    FibLookup fl = new FibLookup("name");
    assertThat(BatfishObjectMapper.clone(fl, FibLookup.class), equalTo(fl));
  }

  @Test
  public void testToString() {
    FibLookup fl = new FibLookup("xxxxxx");
    assertTrue(fl.toString().contains(fl.getClass().getSimpleName()));
    assertTrue(fl.toString().contains("vrfName"));
    assertTrue(fl.toString().contains(fl.getVrfName()));
  }
}
