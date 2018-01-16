package org.batfish.z3;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;
import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.config.Settings;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.State;
import org.batfish.job.BatfishJob;

public final class NodJob extends BatfishJob<NodJobResult> {

  public static Flow createFlow(
      String node, String vrf, Map<String, Long> constraints, String tag) {
    Flow.Builder flowBuilder = new Flow.Builder();
    flowBuilder.setIngressNode(node);
    flowBuilder.setTag(tag);
    for (String varName : constraints.keySet()) {
      Long value = constraints.get(varName);
      switch (varName) {
        case Synthesizer.SRC_IP_VAR:
          flowBuilder.setSrcIp(new Ip(value));
          break;

        case Synthesizer.DST_IP_VAR:
          flowBuilder.setDstIp(new Ip(value));
          break;

        case Synthesizer.SRC_PORT_VAR:
          flowBuilder.setSrcPort(value.intValue());
          break;

        case Synthesizer.DST_PORT_VAR:
          flowBuilder.setDstPort(value.intValue());
          break;

        case Synthesizer.FRAGMENT_OFFSET_VAR:
          flowBuilder.setFragmentOffset(value.intValue());
          break;

        case Synthesizer.IP_PROTOCOL_VAR:
          flowBuilder.setIpProtocol(IpProtocol.fromNumber(value.intValue()));
          break;

        case Synthesizer.DSCP_VAR:
          flowBuilder.setDscp(value.intValue());
          break;

        case Synthesizer.ECN_VAR:
          flowBuilder.setEcn(value.intValue());
          break;

        case Synthesizer.STATE_VAR:
          flowBuilder.setState(State.fromNum(value.intValue()));
          break;

        case Synthesizer.ICMP_TYPE_VAR:
          flowBuilder.setIcmpType(value.intValue());
          break;

        case Synthesizer.ICMP_CODE_VAR:
          flowBuilder.setIcmpCode(value.intValue());
          break;

        case Synthesizer.PACKET_LENGTH_VAR:
          flowBuilder.setPacketLength(value.intValue());
          break;

        case Synthesizer.TCP_FLAGS_CWR_VAR:
          flowBuilder.setTcpFlagsCwr(value.intValue());
          break;

        case Synthesizer.TCP_FLAGS_ECE_VAR:
          flowBuilder.setTcpFlagsEce(value.intValue());
          break;

        case Synthesizer.TCP_FLAGS_URG_VAR:
          flowBuilder.setTcpFlagsUrg(value.intValue());
          break;

        case Synthesizer.TCP_FLAGS_ACK_VAR:
          flowBuilder.setTcpFlagsAck(value.intValue());
          break;

        case Synthesizer.TCP_FLAGS_PSH_VAR:
          flowBuilder.setTcpFlagsPsh(value.intValue());
          break;

        case Synthesizer.TCP_FLAGS_RST_VAR:
          flowBuilder.setTcpFlagsRst(value.intValue());
          break;

        case Synthesizer.TCP_FLAGS_SYN_VAR:
          flowBuilder.setTcpFlagsSyn(value.intValue());
          break;

        case Synthesizer.TCP_FLAGS_FIN_VAR:
          flowBuilder.setTcpFlagsFin(value.intValue());
          break;

        default:
          throw new BatfishException("invalid variable name");
      }
    }
    return flowBuilder.build();
  }

  private Synthesizer _dataPlaneSynthesizer;

  private final SortedSet<Pair<String, String>> _nodeVrfSet;

  private QuerySynthesizer _querySynthesizer;

  private String _tag;

  public NodJob(
      Settings settings,
      Synthesizer dataPlaneSynthesizer,
      QuerySynthesizer querySynthesizer,
      SortedSet<Pair<String, String>> nodeVrfSet,
      String tag) {
    super(settings);
    _dataPlaneSynthesizer = dataPlaneSynthesizer;
    _querySynthesizer = querySynthesizer;
    _nodeVrfSet = new TreeSet<>();
    _nodeVrfSet.addAll(nodeVrfSet);
    _tag = tag;
  }

  @Override
  public NodJobResult call() {
    long startTime = System.currentTimeMillis();
    long elapsedTime;
    try (Context ctx = new Context()) {
      NodProgram baseProgram = _dataPlaneSynthesizer.synthesizeNodDataPlaneProgram(ctx);
      NodProgram queryProgram = _querySynthesizer.getNodProgram(baseProgram);
      NodProgram program = baseProgram.append(queryProgram);
      Params p = ctx.mkParams();
      p.add("timeout", _settings.getZ3timeout());
      p.add("fixedpoint.engine", "datalog");
      p.add("fixedpoint.datalog.default_relation", "doc");
      p.add("fixedpoint.print_answer", true);
      Fixedpoint fix = ctx.mkFixedpoint();
      fix.setParameters(p);
      for (FuncDecl relationDeclaration : program.getRelationDeclarations().values()) {
        fix.registerRelation(relationDeclaration);
      }
      for (BoolExpr rule : program.getRules()) {
        fix.addRule(rule, null);
      }
      for (BoolExpr query : program.getQueries()) {
        Status status = fix.query(query);
        switch (status) {
          case SATISFIABLE:
            break;
          case UNKNOWN:
            throw new BatfishException("Query satisfiability unknown");
          case UNSATISFIABLE:
            break;
          default:
            throw new BatfishException("invalid status");
        }
      }
      Expr answer = fix.getAnswer();
      BoolExpr solverInput;
      if (answer.getArgs().length > 0) {
        List<Expr> reversedVarList = new ArrayList<>();
        reversedVarList.addAll(program.getVariablesAsConsts().values());
        Collections.reverse(reversedVarList);
        Expr[] reversedVars = reversedVarList.toArray(new Expr[] {});
        Expr substitutedAnswer = answer.substituteVars(reversedVars);
        solverInput = (BoolExpr) substitutedAnswer;
      } else {
        solverInput = (BoolExpr) answer;
      }
      if (_querySynthesizer.getNegate()) {
        solverInput = ctx.mkNot(solverInput);
      }
      Solver solver = ctx.mkSolver();
      solver.add(solverInput);
      Status solverStatus = solver.check();
      switch (solverStatus) {
        case SATISFIABLE:
          break;

        case UNKNOWN:
          throw new BatfishException("Stage 2 query satisfiability unknown");

        case UNSATISFIABLE:
          elapsedTime = System.currentTimeMillis() - startTime;
          return new NodJobResult(elapsedTime, _logger.getHistory());

        default:
          throw new BatfishException("invalid status");
      }
      Model model = solver.getModel();
      Map<String, Long> constraints = new LinkedHashMap<>();
      for (FuncDecl constDecl : model.getConstDecls()) {
        String name = constDecl.getName().toString();
        BitVecExpr varConstExpr = program.getVariablesAsConsts().get(name);
        long val = ((BitVecNum) model.getConstInterp(varConstExpr)).getLong();
        constraints.put(name, val);
      }
      Set<Flow> flows = new HashSet<>();
      for (Pair<String, String> nodeVrf : _nodeVrfSet) {
        String node = nodeVrf.getFirst();
        String vrf = nodeVrf.getSecond();
        Flow flow = createFlow(node, vrf, constraints);
        flows.add(flow);
      }
      elapsedTime = System.currentTimeMillis() - startTime;
      return new NodJobResult(elapsedTime, _logger.getHistory(), flows);
    } catch (Z3Exception e) {
      elapsedTime = System.currentTimeMillis() - startTime;
      return new NodJobResult(
          elapsedTime,
          _logger.getHistory(),
          new BatfishException("Error running NoD on concatenated data plane", e));
    }
  }

  private Flow createFlow(String node, String vrf, Map<String, Long> constraints) {
    return createFlow(node, vrf, constraints, _tag);
  }
}
