package org.batfish.datamodel.routing_policy.expr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.routing_policy.Environment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link FirstMatchChain} */
@RunWith(JUnit4.class)
public class FirstMatchChainTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(new Object())
        .addEqualityGroup(
            new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE)),
            new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE)))
        .addEqualityGroup(ImmutableList.of())
        .addEqualityGroup(ImmutableList.of(BooleanExprs.FALSE))
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    FirstMatchChain fmc = new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE));
    assertThat(SerializationUtils.clone(fmc), equalTo(fmc));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    FirstMatchChain fmc = new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE));
    assertThat(BatfishObjectMapper.clone(fmc, FirstMatchChain.class), equalTo(fmc));
  }

  @Test
  public void testEvaluate() {
    FirstMatchChain fmc =
        new FirstMatchChain(ImmutableList.of(BooleanExprs.FALSE, BooleanExprs.TRUE));
    // Test that first match is used
    assertThat(
        fmc.evaluate(
                Environment.builder(new Configuration("host", ConfigurationFormat.JUNIPER))
                    .setVrf(Configuration.DEFAULT_VRF_NAME)
                    .build())
            .getBooleanValue(),
        equalTo(false));
    fmc = new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE, BooleanExprs.FALSE));
    // Test that first match is used
    assertThat(
        fmc.evaluate(
                Environment.builder(new Configuration("host", ConfigurationFormat.JUNIPER))
                    .setVrf(Configuration.DEFAULT_VRF_NAME)
                    .build())
            .getBooleanValue(),
        equalTo(true));
  }

  @Test
  public void testToString() {
    FirstMatchChain fmc = new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE));
    assertThat(fmc.toString(), equalTo("FirstMatchChain{subroutines=[StaticBooleanExpr{}]}"));
  }
}
