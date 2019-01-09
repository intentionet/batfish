package org.batfish.datamodel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link Mlag} */
@RunWith(JUnit4.class)
public class MlagTest {

  @Test
  public void testSerialization() throws IOException {
    Mlag m =
        Mlag.builder()
            .setId("ID")
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setLocalInterface("Ethernet1")
            .build();

    assertThat(BatfishObjectMapper.clone(m, Mlag.class), equalTo(m));
  }
}
