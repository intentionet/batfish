package org.batfish.z3;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.EqExpr;
import org.batfish.z3.expr.LitIntExpr;
import org.batfish.z3.expr.NotExpr;
import org.batfish.z3.expr.QueryStatement;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.expr.TransformationRuleStatement;
import org.batfish.z3.expr.VarIntExpr;
import org.batfish.z3.state.Query;

public class BlacklistDstIpQuerySynthesizer extends BaseQuerySynthesizer {

  private Set<Ip> _blacklistIps;

  public BlacklistDstIpQuerySynthesizer(
      @Nullable Set<Ip> explicitBlacklistIps,
      Set<String> blacklistNodes,
      Set<NodeInterfacePair> blacklistInterfaces,
      SortedSet<Edge> blacklistEdges,
      Map<String, Configuration> configurations) {
    _blacklistIps = new TreeSet<>();
    if (explicitBlacklistIps != null) {
      _blacklistIps.addAll(explicitBlacklistIps);
    }
    if (blacklistNodes != null) {
      for (String hostname : blacklistNodes) {
        Configuration node = configurations.get(hostname);
        for (Interface iface : node.getInterfaces().values()) {
          if (iface.getActive()) {
            InterfaceAddress address = iface.getAddress();
            if (address != null) {
              _blacklistIps.add(address.getIp());
            }
          }
        }
      }
    }
    if (blacklistInterfaces != null) {
      for (NodeInterfacePair p : blacklistInterfaces) {
        String hostname = p.getHostname();
        String ifaceName = p.getInterface();
        Configuration node = configurations.get(hostname);
        Interface iface = node.getInterfaces().get(ifaceName);
        if (iface.getActive()) {
          InterfaceAddress address = iface.getAddress();
          if (address != null) {
            _blacklistIps.add(address.getIp());
          }
        }
      }
    }
    if (blacklistEdges != null) {
      for (Edge edge : blacklistEdges) {
        Ip ip1 =
            configurations
                .get(edge.getNode1())
                .getInterfaces()
                .get(edge.getInt1())
                .getAddress()
                .getIp();
        Ip ip2 =
            configurations
                .get(edge.getNode2())
                .getInterfaces()
                .get(edge.getInt2())
                .getAddress()
                .getIp();
        _blacklistIps.add(ip1);
        _blacklistIps.add(ip2);
      }
    }
  }

  @Override
  public ReachabilityProgram getReachabilityProgram(SynthesizerInput input) {
    ImmutableList.Builder<BooleanExpr> queryConditionsBuilder = ImmutableList.builder();
    queryConditionsBuilder.add(SaneExpr.INSTANCE);
    for (Ip blacklistIp : _blacklistIps) {
      BooleanExpr blacklistIpCondition =
          new NotExpr(
              new EqExpr(new VarIntExpr(BasicHeaderField.DST_IP), new LitIntExpr(blacklistIp)));
      queryConditionsBuilder.add(blacklistIpCondition);
    }
    return ReachabilityProgram.builder()
        .setInput(input)
        .setQueries(ImmutableList.of(new QueryStatement(Query.INSTANCE)))
        .setRules(
            ImmutableList.of(
                new TransformationRuleStatement(
                    new AndExpr(queryConditionsBuilder.build()), Query.INSTANCE)))
        .build();
  }
}
