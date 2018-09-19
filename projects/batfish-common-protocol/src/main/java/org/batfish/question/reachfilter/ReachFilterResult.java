package org.batfish.question.reachfilter;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.acl.AclLineMatchExpr;

/**
 * A result from the ReachFilter question: a description of the headerspace expressed as an {@link
 * AclLineMatchExpr} and an example flow in the headerspace.
 */
@ParametersAreNonnullByDefault
public class ReachFilterResult {
  private final @Nullable AclLineMatchExpr _headerSpaceDescription;
  private final Flow _exampleFlow;

  public ReachFilterResult(Flow exampleFlow, @Nullable AclLineMatchExpr headerSpaceDescription) {
    _headerSpaceDescription = headerSpaceDescription;
    _exampleFlow = exampleFlow;
  }

  public @Nonnull Optional<AclLineMatchExpr> getHeaderSpaceDescription() {
    return Optional.ofNullable(_headerSpaceDescription);
  }

  public @Nonnull Flow getExampleFlow() {
    return _exampleFlow;
  }
}
