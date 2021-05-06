package org.batfish.representation.cisco_xr;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.routing_policy.expr.AsPathSetElem;

@ParametersAreNonnullByDefault
public class AsPathSet implements Serializable {

  private @Nonnull List<AsPathSetElem> _elements;
  private final @Nonnull String _name;

  public AsPathSet(String name) {
    _name = name;
    _elements = ImmutableList.of();
  }

  public @Nonnull List<AsPathSetElem> getElements() {
    return _elements;
  }

  public String getName() {
    return _name;
  }

  public void addElement(AsPathSetElem element) {
    _elements = ImmutableList.<AsPathSetElem>builder().addAll(_elements).add(element).build();
  }
}
