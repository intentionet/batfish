package org.batfish.bddreachability.transition;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDSourceManager;

/**
 * Model the packet entering a node. The {@link BDDSourceManager} sets the source interface variable
 * to the value for that interface. When traversing backwards, we erase the constraint.
 */
public final class AddSourceConstraint implements Transition {
  private final BDDSourceManager _mgr;
  private final BDD _sourceBdd;

  AddSourceConstraint(BDDSourceManager mgr, String iface) {
    checkArgument(
        !mgr.isTrivial(),
        "AddSourceConstraint for a Trivial BDDSourceManager. Use Identity instead");
    _mgr = mgr;
    _sourceBdd = mgr.getSourceInterfaceBDD(iface);
  }

  AddSourceConstraint(BDDSourceManager mgr) {
    checkArgument(
        !mgr.isTrivial(),
        "AddSourceConstraint for a Trivial BDDSourceManager. Use Identity instead");
    _mgr = mgr;
    _sourceBdd = mgr.getOriginatingFromDeviceBDD();
  }

  BDDSourceManager getSourceManager() {
    return _mgr;
  }

  BDD getSourceBdd() {
    return _sourceBdd;
  }

  @Override
  public BDD transitForward(BDD bdd) {
    return bdd.and(_sourceBdd);
  }

  @Override
  public BDD transitBackward(BDD bdd) {
    return _mgr.existsSource(bdd.and(_sourceBdd));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(AddSourceConstraint.class)
        .add("iface", _mgr.getSourceFromAssignment(_sourceBdd).orElse("missing"))
        .toString();
  }
}
