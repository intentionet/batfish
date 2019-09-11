package org.batfish.datamodel.routing_policy.communities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A way to render a {@link CommunitySet} as a string, given a {@link CommunityRendering} for each
 * constituent element.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class CommunitySetRendering implements Serializable {

  public abstract <T> T accept(CommunitySetRenderingVisitor<T> visitor);

  @JsonProperty(PROP_COMMUNITY_RENDERING)
  public final @Nonnull CommunityRendering getCommunityRendering() {
    return _communityRendering;
  }

  protected static final String PROP_COMMUNITY_RENDERING = "communityRendering";

  protected CommunitySetRendering(CommunityRendering communityRendering) {
    _communityRendering = communityRendering;
  }

  public abstract boolean equals(@Nullable Object obj);

  public abstract int hashCode();

  protected final @Nonnull CommunityRendering _communityRendering;
}
