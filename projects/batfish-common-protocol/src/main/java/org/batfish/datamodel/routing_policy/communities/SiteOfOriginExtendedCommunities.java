package org.batfish.datamodel.routing_policy.communities;

import com.fasterxml.jackson.annotation.JsonCreator;
import javax.annotation.Nonnull;

/**
 * Matches a {@link org.batfish.datamodel.bgp.community.Community} iff it is a site-of-origin
 * extended community.
 */
public class SiteOfOriginExtendedCommunities extends CommunityMatchExpr {

  public static @Nonnull SiteOfOriginExtendedCommunities instance() {
    return INSTANCE;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof SiteOfOriginExtendedCommunities;
  }

  @Override
  public int hashCode() {
    return 0xA0087E18; // randomly generated
  }

  @Override
  protected <T> T accept(CommunityMatchExprVisitor<T> visitor) {
    return visitor.visitSiteOfOriginExtendedCommunities(this);
  }

  private static final SiteOfOriginExtendedCommunities INSTANCE =
      new SiteOfOriginExtendedCommunities();

  @JsonCreator
  private static @Nonnull SiteOfOriginExtendedCommunities create() {
    return INSTANCE;
  }

  private SiteOfOriginExtendedCommunities() {}
}
