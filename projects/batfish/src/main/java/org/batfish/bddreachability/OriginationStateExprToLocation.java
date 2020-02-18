package org.batfish.bddreachability;

import java.util.Optional;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.Location;
import org.batfish.symbolic.state.InterfaceAccept;
import org.batfish.symbolic.state.NodeAccept;
import org.batfish.symbolic.state.NodeDropAclIn;
import org.batfish.symbolic.state.NodeDropAclOut;
import org.batfish.symbolic.state.NodeDropNoRoute;
import org.batfish.symbolic.state.NodeDropNullRoute;
import org.batfish.symbolic.state.NodeInterfaceDeliveredToSubnet;
import org.batfish.symbolic.state.NodeInterfaceExitsNetwork;
import org.batfish.symbolic.state.NodeInterfaceInsufficientInfo;
import org.batfish.symbolic.state.NodeInterfaceNeighborUnreachable;
import org.batfish.symbolic.state.OriginateInterface;
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
 * Visitor to convert an origination {@link StateExpr} to its corresponding {@link Location}, if any
 */
public final class OriginationStateExprToLocation implements StateExprVisitor<Optional<Location>> {
  private static final OriginationStateExprToLocation INSTANCE =
      new OriginationStateExprToLocation();

  public static Optional<Location> toLocation(StateExpr expr) {
    return expr.accept(INSTANCE);
  }

  private OriginationStateExprToLocation() {}

  @Override
  public Optional<Location> visitAccept() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitDeliveredToSubnet() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitDropAclIn() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitDropAclOut() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitDropNoRoute() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitDropNullRoute() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitExitsNetwork() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitInsufficientInfo() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitInterfaceAccept(InterfaceAccept interfaceAccept) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNeighborUnreachable() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNodeAccept(NodeAccept nodeAccept) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNodeDropAclIn(NodeDropAclIn nodeDropAclIn) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNodeDropAclOut(NodeDropAclOut nodeDropAclOut) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNodeDropNoRoute(NodeDropNoRoute nodeDropNoRoute) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNodeDropNullRoute(NodeDropNullRoute nodeDropNullRoute) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNodeInterfaceDeliveredToSubnet(
      NodeInterfaceDeliveredToSubnet nodeInterfaceDeliveredToSubnet) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNodeInterfaceExitsNetwork(
      NodeInterfaceExitsNetwork nodeInterfaceExitsNetwork) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNodeInterfaceInsufficientInfo(
      NodeInterfaceInsufficientInfo nodeInterfaceInsufficientInfo) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitNodeInterfaceNeighborUnreachable(
      NodeInterfaceNeighborUnreachable nodeInterfaceNeighborUnreachable) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitOriginateInterface(OriginateInterface originateInterface) {
    return Optional.of(
        new InterfaceLocation(originateInterface.getHostname(), originateInterface.getInterface()));
  }

  @Override
  public Optional<Location> visitOriginateInterfaceLink(
      OriginateInterfaceLink originateInterfaceLink) {
    return Optional.of(
        new InterfaceLinkLocation(
            originateInterfaceLink.getHostname(), originateInterfaceLink.getInterface()));
  }

  @Override
  public Optional<Location> visitOriginateVrf(OriginateVrf originateVrf) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPostInInterface(PostInInterface postInInterface) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPostInInterfacePostNat(
      PostInInterfacePostNat postInInterfacePostNat) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPostInVrf(PostInVrf postInVrf) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPostInVrfSession(PostInVrfSession postInVrfSession) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreInInterface(PreInInterface preInInterface) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreOutEdge(PreOutEdge preOutEdge) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreOutEdgePostNat(PreOutEdgePostNat preOutInterface) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreOutEdgeSession(PreOutEdgeSession preOutEdgeSession) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreOutInterfaceDeliveredToSubnet(
      PreOutInterfaceDeliveredToSubnet preOutInterfaceDeliveredToSubnet) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreOutInterfaceExitsNetwork(
      PreOutInterfaceExitsNetwork preOutInterfaceExitsNetwork) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreOutInterfaceInsufficientInfo(
      PreOutInterfaceInsufficientInfo preOutInterfaceInsufficientInfo) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreOutInterfaceNeighborUnreachable(
      PreOutInterfaceNeighborUnreachable preOutInterfaceNeighborUnreachable) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreOutVrf(PreOutVrf preOutVrf) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitPreOutVrfSession(PreOutVrfSession preOutVrfSession) {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitQuery() {
    return Optional.empty();
  }

  @Override
  public Optional<Location> visitVrfAccept(VrfAccept vrfAccept) {
    return Optional.empty();
  }
}
