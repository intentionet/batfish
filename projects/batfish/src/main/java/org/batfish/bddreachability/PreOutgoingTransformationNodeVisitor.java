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
import org.batfish.symbolic.state.StateExprVisitor;
import org.batfish.symbolic.state.VrfAccept;

/**
 * If the input node occurs right before an outgoing transformation is applied (possibly with an
 * intermediate ACL), return the node/interface of that transformation. Otherwise, return null.
 */
public class PreOutgoingTransformationNodeVisitor implements StateExprVisitor<NodeInterfacePair> {
  public static final PreOutgoingTransformationNodeVisitor INSTANCE =
      new PreOutgoingTransformationNodeVisitor();

  private PreOutgoingTransformationNodeVisitor() {}

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
      NodeInterfaceDeliveredToSubnet nodeInterfaceDeliveredToSubnet) {
    return null;
  }

  @Override
  public NodeInterfacePair visitNodeInterfaceExitsNetwork(
      NodeInterfaceExitsNetwork nodeInterfaceExitsNetwork) {
    return null;
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
    return NodeInterfacePair.of(preOutEdge.getSrcNode(), preOutEdge.getSrcIface());
  }

  @Override
  public NodeInterfacePair visitPreOutEdgePostNat(PreOutEdgePostNat preOutInterface) {
    return null;
  }

  @Override
  public NodeInterfacePair visitPreOutInterfaceDeliveredToSubnet(
      PreOutInterfaceDeliveredToSubnet state) {
    return NodeInterfacePair.of(state.getHostname(), state.getInterface());
  }

  @Override
  public NodeInterfacePair visitPreOutInterfaceExitsNetwork(PreOutInterfaceExitsNetwork state) {
    return NodeInterfacePair.of(state.getHostname(), state.getInterface());
  }

  @Override
  public NodeInterfacePair visitPreOutInterfaceInsufficientInfo(
      PreOutInterfaceInsufficientInfo state) {
    return NodeInterfacePair.of(state.getHostname(), state.getInterface());
  }

  @Override
  public NodeInterfacePair visitPreOutInterfaceNeighborUnreachable(
      PreOutInterfaceNeighborUnreachable state) {
    return NodeInterfacePair.of(state.getHostname(), state.getInterface());
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
