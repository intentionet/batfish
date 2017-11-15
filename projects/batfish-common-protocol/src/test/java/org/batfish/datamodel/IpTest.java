package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link Ip}. */
@RunWith(JUnit4.class)
public class IpTest {
  @Test
  public void numSubnetBitsToSubnetLong() throws Exception {
    assertThat(Ip.numSubnetBitsToSubnetLong(0), equalTo(0L));
    assertThat(Ip.numSubnetBitsToSubnetLong(4), equalTo(0xF0000000L));
    assertThat(Ip.numSubnetBitsToSubnetLong(17), equalTo(0xFFFF8000L));
    assertThat(Ip.numSubnetBitsToSubnetLong(31), equalTo(0xFFFFFFFEL));
    assertThat(Ip.numSubnetBitsToSubnetLong(32), equalTo(0xFFFFFFFFL));
  }
}
