package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Protocol;
import org.batfish.datamodel.ReachabilityType;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.IReachabilityQuestion;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class ReachabilityQuestionPlugin extends QuestionPlugin {

  public static class ReachabilityAnswerer extends Answerer {

    public ReachabilityAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {
      ReachabilityQuestion question = (ReachabilityQuestion) _question;
      ReachabilityType type = question.getReachabilityType();
      switch (type) {
        case MULTIPATH:
          return multipath(question);
        case STANDARD:
          return standard(question);
        case PATH_DIFF:
        case REDUCED_REACHABILITY:
        case INCREASED:
        case MULTIPATH_DIFF:
        default:
          throw new BatfishException(
              "Unsupported non-differential reachabilty type: " + type.reachabilityTypeName());
      }
    }

    @Override
    public AnswerElement answerDiff() {
      ReachabilityQuestion question = (ReachabilityQuestion) _question;
      ReachabilityType type = question.getReachabilityType();
      switch (type) {
        case PATH_DIFF:
          return pathDiff(question);
        case REDUCED_REACHABILITY:
          return reducedReachability(question);
        case STANDARD:
        case MULTIPATH:
        case INCREASED:
        case MULTIPATH_DIFF:
        default:
          throw new BatfishException(
              "Unsupported differential reachabilty type: " + type.reachabilityTypeName());
      }
    }

    private AnswerElement multipath(ReachabilityQuestion question) {
      return _batfish.multipath(question.getHeaderSpace());
    }

    private AnswerElement pathDiff(ReachabilityQuestion question) {
      return _batfish.pathDiff(question.getHeaderSpace());
    }

    private AnswerElement reducedReachability(ReachabilityQuestion question) {
      return _batfish.reducedReachability(question.getHeaderSpace());
    }

    private AnswerElement standard(ReachabilityQuestion question) {
      return _batfish.standard(
          question.getHeaderSpace(),
          question.getActions(),
          question.getIngressNodeRegex(),
          question.getNotIngressNodeRegex(),
          question.getFinalNodeRegex(),
          question.getNotFinalNodeRegex(),
          question.getTransitNodes(),
          question.getNotTransitNodes());
    }
  }

  // <question_page_comment>

  /**
   * Details coming.
   *
   * <p>More details coming.
   *
   * @type Reachability dataplane
   * @param transitNodes set of transit nodes (packet must transit through all of them)
   * @param notTransitNodes set of non-transit nodes (packet does not transit through any of them)
   * @param DetailsComing Details coming.
   * @example bf_answer("Reachability", dstIps=["2.128.0.101"], dstPorts=[53], ipProtocols=["UDP"],
   *     actions=["drop"]) Finds all (starting node, packet header) combinations that cannot reach
   *     (action=drop) the 2.128.0.101 using a DNS (UDP on port 53) packet.
   * @example bf_answer_type("Reachability", actions=["ACCEPT"], dstIps=["2.128.1.101"],
   *     notDstPorts=[22], notIpProtocols=["TCP"]) Finds all (starting node, packet header)
   *     combinations that can reach (action=drop) 2.128.1.101 using non-SSH packets.
   */
  public static class ReachabilityQuestion extends Question implements IReachabilityQuestion {

    private static final String PROP_ACTIONS = "actions";

    private static final String DEFAULT_FINAL_NODE_REGEX = ".*";

    private static final String DEFAULT_INGRESS_NODE_REGEX = ".*";

    private static final String DEFAULT_NOT_FINAL_NODE_REGEX = "";

    private static final String DEFAULT_NOT_INGRESS_NODE_REGEX = "";

    private static final SortedSet<String> DEFAULT_TRANSIT_NODES =
        Collections.<String>emptySortedSet();

    private static final SortedSet<String> DEFAULT_NOT_TRANSIT_NODES =
        Collections.<String>emptySortedSet();

    private static final String PROP_DST_IPS = "dstIps";

    private static final String PROP_DST_PORTS = "dstPorts";

    private static final String PROP_DST_PROTOCOLS = "dstProtocols";

    private static final String PROP_FINAL_NODE_REGEX = "finalNodeRegex";

    private static final String PROP_FRAGMENT_OFFSETS = "fragmentOffsets";

    private static final String PROP_ICMP_CODES = "icmpCodes";

    private static final String PROP_ICMP_TYPES = "icmpTypes";

    private static final String PROP_INGRESS_NODE_REGEX = "ingressNodeRegex";

    private static final String PROP_IP_PROTOCOLS = "ipProtocols";

    private static final String PROP_NEGATE_HEADER = "negateHeader";

    private static final String PROP_NOT_DST_IPS = "notDstIps";

    private static final String PROP_NOT_DST_PORTS = "notDstPorts";

    private static final String PROP_NOT_DST_PROTOCOLS = "notDstProtocols";

    private static final String PROP_NOT_FINAL_NODE_REGEX = "notFinalNodeRegex";

    private static final String PROP_NOT_FRAGMENT_OFFSETS = "notFragmentOffsets";

    private static final String PROP_NOT_ICMP_CODES = "notIcmpCodes";

    private static final String PROP_NOT_ICMP_TYPES = "notIcmpTypes";

    private static final String PROP_NOT_INGRESS_NODE_REGEX = "notIngressNodeRegex";

    private static final String PROP_NOT_IP_PROTOCOLS = "notIpProtocols";

    private static final String PROP_NOT_PACKET_LENGTHS = "notPacketLengths";

    private static final String PROP_NOT_SRC_IPS = "notSrcIps";

    private static final String PROP_NOT_SRC_PORTS = "notSrcPorts";

    private static final String PROP_NOT_SRC_PROTOCOLS = "notSrcProtocols";

    private static final String PROP_PACKET_LENGTHS = "packetLengths";

    private static final String PROP_REACHABILITY_TYPE = "type";

    private static final String PROP_SRC_IPS = "srcIps";

    private static final String PROP_SRC_OR_DST_IPS = "srcOrDstIps";

    private static final String PROP_SRC_OR_DST_PORTS = "srcOrDstPorts";

    private static final String PROP_SRC_OR_DST_PROTOCOLS = "srcOrDstProtocols";

    private static final String PROP_SRC_PORTS = "srcPorts";

    private static final String PROP_SRC_PROTOCOLS = "srcProtocols";

    private static final String PROP_TRANSIT_NODES = "transitNodes";

    private static final String PROP_NOT_TRANSIT_NODES = "notTransitNodes";

    private SortedSet<ForwardingAction> _actions;

    private String _finalNodeRegex;

    private final HeaderSpace _headerSpace;

    private String _ingressNodeRegex;

    private String _notFinalNodeRegex;

    private String _notIngressNodeRegex;

    private SortedSet<String> _transitNodes;

    private SortedSet<String> _notTransitNodes;

    private ReachabilityType _reachabilityType;

    public ReachabilityQuestion() {
      _actions = new TreeSet<>(Collections.singleton(ForwardingAction.ACCEPT));
      _finalNodeRegex = DEFAULT_FINAL_NODE_REGEX;
      _headerSpace = new HeaderSpace();
      _ingressNodeRegex = DEFAULT_INGRESS_NODE_REGEX;
      _reachabilityType = ReachabilityType.STANDARD;
      _notFinalNodeRegex = DEFAULT_NOT_FINAL_NODE_REGEX;
      _notIngressNodeRegex = DEFAULT_NOT_INGRESS_NODE_REGEX;
      _transitNodes = DEFAULT_TRANSIT_NODES;
      _notTransitNodes = DEFAULT_NOT_TRANSIT_NODES;
    }

    @JsonProperty(PROP_ACTIONS)
    public SortedSet<ForwardingAction> getActions() {
      return _actions;
    }

    @Override
    public boolean getDataPlane() {
      return true;
    }

    @JsonProperty(PROP_DST_IPS)
    public SortedSet<IpWildcard> getDstIps() {
      return _headerSpace.getDstIps();
    }

    @JsonProperty(PROP_DST_PORTS)
    public SortedSet<SubRange> getDstPorts() {
      return _headerSpace.getDstPorts();
    }

    @JsonProperty(PROP_DST_PROTOCOLS)
    public SortedSet<Protocol> getDstProtocols() {
      return _headerSpace.getDstProtocols();
    }

    @JsonProperty(PROP_FINAL_NODE_REGEX)
    public String getFinalNodeRegex() {
      return _finalNodeRegex;
    }

    @JsonProperty(PROP_FRAGMENT_OFFSETS)
    public SortedSet<SubRange> getFragmentOffsets() {
      return _headerSpace.getFragmentOffsets();
    }

    @JsonIgnore
    public HeaderSpace getHeaderSpace() {
      return _headerSpace;
    }

    @JsonProperty(PROP_ICMP_CODES)
    public SortedSet<SubRange> getIcmpCodes() {
      return _headerSpace.getIcmpCodes();
    }

    @JsonProperty(PROP_ICMP_TYPES)
    public SortedSet<SubRange> getIcmpTypes() {
      return _headerSpace.getIcmpTypes();
    }

    @JsonProperty(PROP_INGRESS_NODE_REGEX)
    public String getIngressNodeRegex() {
      return _ingressNodeRegex;
    }

    @JsonProperty(PROP_IP_PROTOCOLS)
    public SortedSet<IpProtocol> getIpProtocols() {
      return _headerSpace.getIpProtocols();
    }

    @Override
    public String getName() {
      return "reachability";
    }

    @JsonProperty(PROP_NEGATE_HEADER)
    public boolean getNegateHeader() {
      return _headerSpace.getNegate();
    }

    @JsonProperty(PROP_NOT_DST_IPS)
    public SortedSet<IpWildcard> getNotDstIps() {
      return _headerSpace.getNotDstIps();
    }

    @JsonProperty(PROP_NOT_DST_PORTS)
    public SortedSet<SubRange> getNotDstPorts() {
      return _headerSpace.getNotDstPorts();
    }

    @JsonProperty(PROP_NOT_DST_PROTOCOLS)
    public SortedSet<Protocol> getNotDstProtocols() {
      return _headerSpace.getNotDstProtocols();
    }

    @JsonProperty(PROP_NOT_FINAL_NODE_REGEX)
    public String getNotFinalNodeRegex() {
      return _notFinalNodeRegex;
    }

    @JsonProperty(PROP_NOT_FRAGMENT_OFFSETS)
    private SortedSet<SubRange> getNotFragmentOffsets() {
      return _headerSpace.getNotFragmentOffsets();
    }

    @JsonProperty(PROP_NOT_ICMP_CODES)
    public SortedSet<SubRange> getNotIcmpCodes() {
      return _headerSpace.getNotIcmpCodes();
    }

    @JsonProperty(PROP_NOT_ICMP_TYPES)
    public SortedSet<SubRange> getNotIcmpTypes() {
      return _headerSpace.getNotIcmpTypes();
    }

    @JsonProperty(PROP_NOT_INGRESS_NODE_REGEX)
    public String getNotIngressNodeRegex() {
      return _notIngressNodeRegex;
    }

    @JsonProperty(PROP_TRANSIT_NODES)
    public SortedSet<String> getTransitNodes() {
      return _transitNodes;
    }

    @JsonProperty(PROP_NOT_TRANSIT_NODES)
    public SortedSet<String> getNotTransitNodes() {
      return _notTransitNodes;
    }

    @JsonProperty(PROP_NOT_IP_PROTOCOLS)
    public SortedSet<IpProtocol> getNotIpProtocols() {
      return _headerSpace.getNotIpProtocols();
    }

    @JsonProperty(PROP_NOT_PACKET_LENGTHS)
    public SortedSet<SubRange> getNotPacketLengths() {
      return _headerSpace.getNotPacketLengths();
    }

    @JsonProperty(PROP_NOT_SRC_IPS)
    public SortedSet<IpWildcard> getNotSrcIps() {
      return _headerSpace.getNotSrcIps();
    }

    @JsonProperty(PROP_NOT_SRC_PORTS)
    public SortedSet<SubRange> getNotSrcPorts() {
      return _headerSpace.getNotSrcPorts();
    }

    @JsonProperty(PROP_NOT_SRC_PROTOCOLS)
    public SortedSet<Protocol> getNotSrcProtocols() {
      return _headerSpace.getNotSrcProtocols();
    }

    @JsonProperty(PROP_PACKET_LENGTHS)
    public SortedSet<SubRange> getPacketLengths() {
      return _headerSpace.getPacketLengths();
    }

    @JsonProperty(PROP_REACHABILITY_TYPE)
    public ReachabilityType getReachabilityType() {
      return _reachabilityType;
    }

    @JsonProperty(PROP_SRC_IPS)
    public SortedSet<IpWildcard> getSrcIps() {
      return _headerSpace.getSrcIps();
    }

    @JsonProperty(PROP_SRC_OR_DST_IPS)
    public SortedSet<IpWildcard> getSrcOrDstIps() {
      return _headerSpace.getSrcOrDstIps();
    }

    @JsonProperty(PROP_SRC_OR_DST_PORTS)
    public SortedSet<SubRange> getSrcOrDstPorts() {
      return _headerSpace.getSrcOrDstPorts();
    }

    @JsonProperty(PROP_SRC_OR_DST_PROTOCOLS)
    public SortedSet<Protocol> getSrcOrDstProtocols() {
      return _headerSpace.getSrcOrDstProtocols();
    }

    @JsonProperty(PROP_SRC_PORTS)
    public SortedSet<SubRange> getSrcPorts() {
      return _headerSpace.getSrcPorts();
    }

    @JsonProperty(PROP_SRC_PROTOCOLS)
    public SortedSet<Protocol> getSrcProtocols() {
      return _headerSpace.getSrcProtocols();
    }

    @Override
    public boolean getTraffic() {
      return true;
    }

    @Override
    public String prettyPrint() {
      try {
        String retString = String.format("reachability %sactions=%s", prettyPrintBase(), _actions);
        // we only print "interesting" values
        if (_reachabilityType != ReachabilityType.STANDARD) {
          retString += String.format(", %s=%s", PROP_REACHABILITY_TYPE, _reachabilityType);
        }
        if (getNegateHeader()) {
          retString += ", negateHeader=true";
        }
        if (getDstIps() != null && !getDstIps().isEmpty()) {
          retString += String.format(", %s=%s", PROP_DST_IPS, getDstIps());
        }
        if (getDstPorts() != null && !getDstPorts().isEmpty()) {
          retString += String.format(", %s=%s", PROP_DST_PORTS, getDstPorts());
        }
        if (getDstProtocols() != null && !getDstProtocols().isEmpty()) {
          retString += String.format(", %s=%s", PROP_DST_PROTOCOLS, getDstProtocols());
        }
        if (!_finalNodeRegex.equals(DEFAULT_FINAL_NODE_REGEX)) {
          retString += String.format(", %s=%s", PROP_FINAL_NODE_REGEX, _finalNodeRegex);
        }
        if (getFragmentOffsets() != null && !getFragmentOffsets().isEmpty()) {
          retString += String.format(", %s=%s", PROP_FRAGMENT_OFFSETS, getFragmentOffsets());
        }
        if (getIcmpCodes() != null && !getIcmpCodes().isEmpty()) {
          retString += String.format(", %s=%s", PROP_ICMP_CODES, getIcmpCodes());
        }
        if (getIcmpTypes() != null && !getIcmpTypes().isEmpty()) {
          retString += String.format(", %s=%s", PROP_ICMP_TYPES, getIcmpTypes());
        }
        if (!_ingressNodeRegex.equals(DEFAULT_INGRESS_NODE_REGEX)) {
          retString += String.format(", %s=%s", PROP_INGRESS_NODE_REGEX, _ingressNodeRegex);
        }
        if (getIpProtocols() != null && !getIpProtocols().isEmpty()) {
          retString += String.format(", %s=%s", PROP_IP_PROTOCOLS, getIpProtocols());
        }
        if (getPacketLengths() != null && !getPacketLengths().isEmpty()) {
          retString += String.format(", %s=%s", PROP_PACKET_LENGTHS, getPacketLengths());
        }
        if (getSrcIps() != null && !getSrcIps().isEmpty()) {
          retString += String.format(", %s=%s", PROP_SRC_IPS, getSrcIps());
        }
        if (getSrcPorts() != null && !getSrcPorts().isEmpty()) {
          retString += String.format(", %s=%s", PROP_SRC_PORTS, getSrcPorts());
        }
        if (getSrcProtocols() != null && !getSrcProtocols().isEmpty()) {
          retString += String.format(", %s=%s", PROP_SRC_PROTOCOLS, getSrcProtocols());
        }
        if (getSrcOrDstIps() != null && !getSrcOrDstIps().isEmpty()) {
          retString += String.format(", %s=%s", PROP_SRC_OR_DST_IPS, getSrcOrDstIps());
        }
        if (getSrcOrDstPorts() != null && !getSrcOrDstPorts().isEmpty()) {
          retString += String.format(", %s=%s", PROP_SRC_OR_DST_PORTS, getSrcOrDstPorts());
        }
        if (getSrcOrDstProtocols() != null && !getSrcOrDstProtocols().isEmpty()) {
          retString += String.format(", %s=%s", PROP_SRC_OR_DST_PROTOCOLS, getSrcOrDstProtocols());
        }
        if (_transitNodes != null && !_transitNodes.isEmpty()) {
          retString += String.format(", %s=%s", PROP_TRANSIT_NODES, _transitNodes);
        }
        if (getNotDstIps() != null && !getNotDstIps().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_DST_IPS, getNotDstIps());
        }
        if (getNotDstPorts() != null && !getNotDstPorts().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_DST_PORTS, getNotDstPorts());
        }
        if (getNotDstProtocols() != null && !getNotDstProtocols().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_DST_PROTOCOLS, getNotDstProtocols());
        }
        if (!_notFinalNodeRegex.equals(DEFAULT_NOT_FINAL_NODE_REGEX)) {
          retString += String.format(", %s=%s", PROP_NOT_FINAL_NODE_REGEX, _notFinalNodeRegex);
        }
        if (getNotFragmentOffsets() != null && !getNotFragmentOffsets().isEmpty()) {
          retString +=
              String.format(", %s=%s", PROP_NOT_FRAGMENT_OFFSETS, getNotFragmentOffsets());
        }
        if (getNotIcmpCodes() != null && !getNotIcmpCodes().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_ICMP_CODES, getNotIcmpCodes());
        }
        if (getNotIcmpTypes() != null && !getNotIcmpTypes().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_ICMP_TYPES, getNotIcmpTypes());
        }
        if (!_notIngressNodeRegex.equals(DEFAULT_NOT_INGRESS_NODE_REGEX)) {
          retString += String.format(", %s=%s", PROP_NOT_INGRESS_NODE_REGEX, _notIngressNodeRegex);
        }
        if (getNotIpProtocols() != null && !getNotIpProtocols().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_IP_PROTOCOLS, getNotIpProtocols());
        }
        if (getNotPacketLengths() != null && !getNotPacketLengths().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_PACKET_LENGTHS, getNotPacketLengths());
        }
        if (getNotSrcIps() != null && !getNotSrcIps().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_SRC_IPS, getNotSrcIps());
        }
        if (getNotSrcPorts() != null && !getNotSrcPorts().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_SRC_PORTS, getNotSrcPorts());
        }
        if (getNotSrcProtocols() != null && !getNotSrcProtocols().isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_SRC_PROTOCOLS, getNotSrcProtocols());
        }
        if (_notTransitNodes != null && !_notTransitNodes.isEmpty()) {
          retString += String.format(", %s=%s", PROP_NOT_TRANSIT_NODES, _notTransitNodes);
        }
        return retString;
      } catch (Exception e) {
        try {
          return "Pretty printing failed. Printing Json\n" + toJsonString();
        } catch (BatfishException e1) {
          throw new BatfishException("Both pretty and json printing failed\n");
        }
      }
    }

    @Override
    @JsonProperty(PROP_ACTIONS)
    public void setActions(SortedSet<ForwardingAction> actionSet) {
      _actions = new TreeSet<>(actionSet);
    }

    @Override
    @JsonProperty(PROP_DST_IPS)
    public void setDstIps(SortedSet<IpWildcard> dstIps) {
      _headerSpace.setDstIps(new TreeSet<>(dstIps));
    }

    @JsonProperty(PROP_DST_PORTS)
    public void setDstPorts(SortedSet<SubRange> dstPorts) {
      _headerSpace.setDstPorts(new TreeSet<>(dstPorts));
    }

    @Override
    @JsonProperty(PROP_DST_PROTOCOLS)
    public void setDstProtocols(SortedSet<Protocol> dstProtocols) {
      _headerSpace.setDstProtocols(new TreeSet<>(dstProtocols));
    }

    @JsonProperty(PROP_FINAL_NODE_REGEX)
    public void setFinalNodeRegex(String regex) {
      _finalNodeRegex = regex;
    }

    @JsonProperty(PROP_TRANSIT_NODES)
    public void setTransitNodes(SortedSet<String> transitNodes) {
      _transitNodes = transitNodes;
    }

    @JsonProperty(PROP_NOT_TRANSIT_NODES)
    public void setNotTransitNodes(SortedSet<String> notTransitNodes) {
      _notTransitNodes = notTransitNodes;
    }

    @JsonProperty(PROP_ICMP_CODES)
    public void setIcmpCodes(SortedSet<SubRange> icmpCodes) {
      _headerSpace.setIcmpCodes(new TreeSet<>(icmpCodes));
    }

    @JsonProperty(PROP_ICMP_TYPES)
    public void setIcmpTypes(SortedSet<SubRange> icmpTypes) {
      _headerSpace.setIcmpTypes(new TreeSet<>(icmpTypes));
    }

    @Override
    @JsonProperty(PROP_INGRESS_NODE_REGEX)
    public void setIngressNodeRegex(String regex) {
      _ingressNodeRegex = regex;
    }

    @JsonProperty(PROP_IP_PROTOCOLS)
    public void setIpProtocols(SortedSet<IpProtocol> ipProtocols) {
      _headerSpace.setIpProtocols(ipProtocols);
    }

    @JsonProperty(PROP_NEGATE_HEADER)
    public void setNegateHeader(boolean negateHeader) {
      _headerSpace.setNegate(negateHeader);
    }

    @JsonProperty(PROP_NOT_DST_IPS)
    public void setNotDstIps(SortedSet<IpWildcard> notDstIps) {
      _headerSpace.setNotDstIps(new TreeSet<>(notDstIps));
    }

    @JsonProperty(PROP_NOT_DST_PORTS)
    public void setNotDstPorts(SortedSet<SubRange> notDstPorts) {
      _headerSpace.setNotDstPorts(new TreeSet<>(notDstPorts));
    }

    @Override
    @JsonProperty(PROP_NOT_DST_PROTOCOLS)
    public void setNotDstProtocols(SortedSet<Protocol> notDstProtocols) {
      _headerSpace.setNotDstProtocols(new TreeSet<>(notDstProtocols));
    }

    @JsonProperty(PROP_NOT_FINAL_NODE_REGEX)
    public void setNotFinalNodeRegex(String notFinalNodeRegex) {
      _notFinalNodeRegex = notFinalNodeRegex;
    }

    @JsonProperty(PROP_NOT_ICMP_CODES)
    public void setNotIcmpCodes(SortedSet<SubRange> notIcmpCodes) {
      _headerSpace.setNotIcmpCodes(new TreeSet<>(notIcmpCodes));
    }

    @JsonProperty(PROP_NOT_ICMP_TYPES)
    public void setNotIcmpTypes(SortedSet<SubRange> notIcmpType) {
      _headerSpace.setNotIcmpTypes(new TreeSet<>(notIcmpType));
    }

    @JsonProperty(PROP_NOT_INGRESS_NODE_REGEX)
    public void setNotIngressNodeRegex(String notIngressNodeRegex) {
      _notIngressNodeRegex = notIngressNodeRegex;
    }

    @JsonProperty(PROP_NOT_IP_PROTOCOLS)
    public void setNotIpProtocols(SortedSet<IpProtocol> notIpProtocols) {
      _headerSpace.setNotIpProtocols(notIpProtocols);
    }

    @JsonProperty(PROP_NOT_PACKET_LENGTHS)
    public void setNotPacketLengths(SortedSet<SubRange> notPacketLengths) {
      _headerSpace.setNotPacketLengths(new TreeSet<>(notPacketLengths));
    }

    @JsonProperty(PROP_NOT_SRC_IPS)
    public void setNotSrcIps(SortedSet<IpWildcard> notSrcIps) {
      _headerSpace.setNotSrcIps(new TreeSet<>(notSrcIps));
    }

    @JsonProperty(PROP_NOT_SRC_PORTS)
    public void setNotSrcPortRange(SortedSet<SubRange> notSrcPorts) {
      _headerSpace.setNotSrcPorts(new TreeSet<>(notSrcPorts));
    }

    @JsonProperty(PROP_NOT_SRC_PROTOCOLS)
    public void setNotSrcProtocols(SortedSet<Protocol> notSrcProtocols) {
      _headerSpace.setNotSrcProtocols(new TreeSet<>(notSrcProtocols));
    }

    @JsonProperty(PROP_PACKET_LENGTHS)
    public void setPacketLengths(SortedSet<SubRange> packetLengths) {
      _headerSpace.setPacketLengths(new TreeSet<>(packetLengths));
    }

    @JsonProperty(PROP_REACHABILITY_TYPE)
    public void setReachabilityType(ReachabilityType reachabilityType) {
      _reachabilityType = reachabilityType;
      switch (reachabilityType) {
        case INCREASED:
        case MULTIPATH_DIFF:
        case PATH_DIFF:
        case REDUCED_REACHABILITY:
          setDifferential(true);
          break;
        case MULTIPATH:
        case STANDARD:
          setDifferential(false);
          break;
        default:
          throw new BatfishException(
              "Invalid reachability type: " + reachabilityType.reachabilityTypeName());
      }
    }

    @JsonProperty(PROP_SRC_IPS)
    public void setSrcIps(SortedSet<IpWildcard> srcIps) {
      _headerSpace.setSrcIps(new TreeSet<>(srcIps));
    }

    @JsonProperty(PROP_SRC_OR_DST_IPS)
    public void setSrcOrDstIps(SortedSet<IpWildcard> srcOrDstIps) {
      _headerSpace.setSrcOrDstIps(new TreeSet<>(srcOrDstIps));
    }

    @JsonProperty(PROP_SRC_OR_DST_PORTS)
    public void setSrcOrDstPorts(SortedSet<SubRange> srcOrDstPorts) {
      _headerSpace.setSrcOrDstPorts(new TreeSet<>(srcOrDstPorts));
    }

    @JsonProperty(PROP_SRC_OR_DST_PROTOCOLS)
    public void setSrcOrDstProtocols(SortedSet<Protocol> srcOrDstProtocols) {
      _headerSpace.setSrcOrDstProtocols(new TreeSet<>(srcOrDstProtocols));
    }

    @JsonProperty(PROP_SRC_PORTS)
    public void setSrcPorts(SortedSet<SubRange> srcPorts) {
      _headerSpace.setSrcPorts(new TreeSet<>(srcPorts));
    }

    @JsonProperty(PROP_SRC_PROTOCOLS)
    public void setSrcProtocols(SortedSet<Protocol> srcProtocols) {
      _headerSpace.setSrcProtocols(new TreeSet<>(srcProtocols));
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new ReachabilityAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new ReachabilityQuestion();
  }
}
