package org.batfish.symbolic.smt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.SortedMap;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Configuration.Builder;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.rules.TemporaryFolder;

/** A test network with two nodes and two static routes from one to the other. */
public class TwoNodeNetworkWithTwoLinks {
  public static final Prefix DST_PREFIX_1 = Prefix.parse("1.1.0.0/32");
  public static final Prefix DST_PREFIX_2 = Prefix.parse("2.1.0.0/32");
  public static final Prefix LINK_1_NETWORK = Prefix.parse("1.0.0.0/31");
  public static final Prefix LINK_2_NETWORK = Prefix.parse("2.0.0.0/31");

  public static final String DST_NODE = "dest";
  public static final String SRC_NODE = "src";

  private TwoNodeNetworkWithTwoLinks() {}

  public static Batfish create(TemporaryFolder temp) throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Builder cb = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Interface.Builder ib = nf.interfaceBuilder().setActive(true).setBandwidth(1E9d);
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);

    Configuration dstNode = cb.setHostname(DST_NODE).build();
    Configuration srcNode = cb.setHostname(SRC_NODE).build();
    Vrf dstVrf = vb.setOwner(dstNode).build();
    Vrf srcVrf = vb.setOwner(srcNode).build();

    // first link
    ib.setOwner(srcNode)
        .setVrf(srcVrf)
        .setAddress(
            new InterfaceAddress(LINK_1_NETWORK.getStartIp(), LINK_1_NETWORK.getPrefixLength()))
        .build();
    ib.setOwner(dstNode)
        .setVrf(dstVrf)
        .setAddress(
            new InterfaceAddress(LINK_1_NETWORK.getEndIp(), LINK_1_NETWORK.getPrefixLength()))
        .setSourceNats(ImmutableList.of())
        .build();

    // second link
    ib.setOwner(srcNode)
        .setVrf(srcVrf)
        .setAddress(
            new InterfaceAddress(LINK_2_NETWORK.getStartIp(), LINK_2_NETWORK.getPrefixLength()))
        .build();
    ib.setOwner(dstNode)
        .setVrf(dstVrf)
        .setAddress(
            new InterfaceAddress(LINK_2_NETWORK.getEndIp(), LINK_2_NETWORK.getPrefixLength()))
        .setSourceNats(ImmutableList.of())
        .build();

    // destination for the first link
    ib.setOwner(dstNode)
        .setVrf(dstVrf)
        .setAddress(new InterfaceAddress(DST_PREFIX_1.getStartIp(), DST_PREFIX_1.getPrefixLength()))
        .build();

    // destination for the second link
    ib.setOwner(dstNode)
        .setVrf(dstVrf)
        .setAddress(new InterfaceAddress(DST_PREFIX_2.getStartIp(), DST_PREFIX_2.getPrefixLength()))
        .build();

    StaticRoute.Builder bld = StaticRoute.builder();
    srcVrf.setStaticRoutes(
        ImmutableSortedSet.of(
            bld.setNetwork(DST_PREFIX_1).setNextHopIp(LINK_1_NETWORK.getEndIp()).build(),
            bld.setNetwork(DST_PREFIX_2).setNextHopIp(LINK_2_NETWORK.getEndIp()).build()));

    SortedMap<String, Configuration> configs =
        ImmutableSortedMap.of(srcNode.getHostname(), srcNode, dstNode.getHostname(), dstNode);
    return BatfishTestUtils.getBatfish(configs, temp);
  }
}
