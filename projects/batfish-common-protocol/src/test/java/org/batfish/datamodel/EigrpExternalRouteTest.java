package org.batfish.datamodel;

import static org.hamcrest.MatcherAssert.assertThat;

import org.batfish.datamodel.eigrp.ClassicMetric;
import org.batfish.datamodel.eigrp.EigrpMetricValues;
import org.hamcrest.Matchers;
import org.junit.Test;

public class EigrpExternalRouteTest {

  @Test
  public void testToBuilder() {
    EigrpExternalRoute r =
        EigrpExternalRoute.builder()
            .setNetwork(Prefix.parse("1.1.1.0/24"))
            .setMetric(1L)
            .setDestinationAsn(1L)
            .setEigrpMetric(
                ClassicMetric.builder()
                    .setValues(
                        EigrpMetricValues.builder()
                            .setBandwidth((long) 1E8)
                            .setDelay((long) 1D)
                            .build())
                    .build())
            .setProcessAsn(2L)
            .build();
    assertThat(r.toBuilder().build(), Matchers.equalTo(r));
  }
}
