package org.batfish.question.reachfilter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.FiltersSpecifier;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.question.tracefilters.TraceFiltersAnswerer;
import org.batfish.question.tracefilters.TraceFiltersQuestion;

public class ReachFilterAnswerer extends Answerer {

  public ReachFilterAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    ReachFilterQuestion question = (ReachFilterQuestion) _question;
    List<Pair<String, IpAccessList>> acls = getQueryAcls(question);
    if (acls.isEmpty()) {
      throw new BatfishException("No matching filters");
    }

    /*
     * For each query ACL, try to get a flow. If one exists, run traceFilter on that flow.
     * Concatenate the answers for all flows into one big table.
     */
    TableAnswerElement answer = null;
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    for (Pair<String, IpAccessList> pair : acls) {
      Optional<Flow> result = null;
      try {
        result = _batfish.reachFilter(pair.getFirst(), pair.getSecond());
      } catch (Throwable t) {
        _batfish.getLogger().warn(t.getMessage());
        continue;
      }
      if (!result.isPresent()) {
        continue;
      }
      Configuration config = configurations.get(pair.getFirst());
      IpAccessList acl = pair.getSecond();
      Flow flow = result.get();
      if (answer == null) {
        answer = traceFilter(config, acl, flow);
      } else {
        traceFilter(config, acl, flow).getRows().iterator().forEachRemaining(answer::addRow);
      }
    }
    return answer;
  }

  @VisibleForTesting
  List<Pair<String, IpAccessList>> getQueryAcls(ReachFilterQuestion question) {
    SortedMap<String, Configuration> configs = _batfish.loadConfigurations();
    List<Pair<String, IpAccessList>> acls =
        question
            .getNodesSpecifier()
            .getMatchingNodes(_batfish)
            .stream()
            .map(configs::get)
            .flatMap(
                config ->
                    config
                        .getIpAccessLists()
                        .values()
                        .stream()
                        .filter(acl -> question.getFiltersSpecifier().matches(acl, config))
                        .map(acl -> new Pair<>(config.getHostname(), acl)))
            .collect(Collectors.toList());
    switch (question.getType()) {
      case PERMIT:
        return acls;
      case DENY:
        // for each ACL, construct a new ACL that accepts if and only if the original denies.
        return acls.stream()
            .map(pair -> new Pair<>(pair.getFirst(), toDenyAcl(pair.getSecond())))
            .collect(ImmutableList.toImmutableList());
      case MATCH_LINE:
        // for each ACL, construct a new ACL that accepts if and only if the specified line matches
        Integer lineNumber = question.getLineNumber();
        return acls.stream()
            .filter(pair -> pair.getSecond().getLines().size() > lineNumber)
            .map(pair -> new Pair<>(pair.getFirst(), toMatchLineAcl(lineNumber, pair.getSecond())))
            .collect(ImmutableList.toImmutableList());
      default:
        throw new BatfishException("Unexpected query Type: " + question.getType());
    }
  }

  @VisibleForTesting
  static IpAccessList toMatchLineAcl(Integer lineNumber, IpAccessList acl) {
    List<IpAccessListLine> lines =
        Streams.concat(
                acl.getLines()
                    .subList(0, lineNumber)
                    .stream()
                    .map(l -> l.toBuilder().setAction(LineAction.REJECT).build()),
                Stream.of(
                    acl.getLines()
                        .get(lineNumber)
                        .toBuilder()
                        .setAction(LineAction.ACCEPT)
                        .build()))
            .collect(ImmutableList.toImmutableList());
    return IpAccessList.builder().setName(acl.getName()).setLines(lines).build();
  }

  @VisibleForTesting
  static IpAccessList toDenyAcl(IpAccessList acl) {
    List<IpAccessListLine> lines =
        Streams.concat(
                acl.getLines()
                    .stream()
                    .map(
                        l ->
                            l.toBuilder()
                                // flip action
                                .setAction(
                                    l.getAction() == LineAction.ACCEPT
                                        ? LineAction.REJECT
                                        : LineAction.ACCEPT)
                                .build()),
                // accept if we reach the end of the ACL
                Stream.of(IpAccessListLine.ACCEPT_ALL))
            .collect(ImmutableList.toImmutableList());
    return IpAccessList.builder().setName(acl.getName()).setLines(lines).build();
  }

  @VisibleForTesting
  @Nonnull
  TableAnswerElement traceFilter(Configuration config, IpAccessList acl, Flow flow) {
    TraceFiltersQuestion traceFiltersQuestion =
        new TraceFiltersQuestion(
            new NodesSpecifier(config.getHostname()), new FiltersSpecifier(acl.getName()));
    traceFiltersQuestion.setDscp(flow.getDscp());
    traceFiltersQuestion.setDst(flow.getDstIp().toString());
    traceFiltersQuestion.setDstPort(flow.getDstPort());
    traceFiltersQuestion.setEcn(flow.getEcn());
    traceFiltersQuestion.setFragmentOffset(flow.getFragmentOffset());
    traceFiltersQuestion.setIcmpCode(flow.getFragmentOffset());
    traceFiltersQuestion.setIcmpType(flow.getFragmentOffset());
    traceFiltersQuestion.setIpProtocol(flow.getIpProtocol());
    traceFiltersQuestion.setPacketLength(flow.getPacketLength());
    traceFiltersQuestion.setSrcIp(flow.getSrcIp());
    traceFiltersQuestion.setSrcPort(flow.getSrcPort());
    traceFiltersQuestion.setState(flow.getState());
    traceFiltersQuestion.setTcpFlagsAck(flow.getTcpFlagsAck() == 1);
    traceFiltersQuestion.setTcpFlagsCwr(flow.getTcpFlagsCwr() == 1);
    traceFiltersQuestion.setTcpFlagsEce(flow.getTcpFlagsEce() == 1);
    traceFiltersQuestion.setTcpFlagsFin(flow.getTcpFlagsFin() == 1);
    traceFiltersQuestion.setTcpFlagsPsh(flow.getTcpFlagsPsh() == 1);
    traceFiltersQuestion.setTcpFlagsRst(flow.getTcpFlagsRst() == 1);
    traceFiltersQuestion.setTcpFlagsSyn(flow.getTcpFlagsSyn() == 1);
    traceFiltersQuestion.setTcpFlagsUrg(flow.getTcpFlagsUrg() == 1);
    return new TraceFiltersAnswerer(traceFiltersQuestion, _batfish).answer();
  }
}
