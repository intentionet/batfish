package org.batfish.datamodel.matchers;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.List;
import javax.annotation.Nonnull;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.IkeGateway;
import org.batfish.datamodel.IpsecProposal;
import org.batfish.datamodel.matchers.IpsecPolicyMatchersImpl.HasIkeGateway;
import org.batfish.datamodel.matchers.IpsecPolicyMatchersImpl.HasIpsecProposals;
import org.batfish.datamodel.matchers.IpsecPolicyMatchersImpl.HasPfsKeyGroup;
import org.hamcrest.Matcher;

public final class IpsecPolicyMatchers {

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the Ipsec policy's
   * Ipsec Proposals.
   */
  public static HasIpsecProposals hasIpsecProposals(
      @Nonnull Matcher<? super List<IpsecProposal>> subMatcher) {
    return new HasIpsecProposals(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the Ipsec policy's
   * Ike Gateway
   */
  public static HasIkeGateway hasIkeGateway(@Nonnull Matcher<? super IkeGateway> subMatcher) {
    return new HasIkeGateway(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the Ipsec policy's
   * PfsKeyGroupy
   */
  public static HasPfsKeyGroup hasPfsKeyGroup(DiffieHellmanGroup pfsKeyGroup) {
    return new HasPfsKeyGroup(equalTo(pfsKeyGroup));
  }

  private IpsecPolicyMatchers() {}
}
