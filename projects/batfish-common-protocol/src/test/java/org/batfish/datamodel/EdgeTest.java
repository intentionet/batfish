package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.testing.EqualsTester;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link Edge} */
@RunWith(JUnit4.class)
public class EdgeTest {

  private static Edge EDGE =
      new Edge(NodeInterfacePair.of("tail", "tailInt"), NodeInterfacePair.of("head", "headInt"));

  @Test
  public void testFactory() {
    assertThat(Edge.of("tail", "tailInt", "head", "headInt"), equalTo(EDGE));
  }

  @Test
  public void testGetterEquivalence() {
    assertThat(EDGE.getNode1(), equalTo(EDGE.getTail().getHostname()));
    assertThat(EDGE.getNode2(), equalTo(EDGE.getHead().getHostname()));
    assertThat(EDGE.getInt1(), equalTo(EDGE.getTail().getInterface()));
    assertThat(EDGE.getInt2(), equalTo(EDGE.getHead().getInterface()));
  }

  @Test
  public void testToString() {
    assertThat(EDGE.toString(), equalTo("<tail:tailInt, head:headInt>"));
  }

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            EDGE,
            new Edge(
                NodeInterfacePair.of("tail", "tailInt"), NodeInterfacePair.of("head", "headInt")))
        .addEqualityGroup(EDGE.reverse())
        .addEqualityGroup(
            new Edge(
                NodeInterfacePair.of("tail1", "tailInt"), NodeInterfacePair.of("head", "headInt")))
        .addEqualityGroup(
            new Edge(
                NodeInterfacePair.of("tail", "tailint"), NodeInterfacePair.of("head", "headInt")))
        .addEqualityGroup(
            new Edge(
                NodeInterfacePair.of("tail", "tailInt"), NodeInterfacePair.of("hEad", "headInt")))
        .addEqualityGroup(
            new Edge(
                NodeInterfacePair.of("tail", "tailInt"),
                NodeInterfacePair.of("head", "headIntOther")))
        .testEquals();
  }

  @Test
  public void testReverse() {
    assertThat(
        EDGE.reverse(),
        equalTo(
            new Edge(
                NodeInterfacePair.of("head", "headInt"), NodeInterfacePair.of("tail", "tailInt"))));
  }
}
