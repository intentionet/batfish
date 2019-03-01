package org.batfish.datamodel;

import static org.batfish.datamodel.Prefix.longestCommonPrefix;
import static org.batfish.datamodel.Prefix.parse;
import static org.batfish.datamodel.Prefix.strict;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDInteger;
import org.batfish.common.bdd.BDDUtils;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PrefixTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testCanonicalization() {
    Prefix p = Prefix.parse("255.255.255.255/15");
    assertThat(p.getStartIp(), equalTo(Ip.parse("255.254.0.0")));
    assertThat(p.getPrefixLength(), equalTo(15));
  }

  @Test
  public void testContains() {
    IpSpace p = Prefix.parse("1.2.3.4/31").toIpSpace();
    assertThat(p, containsIp(Ip.parse("1.2.3.4")));
    assertThat(p, containsIp(Ip.parse("1.2.3.5")));
    assertThat(p, not(containsIp(Ip.parse("1.2.3.6"))));
    assertThat(p, not(containsIp(Ip.parse("1.2.3.3"))));

    // Edge cases - 32 bit prefix
    p = Prefix.parse("1.2.3.4/32").toIpSpace();
    assertThat(p, containsIp(Ip.parse("1.2.3.4")));
    assertThat(p, not(containsIp(Ip.parse("1.2.3.5"))));
    assertThat(p, not(containsIp(Ip.parse("1.2.3.3"))));

    // Edge cases - 0 bit prefix
    p = Prefix.parse("0.0.0.0/0").toIpSpace();
    assertThat(p, containsIp(Ip.parse("0.0.0.0")));
    assertThat(p, containsIp(Ip.parse("128.128.128.128")));
    assertThat(p, containsIp(Ip.parse("255.255.255.255")));
  }

  @Test
  public void testComplement() {
    IpSpace ipSpace = Prefix.parse("1.2.3.4/31").toIpSpace().complement();
    assertThat(ipSpace, not(containsIp(Ip.parse("1.2.3.4"))));
    assertThat(ipSpace, not(containsIp(Ip.parse("1.2.3.5"))));
    assertThat(ipSpace, containsIp(Ip.parse("1.2.3.6")));
    assertThat(ipSpace, containsIp(Ip.parse("1.2.3.3")));

    // Edge cases - 32 bit prefix
    ipSpace = Prefix.parse("1.2.3.4/32").toIpSpace().complement();
    assertThat(ipSpace, not(containsIp(Ip.parse("1.2.3.4"))));
    assertThat(ipSpace, containsIp(Ip.parse("1.2.3.5")));
    assertThat(ipSpace, containsIp(Ip.parse("1.2.3.3")));

    // Edge cases - 0 bit prefix
    ipSpace = Prefix.parse("0.0.0.0/0").toIpSpace().complement();
    assertThat(ipSpace, not(containsIp(Ip.parse("0.0.0.0"))));
    assertThat(ipSpace, not(containsIp(Ip.parse("128.128.128.128"))));
    assertThat(ipSpace, not(containsIp(Ip.parse("255.255.255.255"))));
  }

  @Test
  public void testInvalidPrefix() {
    _thrown.expectMessage("Invalid prefix length");
    _thrown.expect(IllegalArgumentException.class);
    Prefix.parse("1.1.1.1/33");
  }

  @Test
  public void testStrictCanonical() {
    String canonicalStr = "1.0.0.0/8";
    Prefix canonical = strict(canonicalStr);

    assertThat(strict(canonicalStr).toString(), equalTo(canonicalStr));
    assertThat(parse(canonicalStr), equalTo(canonical));
  }

  @Test
  public void testStrictNonCanonical() {
    String nonCanonical = "1.2.0.0/8";

    _thrown.expect(IllegalArgumentException.class);
    strict(nonCanonical);
  }

  @Test
  public void testToHostIpSpace() {
    BDDInteger ipAddrBdd = BDDInteger.makeFromIndex(BDDUtils.bddFactory(32), 32, 0, true);
    IpSpaceToBDD toBDD = new IpSpaceToBDD(ipAddrBdd);
    assertThat(
        "/32 host space is preserved",
        toBDD.visit(Prefix.parse("1.2.3.4/32").toHostIpSpace()),
        equalTo(toBDD.visit(Prefix.parse("1.2.3.4/32").toIpSpace())));
    assertThat(
        "/31 host space is preserved",
        toBDD.visit(Prefix.parse("1.2.3.4/31").toHostIpSpace()),
        equalTo(toBDD.visit(Prefix.parse("1.2.3.4/31").toIpSpace())));
    BDD slash30Filtered =
        toBDD
            .visit(Ip.parse("1.2.3.5").toIpSpace())
            .or(toBDD.visit(Ip.parse("1.2.3.6").toIpSpace()));
    assertThat(
        "/30 host space is two IPs",
        toBDD.visit(Prefix.parse("1.2.3.4/30").toHostIpSpace()),
        equalTo(slash30Filtered));
  }

  @Test
  public void testLongestCommonPrefix() {
    // p1 and p2 are equal
    Prefix p1 = Prefix.parse("1.1.1.0/24");
    assertThat(longestCommonPrefix(p1, p1), equalTo(Prefix.parse("1.1.1.0/24")));

    // p1 includes p2
    p1 = Prefix.parse("1.1.1.0/24");
    Prefix p2 = Prefix.parse("1.1.1.0/32");
    assertThat(longestCommonPrefix(p1, p2), equalTo(p1));

    // p2 includes p1
    p1 = Prefix.parse("1.1.1.0/32");
    p2 = Prefix.parse("1.1.1.0/24");
    assertThat(longestCommonPrefix(p1, p2), equalTo(p2));

    // masked bits are ignored
    p1 = Prefix.parse("1.1.1.1/24");
    p2 = Prefix.parse("1.1.1.1/8");
    assertThat(longestCommonPrefix(p1, p2), equalTo(Prefix.parse("1.0.0.0/8")));

    // Different /32s
    p1 = Prefix.parse("0.0.0.0/32");
    p2 = Prefix.parse("0.0.0.255/32");
    assertThat(longestCommonPrefix(p1, p2), equalTo(Prefix.parse("0.0.0.0/24")));
  }
}
