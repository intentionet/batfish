package org.batfish.bddreachability.transition;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDSourceManager;

/**
 * Model the packet exiting a node. The {@link BDDSourceManager} enforces that the source interface
 * variable is constrained to be a valid value while within the node, and erases the constrain when
 * exiting.
 */
@ParametersAreNonnullByDefault
final class RemoveSourceConstraint implements Transition {
  private final @Nonnull BDDSourceManager _mgr;

  RemoveSourceConstraint(BDDSourceManager mgr) {
    checkArgument(
        !mgr.isTrivial(),
        "RemoveSourceConstraint for a Trivial BDDSourceManager. Use Identity instead");
    _mgr = mgr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RemoveSourceConstraint)) {
      return false;
    }
    RemoveSourceConstraint that = (RemoveSourceConstraint) o;
    return _mgr.equals(that._mgr);
  }

  BDDSourceManager getSourceManager() {
    return _mgr;
  }

  @Override
  public int hashCode() {
    return _mgr.hashCode();
  }

  @Override
  public BDD transitForward(BDD bdd) {
    return _mgr.existsSource(bdd);
  }

  @Override
  public BDD transitBackward(BDD bdd) {
    return bdd.and(_mgr.isValidValue());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
