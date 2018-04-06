package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;

/** Returns true if the IPv4 route is classful, aka it is exactly */
public final class RouteIsClassful extends BooleanExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private static final RouteIsClassful INSTANCE = new RouteIsClassful();

  private RouteIsClassful() {}

  @JsonCreator
  public static RouteIsClassful instance() {
    return INSTANCE;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  @Override
  public Result evaluate(Environment environment) {
    AbstractRoute route = environment.getOriginalRoute();
    Prefix network = route.getNetwork();
    Ip ip = network.getStartIp();

    Result ret = new Result();
    ret.setBooleanValue(ip.getClassNetworkSize() >= network.getPrefixLength());
    return ret;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
