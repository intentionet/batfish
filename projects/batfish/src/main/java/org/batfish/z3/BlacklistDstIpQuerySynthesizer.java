package org.batfish.z3;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Z3Exception;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkAddress;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.node.AndExpr;
import org.batfish.z3.node.BooleanExpr;
import org.batfish.z3.node.EqExpr;
import org.batfish.z3.node.LitIntExpr;
import org.batfish.z3.node.NotExpr;
import org.batfish.z3.node.QueryExpr;
import org.batfish.z3.node.QueryRelationExpr;
import org.batfish.z3.node.RuleExpr;
import org.batfish.z3.node.SaneExpr;
import org.batfish.z3.node.VarIntExpr;

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
            NetworkAddress prefix = iface.getAddress();
            if (prefix != null) {
              _blacklistIps.add(prefix.getAddress());
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
          NetworkAddress prefix = iface.getAddress();
          if (prefix != null) {
            _blacklistIps.add(prefix.getAddress());
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
                .getAddress();
        Ip ip2 =
            configurations
                .get(edge.getNode2())
                .getInterfaces()
                .get(edge.getInt2())
                .getAddress()
                .getAddress();
        _blacklistIps.add(ip1);
        _blacklistIps.add(ip2);
      }
    }
  }

  @Override
  public NodProgram getNodProgram(NodProgram baseProgram) throws Z3Exception {
    NodProgram program = new NodProgram(baseProgram.getContext());
    AndExpr queryConditions = new AndExpr();
    queryConditions.addConjunct(SaneExpr.INSTANCE);
    for (Ip blacklistIp : _blacklistIps) {
      BooleanExpr blacklistIpCondition =
          new NotExpr(
              new EqExpr(new VarIntExpr(Synthesizer.DST_IP_VAR), new LitIntExpr(blacklistIp)));
      queryConditions.addConjunct(blacklistIpCondition);
    }
    RuleExpr queryRule = new RuleExpr(queryConditions, QueryRelationExpr.INSTANCE);
    List<BoolExpr> rules = program.getRules();
    rules.add(queryRule.toBoolExpr(baseProgram));
    QueryExpr query = new QueryExpr(QueryRelationExpr.INSTANCE);
    BoolExpr queryBoolExpr = query.toBoolExpr(baseProgram);
    program.getQueries().add(queryBoolExpr);
    return program;
  }
}
