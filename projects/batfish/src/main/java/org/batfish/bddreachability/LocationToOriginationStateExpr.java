package org.batfish.bddreachability;

import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.LocationVisitor;
import org.batfish.symbolic.state.OriginateInterface;
import org.batfish.symbolic.state.OriginateInterfaceLink;
import org.batfish.symbolic.state.OriginateVrf;
import org.batfish.symbolic.state.StateExpr;

/**
 * {@link LocationVisitor} that converts locations to corresponding origination {@link StateExpr}.
 */
public class LocationToOriginationStateExpr implements LocationVisitor<Optional<StateExpr>> {
  private final Map<String, Configuration> _configs;
  private final boolean _useOriginateInterface;

  /**
   * @param useOriginateInterface Whether visiting {@link InterfaceLocation} should return an {@link
   *     OriginateInterface}. If false, it will return an {@link OriginateVrf} instead.
   */
  LocationToOriginationStateExpr(
      Map<String, Configuration> configs, boolean useOriginateInterface) {
    _configs = configs;
    _useOriginateInterface = useOriginateInterface;
  }

  @Override
  public Optional<StateExpr> visitInterfaceLinkLocation(
      @Nonnull InterfaceLinkLocation interfaceLinkLocation) {
    Configuration config = _configs.get(interfaceLinkLocation.getNodeName());
    if (config == null) {
      return Optional.empty();
    }
    Interface iface = config.getAllInterfaces().get(interfaceLinkLocation.getInterfaceName());
    if (iface == null) {
      return Optional.empty();
    }
    return Optional.of(
        new OriginateInterfaceLink(
            interfaceLinkLocation.getNodeName(), interfaceLinkLocation.getInterfaceName()));
  }

  @Override
  public Optional<StateExpr> visitInterfaceLocation(@Nonnull InterfaceLocation interfaceLocation) {
    Configuration config = _configs.get(interfaceLocation.getNodeName());
    if (config == null) {
      return Optional.empty();
    }
    Interface iface = config.getAllInterfaces().get(interfaceLocation.getInterfaceName());
    if (iface == null) {
      return Optional.empty();
    }
    if (_useOriginateInterface) {
      return Optional.of(
          new OriginateInterface(
              interfaceLocation.getNodeName(), interfaceLocation.getInterfaceName()));
    }
    String vrf = iface.getVrfName();
    return Optional.of(new OriginateVrf(interfaceLocation.getNodeName(), vrf));
  }
}
