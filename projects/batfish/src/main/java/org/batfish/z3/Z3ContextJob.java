package org.batfish.z3;

import com.google.common.collect.Lists;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Params;
import com.microsoft.z3.Status;
import java.util.Map;
import org.batfish.common.BatfishException;
import org.batfish.config.Settings;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.State;
import org.batfish.job.BatfishJob;
import org.batfish.job.BatfishJobResult;

public abstract class Z3ContextJob<R extends BatfishJobResult<?, ?>> extends BatfishJob<R> {

  protected static Flow createFlow(
      String node, String vrf, Map<HeaderField, Long> constraints, String tag) {
    Flow.Builder flowBuilder = new Flow.Builder();
    flowBuilder.setIngressNode(node);
    flowBuilder.setTag(tag);
    constraints.forEach(
        (headerField, value) -> {
          if (headerField instanceof BasicHeaderField) {
            switch ((BasicHeaderField) headerField) {
              case DST_IP:
                flowBuilder.setDstIp(new Ip(value));
                break;

              case SRC_PORT:
                flowBuilder.setSrcPort(value.intValue());
                break;

              case DST_PORT:
                flowBuilder.setDstPort(value.intValue());
                break;

              case FRAGMENT_OFFSET:
                flowBuilder.setFragmentOffset(value.intValue());
                break;

              case IP_PROTOCOL:
                flowBuilder.setIpProtocol(IpProtocol.fromNumber(value.intValue()));
                break;

              case DSCP:
                flowBuilder.setDscp(value.intValue());
                break;

              case ECN:
                flowBuilder.setEcn(value.intValue());
                break;

              case STATE:
                flowBuilder.setState(State.fromNum(value.intValue()));
                break;

              case ICMP_TYPE:
                flowBuilder.setIcmpType(value.intValue());
                break;

              case ICMP_CODE:
                flowBuilder.setIcmpCode(value.intValue());
                break;

              case ORIG_SRC_IP:
                flowBuilder.setSrcIp(new Ip(value));
                break;

              case SRC_IP:
                break;

              case PACKET_LENGTH:
                flowBuilder.setPacketLength(value.intValue());
                break;

              case TCP_FLAGS_CWR:
                flowBuilder.setTcpFlagsCwr(value.intValue());
                break;

              case TCP_FLAGS_ECE:
                flowBuilder.setTcpFlagsEce(value.intValue());
                break;

              case TCP_FLAGS_URG:
                flowBuilder.setTcpFlagsUrg(value.intValue());
                break;

              case TCP_FLAGS_ACK:
                flowBuilder.setTcpFlagsAck(value.intValue());
                break;

              case TCP_FLAGS_PSH:
                flowBuilder.setTcpFlagsPsh(value.intValue());
                break;

              case TCP_FLAGS_RST:
                flowBuilder.setTcpFlagsRst(value.intValue());
                break;

              case TCP_FLAGS_SYN:
                flowBuilder.setTcpFlagsSyn(value.intValue());
                break;

              case TCP_FLAGS_FIN:
                flowBuilder.setTcpFlagsFin(value.intValue());
                break;

              default:
                throw new BatfishException(
                    String.format(
                        "Unsupported %s: %s", BasicHeaderField.class.getSimpleName(), headerField));
            }
          } else if (headerField instanceof TransformationHeaderField) {
            switch ((TransformationHeaderField) headerField) {
              default:
                throw new BatfishException(
                    String.format(
                        "Unsupported %s: %s",
                        TransformationHeaderField.class.getSimpleName(), headerField));
            }
          }
        });
    return flowBuilder.build();
  }

  public Z3ContextJob(Settings settings) {
    super(settings);
  }

  protected Expr answerFixedPoint(Fixedpoint fix, NodProgram program) {
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
    return fix.getAnswer();
  }

  protected BoolExpr computeSmtConstraintsViaNod(NodProgram program, boolean negate) {
    Fixedpoint fix = mkFixedpoint(program, true);
    Expr answer = answerFixedPoint(fix, program);
    return getSolverInput(answer, program, negate);
  }

  protected BoolExpr getSolverInput(Expr answer, NodProgram program, boolean negate) {
    BoolExpr solverInput;
    if (answer.getArgs().length > 0) {

      Expr[] reversedVars =
          Lists.reverse(Lists.newArrayList(program.getNodContext().getVariablesAsConsts().values()))
              .toArray(new Expr[] {});
      Expr substitutedAnswer = answer.substituteVars(reversedVars);
      solverInput = (BoolExpr) substitutedAnswer;
    } else {
      solverInput = (BoolExpr) answer;
    }
    if (negate) {
      solverInput = program.getNodContext().getContext().mkNot(solverInput);
    }
    return solverInput;
  }

  protected Fixedpoint mkFixedpoint(NodProgram program, boolean printAnswer) {
    Context ctx = program.getNodContext().getContext();
    Params p = ctx.mkParams();
    p.add("timeout", _settings.getZ3timeout());
    p.add("fixedpoint.engine", "datalog");
    p.add("fixedpoint.datalog.default_relation", "doc");
    p.add("fixedpoint.print_answer", printAnswer);
    Fixedpoint fix = ctx.mkFixedpoint();
    fix.setParameters(p);
    for (FuncDecl relationDeclaration :
        program.getNodContext().getRelationDeclarations().values()) {
      fix.registerRelation(relationDeclaration);
    }
    for (BoolExpr rule : program.getRules()) {
      fix.addRule(rule, null);
    }
    return fix;
  }
}
