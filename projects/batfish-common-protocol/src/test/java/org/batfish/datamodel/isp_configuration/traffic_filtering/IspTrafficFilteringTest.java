package org.batfish.datamodel.isp_configuration.traffic_filtering;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

/** Tests of {@link IspTrafficFiltering} */
public class IspTrafficFilteringTest {
  @Test
  public void testSerializationNone() {
    IspTrafficFiltering f = IspTrafficFiltering.none();
    assertThat(BatfishObjectMapper.clone(f, IspTrafficFiltering.class), equalTo(f));
  }
}
