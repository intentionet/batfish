package org.batfish.datamodel.applications;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.SubRange;

/**
 * An abstract class that represents an application, which is an IP protocol and
 * application-speficic details covered in child classes
 */
@ParametersAreNonnullByDefault
public abstract class Application {

  @Nonnull private final IpProtocol _ipProtocol;

  protected Application(IpProtocol ipProtocol) {
    _ipProtocol = ipProtocol;
  }

  @Nonnull
  public IpProtocol getIpProtocol() {
    return _ipProtocol;
  }

  protected String stringifySubRanges(List<SubRange> subranges) {
    return subranges.stream()
        .map(
            subrange ->
                subrange.isSingleValue()
                    ? Objects.toString(subrange.getStart())
                    : subrange.serializedForm())
        .collect(Collectors.joining(","));
  }
}
