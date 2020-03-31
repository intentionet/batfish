package org.batfish.common.util.isp;

import com.google.common.testing.EqualsTester;
import org.batfish.common.util.isp.IspModel.Remote;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.isp_configuration.traffic_filtering.IspTrafficFiltering;
import org.junit.Test;

public class IspModelTest {

  @Test
  public void testEquals() {
    IspModel.Builder builder = IspModel.builder().setAsn(1L).setName("name");
    new EqualsTester()
        .addEqualityGroup(builder.build(), builder.build())
        .addEqualityGroup(builder.setAsn(2).build())
        .addEqualityGroup(builder.setName("other").build())
        .addEqualityGroup(
            builder
                .setRemotes(
                    new Remote(
                        "a",
                        "b",
                        ConcreteInterfaceAddress.parse("1.1.1.1/32"),
                        BgpActivePeerConfig.builder().build()))
                .build())
        .addEqualityGroup(
            builder.setAdditionalPrefixesToInternet(Prefix.parse("1.1.1.1/32")).build())
        .addEqualityGroup(
            builder.setTrafficFiltering(IspTrafficFiltering.blockReservedAddressesAtInternet()))
        .testEquals();
  }

  @Test
  public void testEqualsRemote() {
    new EqualsTester()
        .addEqualityGroup(
            new Remote(
                "a",
                "b",
                ConcreteInterfaceAddress.parse("1.1.1.1/32"),
                BgpActivePeerConfig.builder().build()),
            new Remote(
                "a",
                "b",
                ConcreteInterfaceAddress.parse("1.1.1.1/32"),
                BgpActivePeerConfig.builder().build()))
        .addEqualityGroup(
            new Remote(
                "other",
                "b",
                ConcreteInterfaceAddress.parse("1.1.1.1/32"),
                BgpActivePeerConfig.builder().build()))
        .addEqualityGroup(
            new Remote(
                "a",
                "other",
                ConcreteInterfaceAddress.parse("1.1.1.1/32"),
                BgpActivePeerConfig.builder().build()))
        .addEqualityGroup(
            new Remote(
                "a",
                "b",
                ConcreteInterfaceAddress.parse("2.2.2.2/32"),
                BgpActivePeerConfig.builder().build()))
        .addEqualityGroup(
            new Remote(
                "other",
                "b",
                ConcreteInterfaceAddress.parse("1.1.1.1/32"),
                BgpActivePeerConfig.builder().setDescription("other").build()))
        .testEquals();
  }
}
