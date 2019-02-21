package org.batfish.datamodel.packet_policy;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

/** Tests of {@link If} */
public class IfTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup()
        .addEqualityGroup()
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    If ifExpr = new If(Noop.instance(), ImmutableList.of(Drop.instance()));
    assertThat(SerializationUtils.clone(ifExpr), equalTo(ifExpr));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    If ifExpr = new If(Noop.instance(), ImmutableList.of(Drop.instance()));
    assertThat(BatfishObjectMapper.clone(ifExpr, If.class), equalTo(ifExpr));
  }

  @Test
  public void testToString() {
    If ifExpr = new If(Noop.instance(), ImmutableList.of(Drop.instance()));
    assertTrue(ifExpr.toString().contains(ifExpr.getClass().getSimpleName()));
  }
}
