package org.batfish.datamodel.acl;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.Traverser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.AclIpSpaceLine;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceMetadata;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Protocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.visitors.IpSpaceDescriber;
import org.batfish.datamodel.visitors.IpSpaceTracer;

/**
 * Tracer for {@link IpAccessList}, {@link IpSpace}, {@link HeaderSpace}.<br>
 * Acts like {@link Evaluator} on {@link IpAccessList}, except that it introduces tracing when
 * encountering traceable classes.
 */
public final class AclTracer extends Evaluator {

  public static AclTrace trace(
      @Nonnull IpAccessList ipAccessList,
      @Nonnull Flow flow,
      @Nullable String srcInterface,
      @Nonnull Map<String, IpAccessList> availableAcls,
      @Nonnull Map<String, IpSpace> namedIpSpaces,
      @Nonnull Map<String, IpSpaceMetadata> namedIpSpaceMetadata) {
    AclTracer tracer =
        new AclTracer(flow, srcInterface, availableAcls, namedIpSpaces, namedIpSpaceMetadata);
    tracer.trace(ipAccessList);
    return tracer.getTrace();
  }

  private final Map<IpSpace, IpSpaceMetadata> _ipSpaceMetadata;

  private final Map<IpSpace, String> _ipSpaceNames;

  private final TraceEventNode _traceRoot;

  private TraceEventNode _currentTreeNode;

  public AclTracer(
      @Nonnull Flow flow,
      @Nullable String srcInterface,
      @Nonnull Map<String, IpAccessList> availableAcls,
      @Nonnull Map<String, IpSpace> namedIpSpaces,
      @Nonnull Map<String, IpSpaceMetadata> namedIpSpaceMetadata) {
    super(flow, srcInterface, availableAcls, namedIpSpaces);
    _ipSpaceNames = new IdentityHashMap<>();
    _ipSpaceMetadata = new IdentityHashMap<>();
    _traceRoot = TraceEventNode.withParent(null);
    _currentTreeNode = _traceRoot;
    namedIpSpaces.forEach((name, ipSpace) -> _ipSpaceNames.put(ipSpace, name));
    namedIpSpaceMetadata.forEach(
        (name, ipSpaceMetadata) -> _ipSpaceMetadata.put(namedIpSpaces.get(name), ipSpaceMetadata));
  }

  private String computeLineDescription(AclIpSpaceLine line, IpSpaceDescriber describer) {
    String srcText = line.getSrcText();
    if (srcText != null) {
      return srcText;
    }
    return line.getIpSpace().accept(describer);
  }

  public Flow getFlow() {
    return _flow;
  }

  public Map<IpSpace, IpSpaceMetadata> getIpSpaceMetadata() {
    return _ipSpaceMetadata;
  }

  public @Nonnull Map<IpSpace, String> getIpSpaceNames() {
    return _ipSpaceNames;
  }

  public @Nonnull Map<String, IpSpace> getNamedIpSpaces() {
    return _namedIpSpaces;
  }

  public @Nonnull AclTrace getTrace() {
    return new AclTrace(
        ImmutableList.copyOf(
                Traverser.forTree(TraceEventNode::getChildren).depthFirstPreOrder(_traceRoot))
            .stream()
            .map(TraceEventNode::getEvent)
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList()));
  }

  public void recordAction(
      @Nonnull IpAccessList ipAccessList, int index, @Nonnull IpAccessListLine line) {
    String lineDescription = firstNonNull(line.getName(), line.toString());
    String type = firstNonNull(ipAccessList.getSourceType(), "filter");
    String name = firstNonNull(ipAccessList.getSourceName(), ipAccessList.getName());
    String actionStr = line.getAction() == LineAction.PERMIT ? "permitted" : "denied";
    String description =
        String.format(
            "Flow %s by %s named %s, index %d: %s", actionStr, type, name, index, lineDescription);
    if (line.getAction() == LineAction.PERMIT) {
      _currentTreeNode.setEvent(
          new PermittedByIpAccessListLine(
              description, index, lineDescription, ipAccessList.getName()));
    } else {
      _currentTreeNode.setEvent(
          new DeniedByIpAccessListLine(
              description, index, lineDescription, ipAccessList.getName()));
    }
  }

  public void recordAction(
      @Nonnull String aclIpSpaceName,
      @Nullable IpSpaceMetadata ipSpaceMetadata,
      int index,
      @Nonnull AclIpSpaceLine line,
      Ip ip,
      String ipDescription,
      IpSpaceDescriber describer) {
    if (line.getAction() == LineAction.PERMIT) {
      _currentTreeNode.setEvent(
          new PermittedByAclIpSpaceLine(
              aclIpSpaceName,
              ipSpaceMetadata,
              index,
              computeLineDescription(line, describer),
              ip,
              ipDescription));
    } else {
      _currentTreeNode.setEvent(
          new DeniedByAclIpSpaceLine(
              aclIpSpaceName,
              ipSpaceMetadata,
              index,
              computeLineDescription(line, describer),
              ip,
              ipDescription));
    }
  }

  public void recordDefaultDeny(@Nonnull IpAccessList ipAccessList) {
    _currentTreeNode.setEvent(
        new DefaultDeniedByIpAccessList(
            ipAccessList.getName(), ipAccessList.getSourceName(), ipAccessList.getSourceType()));
  }

  public void recordDefaultDeny(
      @Nonnull String aclIpSpaceName,
      @Nullable IpSpaceMetadata ipSpaceMetadata,
      Ip ip,
      String ipDescription) {
    _currentTreeNode.setEvent(
        new DefaultDeniedByAclIpSpace(aclIpSpaceName, ip, ipDescription, ipSpaceMetadata));
  }

  private static boolean rangesContain(Collection<SubRange> ranges, @Nullable Integer num) {
    return num != null && ranges.stream().anyMatch(sr -> sr.includes(num));
  }

  public void recordNamedIpSpaceAction(
      @Nonnull String name,
      @Nonnull String ipSpaceDescription,
      IpSpaceMetadata ipSpaceMetadata,
      boolean permit,
      Ip ip,
      String ipDescription) {
    if (permit) {
      _currentTreeNode.setEvent(
          new PermittedByNamedIpSpace(
              ip, ipDescription, ipSpaceDescription, ipSpaceMetadata, name));
    } else {
      _currentTreeNode.setEvent(
          new DeniedByNamedIpSpace(ip, ipDescription, ipSpaceDescription, ipSpaceMetadata, name));
    }
  }

  private boolean trace(@Nonnull HeaderSpace headerSpace) {
    if (!headerSpace.getDscps().isEmpty() && !headerSpace.getDscps().contains(_flow.getDscp())) {
      return false;
    }
    if (!headerSpace.getNotDscps().isEmpty()
        && headerSpace.getNotDscps().contains(_flow.getDscp())) {
      return false;
    }
    if (headerSpace.getDstIps() != null && !traceDstIp(headerSpace.getDstIps(), _flow.getDstIp())) {
      return false;
    }
    if (headerSpace.getNotDstIps() != null
        && traceDstIp(headerSpace.getNotDstIps(), _flow.getDstIp())) {
      return false;
    }
    if (!headerSpace.getDstPorts().isEmpty()
        && !rangesContain(headerSpace.getDstPorts(), _flow.getDstPort())) {
      return false;
    }
    if (!headerSpace.getNotDstPorts().isEmpty()
        && rangesContain(headerSpace.getNotDstPorts(), _flow.getDstPort())) {
      return false;
    }
    if (!headerSpace.getDstProtocols().isEmpty()) {
      boolean match = false;
      for (Protocol dstProtocol : headerSpace.getDstProtocols()) {
        if (dstProtocol.getIpProtocol().equals(_flow.getIpProtocol())) {
          match = true;
          Integer dstPort = dstProtocol.getPort();
          if (!dstPort.equals(_flow.getDstPort())) {
            match = false;
          }
          if (match) {
            break;
          }
        }
      }
      if (!match) {
        return false;
      }
    }
    if (!headerSpace.getNotDstProtocols().isEmpty()) {
      boolean match = false;
      for (Protocol notDstProtocol : headerSpace.getNotDstProtocols()) {
        if (notDstProtocol.getIpProtocol().equals(_flow.getIpProtocol())) {
          match = true;
          Integer dstPort = notDstProtocol.getPort();
          if (!dstPort.equals(_flow.getDstPort())) {
            match = false;
          }
          if (match) {
            return false;
          }
        }
      }
    }
    if (!headerSpace.getFragmentOffsets().isEmpty()
        && !rangesContain(headerSpace.getFragmentOffsets(), _flow.getFragmentOffset())) {
      return false;
    }
    if (!headerSpace.getNotFragmentOffsets().isEmpty()
        && rangesContain(headerSpace.getNotFragmentOffsets(), _flow.getFragmentOffset())) {
      return false;
    }
    if (!headerSpace.getIcmpCodes().isEmpty()
        && !rangesContain(headerSpace.getIcmpCodes(), _flow.getIcmpCode())) {
      return false;
    }
    if (!headerSpace.getNotIcmpCodes().isEmpty()
        && rangesContain(headerSpace.getNotIcmpCodes(), _flow.getFragmentOffset())) {
      return false;
    }
    if (!headerSpace.getIcmpTypes().isEmpty()
        && !rangesContain(headerSpace.getIcmpTypes(), _flow.getIcmpType())) {
      return false;
    }
    if (!headerSpace.getNotIcmpTypes().isEmpty()
        && rangesContain(headerSpace.getNotIcmpTypes(), _flow.getFragmentOffset())) {
      return false;
    }
    if (!headerSpace.getIpProtocols().isEmpty()
        && !headerSpace.getIpProtocols().contains(_flow.getIpProtocol())) {
      return false;
    }
    if (!headerSpace.getNotIpProtocols().isEmpty()
        && headerSpace.getNotIpProtocols().contains(_flow.getIpProtocol())) {
      return false;
    }
    if (!headerSpace.getPacketLengths().isEmpty()
        && !rangesContain(headerSpace.getPacketLengths(), _flow.getPacketLength())) {
      return false;
    }
    if (!headerSpace.getNotPacketLengths().isEmpty()
        && rangesContain(headerSpace.getNotPacketLengths(), _flow.getPacketLength())) {
      return false;
    }
    if (headerSpace.getSrcOrDstIps() != null
        && !(traceSrcIp(headerSpace.getSrcOrDstIps(), _flow.getSrcIp())
            || traceDstIp(headerSpace.getSrcOrDstIps(), _flow.getDstIp()))) {
      return false;
    }
    if (!headerSpace.getSrcOrDstPorts().isEmpty()
        && !(rangesContain(headerSpace.getSrcOrDstPorts(), _flow.getSrcPort())
            || rangesContain(headerSpace.getSrcOrDstPorts(), _flow.getDstPort()))) {
      return false;
    }
    if (!headerSpace.getSrcOrDstProtocols().isEmpty()) {
      boolean match = false;
      for (Protocol protocol : headerSpace.getSrcOrDstProtocols()) {
        if (protocol.getIpProtocol().equals(_flow.getIpProtocol())) {
          match = true;
          Integer port = protocol.getPort();
          if (!port.equals(_flow.getDstPort()) && !port.equals(_flow.getSrcPort())) {
            match = false;
          }
          if (match) {
            break;
          }
        }
      }
      if (!match) {
        return false;
      }
    }
    if (headerSpace.getSrcIps() != null && !traceSrcIp(headerSpace.getSrcIps(), _flow.getSrcIp())) {
      return false;
    }
    if (headerSpace.getNotSrcIps() != null
        && traceSrcIp(headerSpace.getNotSrcIps(), _flow.getSrcIp())) {
      return false;
    }
    if (!headerSpace.getSrcPorts().isEmpty()
        && !rangesContain(headerSpace.getSrcPorts(), _flow.getSrcPort())) {
      return false;
    }
    if (!headerSpace.getNotSrcPorts().isEmpty()
        && rangesContain(headerSpace.getNotSrcPorts(), _flow.getSrcPort())) {
      return false;
    }
    if (!headerSpace.getSrcProtocols().isEmpty()) {
      boolean match = false;
      for (Protocol srcProtocol : headerSpace.getSrcProtocols()) {
        if (srcProtocol.getIpProtocol().equals(_flow.getIpProtocol())) {
          match = true;
          Integer srcPort = srcProtocol.getPort();
          if (!srcPort.equals(_flow.getSrcPort())) {
            match = false;
          }
          if (match) {
            break;
          }
        }
      }
      if (!match) {
        return false;
      }
    }
    if (!headerSpace.getNotSrcProtocols().isEmpty()) {
      boolean match = false;
      for (Protocol notSrcProtocol : headerSpace.getNotSrcProtocols()) {
        if (notSrcProtocol.getIpProtocol().equals(_flow.getIpProtocol())) {
          match = true;
          Integer srcPort = notSrcProtocol.getPort();
          if (!srcPort.equals(_flow.getSrcPort())) {
            match = false;
          }
          if (match) {
            return false;
          }
        }
      }
    }
    if (!headerSpace.getStates().isEmpty() && !headerSpace.getStates().contains(_flow.getState())) {
      return false;
    }
    if (!headerSpace.getTcpFlags().isEmpty()
        && headerSpace.getTcpFlags().stream().noneMatch(tcpFlags -> tcpFlags.match(_flow))) {
      return false;
    }
    return true;
  }

  private boolean trace(@Nonnull IpAccessList ipAccessList) {
    List<IpAccessListLine> lines = ipAccessList.getLines();
    newTrace();
    for (int i = 0; i < lines.size(); i++) {
      IpAccessListLine line = lines.get(i);
      if (line.getMatchCondition().accept(this)) {
        recordAction(ipAccessList, i, line);
        endTrace();
        return line.getAction() == LineAction.PERMIT;
      }
      nextLine();
    }
    recordDefaultDeny(ipAccessList);
    endTrace();
    return false;
  }

  public boolean trace(@Nonnull IpSpace ipSpace, @Nonnull Ip ip, @Nonnull String ipDescription) {
    return ipSpace.accept(new IpSpaceTracer(this, ip, ipDescription));
  }

  public boolean traceDstIp(@Nonnull IpSpace ipSpace, @Nonnull Ip ip) {
    return ipSpace.accept(new IpSpaceTracer(this, ip, "destination IP"));
  }

  public boolean traceSrcIp(@Nonnull IpSpace ipSpace, @Nonnull Ip ip) {
    return ipSpace.accept(new IpSpaceTracer(this, ip, "source IP"));
  }

  @Override
  public Boolean visitMatchHeaderSpace(MatchHeaderSpace matchHeaderSpace) {
    return trace(matchHeaderSpace.getHeaderspace());
  }

  @Override
  public Boolean visitPermittedByAcl(PermittedByAcl permittedByAcl) {
    return trace(_availableAcls.get(permittedByAcl.getAclName()));
  }

  @Override
  public Boolean visitAndMatchExpr(AndMatchExpr andMatchExpr) {
    return andMatchExpr.getConjuncts().stream()
        .allMatch(
            c -> {
              newTrace();
              Boolean result = c.accept(this);
              endTrace();
              return result;
            });
  }

  @Override
  public Boolean visitOrMatchExpr(OrMatchExpr orMatchExpr) {
    return orMatchExpr.getDisjuncts().stream()
        .anyMatch(
            d -> {
              newTrace();
              Boolean result = d.accept(this);
              endTrace();
              return result;
            });
  }

  /**
   * Start a new trace at the current depth level. Indicates jump in a level of indirection to a new
   * structure (even though said structure can still be part of a single ACL line.
   */
  public void newTrace() {
    // Add new child, set it as current node
    _currentTreeNode = _currentTreeNode.addChild(TraceEventNode.withParent(_currentTreeNode));
  }

  /** End a trace: indicates that tracing of a structure is finished. */
  public void endTrace() {
    // Go up level of a tree, do not delete children
    _currentTreeNode = _currentTreeNode.getParent();
  }

  /**
   * Indicate we are moving on to the next line in current data structure (i.e., did not match
   * previous line)
   */
  public void nextLine() {
    // All previous children are of no interest since they resulted in a no-match on previous line
    _currentTreeNode.clearChildren();
  }

  /** For building trace event trees */
  private static final class TraceEventNode {
    private @Nullable TraceEvent _event;
    private final @Nullable TraceEventNode _parent;
    private final @Nonnull List<TraceEventNode> _children;

    private TraceEventNode(
        @Nullable TraceEvent event,
        @Nullable TraceEventNode parent,
        @Nonnull List<TraceEventNode> children) {
      _event = event;
      _parent = parent;
      _children = children;
    }

    private static TraceEventNode withParent(@Nullable TraceEventNode parent) {
      return new TraceEventNode(null, parent, new ArrayList<>());
    }

    @Nonnull
    private List<TraceEventNode> getChildren() {
      return _children;
    }

    @Nullable
    private TraceEventNode getParent() {
      return _parent;
    }

    @Nullable
    private TraceEvent getEvent() {
      return _event;
    }

    private void setEvent(@Nonnull TraceEvent event) {
      _event = event;
    }

    /** Adds a new child to this node trace node. Returns pointer to given node */
    private TraceEventNode addChild(@Nonnull TraceEventNode node) {
      _children.add(node);
      return node;
    }

    /** Clears all children from this node */
    private void clearChildren() {
      _children.clear();
    }
  }
}
