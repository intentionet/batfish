package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.visitors.GenericIpSpaceVisitor;

/**
 * Represents a space of IPv4 addresses using a whitelist and blacklist of {@link IpWildcard}s. The
 * blacklist takes priority, so if an {@link Ip} is matched by both lists, it is not in the space.
 *
 * <p>Any empty whitelist is equivalent to an {@link EmptyIpSpace}.
 */
public final class IpWildcardSetIpSpace
    implements IpSpace, Serializable, Comparable<IpWildcardSetIpSpace> {

  public static class Builder {

    private final ImmutableSet.Builder<IpWildcard> _blacklistBuilder;

    private final ImmutableSet.Builder<IpWildcard> _whitelistBuilder;

    private Builder() {
      _blacklistBuilder = ImmutableSet.builder();
      _whitelistBuilder = ImmutableSet.builder();
    }

    public IpWildcardSetIpSpace build() {
      return new IpWildcardSetIpSpace(_blacklistBuilder.build(), _whitelistBuilder.build());
    }

    public Builder excluding(IpWildcard... wildcards) {
      return excluding(Arrays.asList(wildcards));
    }

    public Builder excluding(Iterable<IpWildcard> wildcards) {
      _blacklistBuilder.addAll(wildcards);
      return this;
    }

    public Builder including(IpWildcard... wildcards) {
      return including(Arrays.asList(wildcards));
    }

    public Builder including(Iterable<IpWildcard> wildcards) {
      _whitelistBuilder.addAll(wildcards);
      return this;
    }
  }

  public static final IpWildcardSetIpSpace ANY =
      IpWildcardSetIpSpace.builder().including(IpWildcard.ANY).build();

  private static final String PROP_BLACKLIST = "blacklist";

  private static final String PROP_WHITELIST = "whitelist";

  /** */
  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  private final SortedSet<IpWildcard> _blacklist;

  private final SortedSet<IpWildcard> _whitelist;

  @JsonCreator
  private IpWildcardSetIpSpace(
      @JsonProperty(PROP_BLACKLIST) Set<IpWildcard> blacklist,
      @JsonProperty(PROP_WHITELIST) Set<IpWildcard> whitelist) {
    _blacklist = blacklist == null ? ImmutableSortedSet.of() : ImmutableSortedSet.copyOf(blacklist);
    _whitelist = whitelist == null ? ImmutableSortedSet.of() : ImmutableSortedSet.copyOf(whitelist);
  }

  @Override
  public <R> R accept(GenericIpSpaceVisitor<R> ipSpaceVisitor) {
    return ipSpaceVisitor.visitIpWildcardSetIpSpace(this);
  }

  @Override
  public int compareTo(IpWildcardSetIpSpace o) {
    return Comparator.comparing(IpWildcardSetIpSpace::getBlacklist, CommonUtil::compareIterable)
        .thenComparing(IpWildcardSetIpSpace::getWhitelist, CommonUtil::compareIterable)
        .compare(this, o);
  }

  @Override
  public IpSpace complement() {
    /*
     * the current is first reject everything in blacklist.
     * then of what's left over accept everything in whitelist.
     * and then reject everything else
     *
     * the complement then is to accept everything in the blacklist
     * then reject everything in the whitelist
     * then accept everything else.
     */
    AclIpSpace.Builder builder = AclIpSpace.builder();
    _blacklist.forEach(builder::thenPermitting);
    _whitelist.forEach(builder::thenRejecting);
    builder.thenPermitting(UniverseIpSpace.INSTANCE);
    return builder.build();
  }

  @Override
  public boolean containsIp(@Nonnull Ip ip) {
    return _blacklist.stream().noneMatch(w -> w.containsIp(ip))
        && _whitelist.stream().anyMatch(w -> w.containsIp(ip));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof IpWildcardSetIpSpace)) {
      return false;
    }
    IpWildcardSetIpSpace rhs = (IpWildcardSetIpSpace) o;
    return Objects.equals(_blacklist, rhs._blacklist) && Objects.equals(_whitelist, rhs._whitelist);
  }

  @JsonProperty(PROP_BLACKLIST)
  public SortedSet<IpWildcard> getBlacklist() {
    return _blacklist;
  }

  @JsonProperty(PROP_WHITELIST)
  public SortedSet<IpWildcard> getWhitelist() {
    return _whitelist;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_blacklist, _whitelist);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass())
        .add(PROP_BLACKLIST, _blacklist)
        .add(PROP_WHITELIST, _whitelist)
        .toString();
  }
}
