package org.batfish.specifier;

import com.google.auto.service.AutoService;
import org.batfish.datamodel.UniverseIpSpace;

@AutoService(IpSpaceSpecifierFactory.class)
public final class ConstantUniverseIpSpaceSpecifierFactory implements IpSpaceSpecifierFactory {
  public static final String NAME = ConstantUniverseIpSpaceSpecifierFactory.class.getSimpleName();

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public IpSpaceSpecifier buildIpSpaceSpecifier(Object input) {
    return new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE);
  }
}
