package org.batfish.datamodel.packet_policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

/** Tests of {@link Drop} */
public class DropTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(Drop.instance(), Drop.instance())
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    assertThat(SerializationUtils.clone(Drop.instance()), equalTo(Drop.instance()));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    assertThat(BatfishObjectMapper.clone(Drop.instance(), Drop.class), equalTo(Drop.instance()));
  }

  @Test
  public void testToString() {
    assertTrue(Drop.instance().toString().contains(Drop.class.getSimpleName()));
  }
}
