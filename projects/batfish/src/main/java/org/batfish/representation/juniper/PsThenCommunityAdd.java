package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.statement.AddCommunity;
import org.batfish.datamodel.routing_policy.statement.Comment;
import org.batfish.datamodel.routing_policy.statement.Statement;

@ParametersAreNonnullByDefault
public final class PsThenCommunityAdd extends PsThen {

  /** */
  private static final long serialVersionUID = 1L;

  private JuniperConfiguration _configuration;

  private final String _name;

  public PsThenCommunityAdd(String name, JuniperConfiguration configuration) {
    _name = name;
    _configuration = configuration;
  }

  @Override
  public void applyTo(
      List<Statement> statements,
      JuniperConfiguration juniperVendorConfiguration,
      Configuration c,
      Warnings warnings) {
    CommunityList namedList =
        _configuration.getMasterLogicalSystem().getCommunityLists().get(_name);
    if (namedList == null) {
      warnings.redFlag("Reference to undefined community: \"" + _name + "\"");
      return;
    } else {
      /*
       * Regex semantics do not apply in literal context. Instead, members with wildcards are
       * filtered out. If a list is used in a 'then community add' but does not contain any literal
       * community expressions, the entire configuration is invalid. As a best-effort, warn and
       * treat line as a NOP.
       */
      Set<Long> literalCommunities = namedList.extractLiteralCommunities();
      if (literalCommunities.isEmpty()) {
        String msg =
            String.format(
                "Juniper will not commit this configuration: 'then community add %s' is not valid"
                    + "because %s does not contain any literal communities",
                _name, _name);
        warnings.redFlag(msg);
        statements.add(new Comment(msg));
        return;
      } else if (literalCommunities.size() != namedList.getLines().size()) {
        warnings.redFlag(
            String.format(
                "Use of 'then community add %s' where '%s' contains both literal communities and "
                    + "community regex expressions is unusual and may be unintentional. Note that "
                    + "regex community expressions in '%s' are ignored in this context.",
                _name, _name, _name));
      }
      statements.add(
          new AddCommunity(
              new LiteralCommunitySet(
                  literalCommunities.stream()
                      .map(StandardCommunity::of)
                      .collect(ImmutableSet.toImmutableSet()))));
    }
  }

  public @Nonnull String getName() {
    return _name;
  }
}
