package org.batfish.representation.cisco_xr;

import static org.batfish.common.WellKnownCommunity.INTERNET;
import static org.batfish.common.WellKnownCommunity.NO_ADVERTISE;
import static org.batfish.common.WellKnownCommunity.NO_EXPORT;
import static org.batfish.common.WellKnownCommunity.NO_EXPORT_SUBCONFED;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.communities.AllStandardCommunities;
import org.batfish.datamodel.routing_policy.communities.CommunityIn;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunityNot;
import org.batfish.datamodel.routing_policy.communities.CommunitySetDifference;
import org.batfish.datamodel.routing_policy.communities.InputCommunities;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.communities.SetCommunities;
import org.batfish.datamodel.routing_policy.statement.Statement;

@ParametersAreNonnullByDefault
public final class XrRoutePolicyDeleteAllStatement extends RoutePolicySetStatement {

  public static @Nonnull XrRoutePolicyDeleteAllStatement instance() {
    return INSTANCE;
  }

  @Override
  public @Nonnull Statement toSetStatement(CiscoXrConfiguration cc, Configuration c, Warnings w) {
    return SET_STATEMENT;
  }

  private XrRoutePolicyDeleteAllStatement() {}

  private static final XrRoutePolicyDeleteAllStatement INSTANCE =
      new XrRoutePolicyDeleteAllStatement();

  private static final Statement SET_STATEMENT =
      new SetCommunities(
          new CommunitySetDifference(
              InputCommunities.instance(),
              new CommunityMatchAll(
                  ImmutableList.of(
                      AllStandardCommunities.instance(),
                      new CommunityNot(
                          new CommunityIn(
                              new LiteralCommunitySet(
                                  org.batfish.datamodel.routing_policy.communities.CommunitySet.of(
                                      ImmutableList.of(
                                              // https://www.cisco.com/c/en/us/td/docs/routers/xr12000/software/xr12k_r4-0/routing/configuration/guide/rc40xr12k_chapter7.html
                                              INTERNET,
                                              NO_EXPORT,
                                              NO_ADVERTISE,
                                              NO_EXPORT_SUBCONFED)
                                          .stream()
                                          .map(StandardCommunity::of)
                                          .collect(ImmutableSet.toImmutableSet())))))))));
}
