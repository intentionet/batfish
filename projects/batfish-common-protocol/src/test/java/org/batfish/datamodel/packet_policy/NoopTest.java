package org.batfish.datamodel.packet_policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

/** Tests of {@link Noop} */
public class NoopTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(Noop.instance(), Noop.instance())
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    assertThat(SerializationUtils.clone(Noop.instance()), equalTo(Noop.instance()));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    assertThat(BatfishObjectMapper.clone(Noop.instance(), Noop.class), equalTo(Noop.instance()));
  }

  @Test
  public void testToString() {
    assertTrue(Noop.instance().toString().contains(Noop.class.getSimpleName()));
  }
}
