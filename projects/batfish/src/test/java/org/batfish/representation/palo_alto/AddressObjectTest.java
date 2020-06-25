package org.batfish.representation.palo_alto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Range;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.representation.palo_alto.AddressObject.Type;
import org.junit.Test;

/** Tests of {@link AddressObject} */
public class AddressObjectTest {

  @Test
  public void testSetClearsTypeAndMembers() {
    AddressObject a = new AddressObject("name");
    assertNull(a.getIp());
    assertNull(a.getType());

    a.setIp(Ip.ZERO);
    assertThat(a.getIp(), equalTo(Ip.ZERO));
    assertThat(a.getType(), equalTo(Type.IP));

    // Setting prefix clears members and updates type
    a.setPrefix(IpPrefix.parse("1.2.3.4/24"));
    assertNull(a.getIp());
    // Make sure we preserve pre-canonicalized form of prefix ip
    assertThat(a.getIpPrefix().getIp(), equalTo(Ip.parse("1.2.3.4")));
    assertThat(a.getIpPrefix().getPrefix(), equalTo(Prefix.parse("1.2.3.4/24")));
    assertThat(a.getType(), equalTo(Type.PREFIX));

    // Setting range clears members and updates type
    Range<Ip> range = Range.closed(Ip.ZERO, Ip.parse("1.1.1.1"));
    a.setIpRange(range);
    assertNull(a.getIpPrefix());
    assertThat(a.getIpRange(), equalTo(range));
    assertThat(a.getType(), equalTo(Type.IP_RANGE));

    // Setting IP clears members and updates type
    a.setIp(Ip.ZERO);
    assertNull(a.getIpRange());
    assertThat(a.getIp(), equalTo(Ip.ZERO));
    assertThat(a.getType(), equalTo(Type.IP));

    // Setting IP to null clears type and members
    a.setIp(null);
    assertNull(a.getIp());
    assertNull(a.getType());
  }
}
