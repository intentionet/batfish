package org.batfish.bddreachability;

import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.symbolic.state.NodeAccept;
import org.batfish.symbolic.state.NodeDropAclIn;
import org.batfish.symbolic.state.NodeDropAclOut;
import org.batfish.symbolic.state.NodeDropNoRoute;
import org.batfish.symbolic.state.NodeDropNullRoute;
import org.batfish.symbolic.state.NodeInterfaceDeliveredToSubnet;
import org.batfish.symbolic.state.NodeInterfaceExitsNetwork;
import org.batfish.symbolic.state.NodeInterfaceInsufficientInfo;
import org.batfish.symbolic.state.NodeInterfaceNeighborUnreachable;
import org.batfish.symbolic.state.OriginateInterfaceLink;
import org.batfish.symbolic.state.OriginateVrf;
import org.batfish.symbolic.state.PostInInterface;
import org.batfish.symbolic.state.PostInInterfacePostNat;
import org.batfish.symbolic.state.PostInVrf;
import org.batfish.symbolic.state.PostInVrfSession;
import org.batfish.symbolic.state.PreInInterface;
import org.batfish.symbolic.state.PreOutEdge;
import org.batfish.symbolic.state.PreOutEdgePostNat;
import org.batfish.symbolic.state.PreOutEdgeSession;
import org.batfish.symbolic.state.PreOutInterfaceDeliveredToSubnet;
import org.batfish.symbolic.state.PreOutInterfaceExitsNetwork;
import org.batfish.symbolic.state.PreOutInterfaceInsufficientInfo;
import org.batfish.symbolic.state.PreOutInterfaceNeighborUnreachable;
import org.batfish.symbolic.state.PreOutVrf;
import org.batfish.symbolic.state.PreOutVrfSession;
import org.batfish.symbolic.state.StateExpr;
import org.batfish.symbolic.state.StateExprVisitor;
import org.batfish.symbolic.state.VrfAccept;

/**
 * For {@link StateExpr StateExprs} that correspond to points where a firewall session would get
 * installed, return the (node,outgoing interface) of that point. Return null for all other
 * StateExprs.
 *
 * <p>Currently the session StateExprs are {@link NodeInterfaceDeliveredToSubnet}, {@link
 * NodeInterfaceExitsNetwork}, and {@link PreOutEdgePostNat}. Other ARP-failure StateExprs like
 * {@link NodeInterfaceNeighborUnreachable} are excluded because they are failure cases (we don't
 * need to track sessions for failures).
 */
final class SessionCreationNodeVisitor implements StateExprVisitor<NodeInterfacePair> {
  public static final SessionCreationNodeVisitor INSTANCE = new SessionCreationNodeVisitor();

  private SessionCreationNodeVisitor() {}

  @Override
  public NodeInterfacePair visitAccept() {
    return null;
  }

  @Override
  public NodeInterfacePair visitDropAclIn() {
    return null;
  }

  @Override
  public NodeInterfacePair visitDropAclOut() {
    return null;
  }

  @Override
  public NodeInterfacePair visitDropNoRoute() {
    return null;
  }

  @Override
  public NodeInterfacePair visitDropNullRoute() {
    return null;
  }

  @Override
  public NodeInterfacePair visitExitsNetwork() {
    return null;
  }

  @Override
  public NodeInterfacePair visitDeliveredToSubnet() {
    return null;
  }

  @Override
  public NodeInterfacePair visitInsufficientInfo() {
    return null;
  }

  @Override
  public NodeInterfacePair visitNeighborUnreachable() {
    return null;
  }

  @Override
  public NodeInterfacePair visitNodeAccept(NodeAccept nodeAccept) {
    return null;
  }

  @Override
  public NodeInterfacePair visitNodeDropAclIn(NodeDropAclIn nodeDropAclIn) {
    return null;
  }

  @Override
  public NodeInterfacePair visitNodeDropAclOut(NodeDropAclOut nodeDropAclOut) {
    return null;
  }

  @Override
  public NodeInterfacePair visitNodeDropNoRoute(NodeDropNoRoute nodeDropNoRoute) {
    return null;
  }

  @Override
  public NodeInterfacePair visitNodeDropNullRoute(NodeDropNullRoute nodeDropNullRoute) {
    return null;
  }

  @Override
  public NodeInterfacePair visitNodeInterfaceDeliveredToSubnet(
      NodeInterfaceDeliveredToSubnet expr) {
    return NodeInterfacePair.of(expr.getHostname(), expr.getInterface());
  }

  @Override
  public NodeInterfacePair visitNodeInterfaceExitsNetwork(NodeInterfaceExitsNetwork expr) {
    return NodeInterfacePair.of(expr.getHostname(), expr.getInterface());
  }

  @Override
  public NodeInterfacePair visitNodeInterfaceInsufficientInfo(
      NodeInterfaceInsufficientInfo nodeInterfaceInsufficientInfo) {
    return null;
  }

  @Override
  public NodeInterfacePair visitNodeInterfaceNeighborUnreachable(
      NodeInterfaceNeighborUnreachable nodeInterfaceNeighborUnreachable) {
    return null;
  }

  @Override
  public NodeInterfacePair visitOriginateInterfaceLink(
      OriginateInterfaceLink originateInterfaceLink) {
    return null;
  }

  @Override
  public NodeInterfacePair visitOriginateVrf(OriginateVrf originateVrf) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPostInInterface(PostInInterface postInInterface) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPostInInterfacePostNat(
      PostInInterfacePostNat postInInterfacePostNat) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPostInVrf(PostInVrf postInVrf) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreInInterface(PreInInterface preInInterface) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreOutVrf(PreOutVrf preOutVrf) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreOutEdge(PreOutEdge preOutEdge) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreOutEdgePostNat(PreOutEdgePostNat expr) {
    return NodeInterfacePair.of(expr.getSrcNode(), expr.getSrcIface());
  }

  @Override
  public NodeInterfacePair visitPreOutInterfaceDeliveredToSubnet(
      PreOutInterfaceDeliveredToSubnet preOutInterfaceDeliveredToSubnet) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreOutInterfaceExitsNetwork(
      PreOutInterfaceExitsNetwork preOutInterfaceExitsNetwork) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreOutInterfaceInsufficientInfo(
      PreOutInterfaceInsufficientInfo preOutInterfaceInsufficientInfo) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreOutInterfaceNeighborUnreachable(
      PreOutInterfaceNeighborUnreachable preOutInterfaceNeighborUnreachable) {
    return null;
  }

  @Override
  public NodeInterfacePair visitQuery() {
    return null;
  }

  @Override
  public NodeInterfacePair visitPostInVrfSession(PostInVrfSession postInVrfSession) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreOutEdgeSession(PreOutEdgeSession preOutEdgeSession) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreOutVrfSession(PreOutVrfSession preOutVrfSession) {
    return null;
  }

  @Override
  public NodeInterfacePair visitVrfAccept(VrfAccept vrfAccept) {
    return null;
  }
}
