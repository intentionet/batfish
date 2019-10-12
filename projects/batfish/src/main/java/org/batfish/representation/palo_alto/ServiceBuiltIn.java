package org.batfish.representation.palo_alto;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.function.Supplier;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.SubRange;

public enum ServiceBuiltIn {
  ANY,
  APPLICATION_DEFAULT,
  SERVICE_HTTP,
  SERVICE_HTTPS;

  private final Supplier<HeaderSpace> _serviceHeaderSpace;

  ServiceBuiltIn() {
    _serviceHeaderSpace = Suppliers.memoize(this::init);
  }

  private HeaderSpace init() {
    switch (this) {
      case SERVICE_HTTP:
        return HeaderSpace.builder()
            .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
            .setDstPorts(ImmutableSortedSet.of(SubRange.singleton(80), new SubRange(8080, 8080)))
            .build();
      case SERVICE_HTTPS:
        return HeaderSpace.builder()
            .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
            .setDstPorts(ImmutableSortedSet.of(SubRange.singleton(443)))
            .build();
        // any and application-default don't match a specific port
      case ANY:
      case APPLICATION_DEFAULT:
      default:
        return null;
    }
  }

  public HeaderSpace getHeaderSpace() {
    return _serviceHeaderSpace.get();
  }

  public String getName() {
    switch (this) {
      case ANY:
        return "any";
      case APPLICATION_DEFAULT:
        return "application-default";
      case SERVICE_HTTP:
        return "service-http";
      case SERVICE_HTTPS:
        return "service-https";
      default:
        return null;
    }
  }
}
