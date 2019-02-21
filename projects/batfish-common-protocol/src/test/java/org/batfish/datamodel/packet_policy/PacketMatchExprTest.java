package org.batfish.datamodel.packet_policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.TrueExpr;
import org.junit.Test;

/** Tests of {@link PacketMatchExpr} */
public class PacketMatchExprTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new PacketMatchExpr(TrueExpr.INSTANCE), new PacketMatchExpr(TrueExpr.INSTANCE))
        .addEqualityGroup(new PacketMatchExpr(FalseExpr.INSTANCE))
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    PacketMatchExpr pm = new PacketMatchExpr(TrueExpr.INSTANCE);
    assertThat(SerializationUtils.clone(pm), equalTo(pm));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    PacketMatchExpr pm = new PacketMatchExpr(TrueExpr.INSTANCE);
    assertThat(BatfishObjectMapper.clone(pm, PacketMatchExpr.class), equalTo(pm));
  }

  @Test
  public void testToString() {
    PacketMatchExpr pm = new PacketMatchExpr(TrueExpr.INSTANCE);
    assertTrue(pm.toString().contains(pm.getClass().getSimpleName()));
  }
}
