package org.batfish.specifier;

public interface LocationVisitor<T> {
  default T visit(Location location) {
    return location.accept(this);
  }

  T visitInterfaceLinkLocation(InterfaceLinkLocation interfaceLinkLocation);

  T visitInterfaceLocation(InterfaceLocation interfaceLocation);

  /** A visitor that returns true if the location is on {@code node} */
  static LocationVisitor<Boolean> onNode(String node) {
    return new LocationVisitor<Boolean>() {
      @Override
      public Boolean visitInterfaceLinkLocation(InterfaceLinkLocation interfaceLinkLocation) {
        return interfaceLinkLocation.getNodeName().equals(node);
      }

      @Override
      public Boolean visitInterfaceLocation(InterfaceLocation interfaceLocation) {
        return interfaceLocation.getNodeName().equals(node);
      }
    };
  }
}
