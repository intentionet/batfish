package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclTrace;
import org.batfish.datamodel.acl.DefaultDeniedByAclIpSpace;
import org.batfish.datamodel.acl.DefaultDeniedByIpAccessList;
import org.batfish.datamodel.acl.DeniedByAclIpSpaceLine;
import org.batfish.datamodel.acl.DeniedByIpAccessListLine;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.PermittedByAclIpSpaceLine;
import org.batfish.datamodel.acl.PermittedByIpAccessListLine;
import org.batfish.datamodel.acl.TraceEvent;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.eigrp.EigrpProcess;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.matchers.AclTraceMatchers.HasEvents;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasRoute6FilterList;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasRoute6FilterLists;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasRouteFilterList;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasRouteFilterLists;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasZone;
import org.batfish.datamodel.matchers.ConvertConfigurationAnswerElementMatchers.HasNumReferrers;
import org.batfish.datamodel.matchers.ConvertConfigurationAnswerElementMatchers.HasRedFlagWarning;
import org.batfish.datamodel.matchers.DefaultDeniedByAclIpSpaceMatchers.IsDefaultDeniedByAclIpSpaceThat;
import org.batfish.datamodel.matchers.DefaultDeniedByIpAccessListMatchers.IsDefaultDeniedByIpAccessListThat;
import org.batfish.datamodel.matchers.DeniedByAclIpSpaceLineMatchersImpl.IsDeniedByAclIpSpaceLineThat;
import org.batfish.datamodel.matchers.DeniedByIpAccessListLineMatchersImpl.IsDeniedByIpAccessListLineThat;
import org.batfish.datamodel.matchers.DeniedByNamedIpSpaceMatchers.IsDeniedByNamedIpSpaceThat;
import org.batfish.datamodel.matchers.HeaderSpaceMatchersImpl.HasSrcOrDstPorts;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasBandwidth;
import org.batfish.datamodel.matchers.OspfProcessMatchersImpl.HasReferenceBandwidth;
import org.batfish.datamodel.matchers.PermittedByAclIpSpaceLineMatchersImpl.IsPermittedByAclIpSpaceLineThat;
import org.batfish.datamodel.matchers.PermittedByIpAccessListLineMatchersImpl.IsPermittedByIpAccessListLineThat;
import org.batfish.datamodel.matchers.PermittedByNamedIpSpaceMatchers.IsPermittedByNamedIpSpaceThat;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasEigrpProcesses;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasIsisProcess;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.vendor.StructureType;
import org.batfish.vendor.StructureUsage;
import org.hamcrest.Matcher;

public final class DataModelMatchers {

  /**
   * Provides a matcher for a collection that matches if each element matched by {@code
   * identifiedBy} is matched by {@code conformsTo}
   */
  public static <E> Matcher<Iterable<? extends E>> forAll(
      Matcher<? super E> identifiedBy, Matcher<? super E> conformsTo) {
    return new ForAll<>(identifiedBy, conformsTo);
  }

  /** Provides a matcher that matches if the RouteFilterList permits the given {@code prefix}. */
  public static RouteFilterListMatchersImpl.Permits permits(@Nonnull Prefix prefix) {
    return new RouteFilterListMatchersImpl.Permits(prefix);
  }

  /** Provides a matcher that matches if the Route6FilterList permits the given {@code prefix}. */
  public static Route6FilterListMatchersImpl.Permits permits(@Nonnull Prefix6 prefix) {
    return new Route6FilterListMatchersImpl.Permits(prefix);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IpSpaceReference}'s {@code name}.
   */
  public static @Nonnull Matcher<PermittedByAcl> hasAclName(
      @Nonnull Matcher<? super String> subMatcher) {
    return new PermittedByAclMatchers.HasAclName(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * {@link Zone} with specified name.
   */
  public static Matcher<Configuration> hasZone(
      @Nonnull String name, @Nonnull Matcher<? super Zone> subMatcher) {
    return new HasZone(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * Interface}'s {@code incomingFilter}.
   */
  public static @Nonnull Matcher<Interface> hasIncomingFilter(
      @Nonnull Matcher<? super IpAccessList> subMatcher) {
    return new InterfaceMatchersImpl.HasIncomingFilter(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * Interface}'s {@code outgoingFilterName}.
   */
  public static @Nonnull Matcher<Interface> hasOutgoingFilter(
      @Nonnull Matcher<? super IpAccessList> subMatcher) {
    return new InterfaceMatchersImpl.HasOutgoingFilter(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * Interface}'s {@code outgoingFilterName}.
   */
  public static @Nonnull Matcher<Interface> hasOutgoingFilterName(
      @Nonnull Matcher<? super String> subMatcher) {
    return new InterfaceMatchersImpl.HasOutgoingFilterName(subMatcher);
  }

  /**
   * Provides a matcher that matches if the {@link Interface}'s {@code outgoingFilterName} is equal
   * to {@code expectedName}.
   */
  public static @Nonnull Matcher<Interface> hasOutgoingFilterName(@Nullable String expectedName) {
    return new InterfaceMatchersImpl.HasOutgoingFilterName(equalTo(expectedName));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * Interface}'s {@code postTransformationIncomingFilterName}.
   */
  public static @Nonnull Matcher<Interface> hasPostTransformationIncomingFilter(
      @Nonnull Matcher<? super IpAccessList> subMatcher) {
    return new InterfaceMatchersImpl.HasPostTransformationIncomingFilter(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * Interface}'s {@code preTransformationOutgoingFilterName}.
   */
  public static @Nonnull Matcher<Interface> hasPreTransformationOutgoingFilter(
      @Nonnull Matcher<? super IpAccessList> subMatcher) {
    return new InterfaceMatchersImpl.HasPreTransformationOutgoingFilter(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * {@link RouteFilterList} with specified name.
   */
  public static Matcher<Configuration> hasRouteFilterList(
      @Nonnull String name, @Nonnull Matcher<? super RouteFilterList> subMatcher) {
    return new HasRouteFilterList(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * routeFilterLists.
   */
  public static Matcher<Configuration> hasRouteFilterLists(
      @Nonnull Matcher<? super NavigableMap<String, RouteFilterList>> subMatcher) {
    return new HasRouteFilterLists(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has a
   * red-flag warning with text matched by {@code subMatcher}.
   */
  public static Matcher<ConvertConfigurationAnswerElement> hasRedFlagWarning(
      @Nonnull String hostname, @Nonnull Matcher<? super String> subMatcher) {
    return new HasRedFlagWarning(hostname, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code expectedReferenceBandwidth} is equal to
   * the {@link OspfProcess}'s reference-bandwidth.
   */
  public static Matcher<OspfProcess> hasReferenceBandwidth(double expectedReferenceBandwidth) {
    return new HasReferenceBandwidth(equalTo(expectedReferenceBandwidth));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * {@link Route6FilterList} with specified name.
   */
  public static Matcher<Configuration> hasRoute6FilterList(
      @Nonnull String name, @Nonnull Matcher<? super Route6FilterList> subMatcher) {
    return new HasRoute6FilterList(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * route6FilterLists.
   */
  public static Matcher<Configuration> hasRoute6FilterLists(
      @Nonnull Matcher<? super NavigableMap<String, Route6FilterList>> subMatcher) {
    return new HasRoute6FilterLists(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the HeaderSpace's
   * srcOrDstPorts.
   */
  public static @Nonnull Matcher<HeaderSpace> hasSrcOrDstPorts(
      @Nonnull Matcher<? super SortedSet<SubRange>> subMatcher) {
    return new HasSrcOrDstPorts(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code name} is that of the {@link
   * IpSpaceReference}.
   */
  public static @Nonnull Matcher<PermittedByAcl> hasAclName(@Nonnull String name) {
    return hasAclName(equalTo(name));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * AclTrace}'s events.
   */
  public static @Nonnull Matcher<AclTrace> hasEvents(
      @Nonnull Matcher<? super List<TraceEvent>> subMatcher) {
    return new HasEvents(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * HeaderSpace}'s ipProtcols.
   */
  public static @Nonnull Matcher<HeaderSpace> hasIpProtocols(
      @Nonnull Matcher<? super SortedSet<IpProtocol>> subMatcher) {
    return new HeaderSpaceMatchersImpl.HasIpProtocols(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code bandwidth} is that of the {@link
   * Interface}.
   */
  public static @Nonnull Matcher<Interface> hasBandwidth(double bandwidth) {
    return hasBandwidth(equalTo(bandwidth));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * Interface}'s bandwidth.
   */
  public static @Nonnull Matcher<Interface> hasBandwidth(Matcher<? super Double> subMatcher) {
    return new HasBandwidth(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has a
   * defined structure for {@code hostname} of type {@code type} named {@code structureName}.
   */
  public static @Nonnull Matcher<ConvertConfigurationAnswerElement> hasDefinedStructure(
      @Nonnull String hostname, @Nonnull StructureType type, @Nonnull String structureName) {
    return new ConvertConfigurationAnswerElementMatchers.HasDefinedStructure(
        hostname, type, structureName);
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has a
   * defined structure for {@code hostname} of type {@code type} named {@code structureName} with
   * definition lines matching the provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<ConvertConfigurationAnswerElement>
      hasDefinedStructureWithDefinitionLines(
          @Nonnull String hostname,
          @Nonnull StructureType type,
          @Nonnull String structureName,
          @Nonnull Matcher<? super Set<Integer>> subMatcher) {
    return new ConvertConfigurationAnswerElementMatchers.HasDefinedStructureWithDefinitionLines(
        hostname, type, structureName, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link Zone}'s
   * interfaces.
   */
  public static @Nonnull Matcher<Zone> hasMemberInterfaces(
      @Nonnull Matcher<? super SortedSet<String>> subMatcher) {
    return new ZoneMatchers.HasInterfaces(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link Vrf}'s
   * eigrpProcess.
   */
  public static @Nonnull Matcher<Vrf> hasEigrpProcesses(
      @Nonnull Matcher<? super Map<Long, EigrpProcess>> subMatcher) {
    return new HasEigrpProcesses(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link Vrf}'s
   * isisProcess.
   */
  public static @Nonnull Matcher<Vrf> hasIsisProcess(
      @Nonnull Matcher<? super IsisProcess> subMatcher) {
    return new HasIsisProcess(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IpSpaceReference}'s {@code name}.
   */
  public static @Nonnull Matcher<IpSpaceReference> hasName(
      @Nonnull Matcher<? super String> subMatcher) {
    return new IpSpaceReferenceMatchers.HasName(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code name} is that of the {@link
   * IpSpaceReference}.
   */
  public static @Nonnull Matcher<IpSpaceReference> hasName(@Nonnull String name) {
    return hasName(equalTo(name));
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has
   * an undefined reference in {@code hostname} to a structure of type {@code type} named {@code
   * structureName}.
   */
  public static @Nonnull Matcher<ConvertConfigurationAnswerElement> hasUndefinedReference(
      @Nonnull String hostname, @Nonnull StructureType type, @Nonnull String structureName) {
    return new ConvertConfigurationAnswerElementMatchers.HasUndefinedReference(
        hostname, type, structureName);
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has
   * an undefined reference in {@code filename} to a structure of type {@code type} named {@code
   * structureName} of usage type {@code usage}.
   */
  public static @Nonnull Matcher<ConvertConfigurationAnswerElement> hasUndefinedReference(
      @Nonnull String filename,
      @Nonnull StructureType type,
      @Nonnull String structureName,
      @Nonnull StructureUsage usage) {
    return new ConvertConfigurationAnswerElementMatchers.HasUndefinedReferenceWithUsage(
        filename, type, structureName, usage);
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has
   * an undefined reference in {@code hostname} to a structure of type {@code type} named {@code
   * structureName} of usage type {@code usage} with reference lines matching the provided {@code
   * subMatcher}.
   */
  public static @Nonnull Matcher<ConvertConfigurationAnswerElement>
      hasUndefinedReferenceWithReferenceLines(
          @Nonnull String hostname,
          @Nonnull StructureType type,
          @Nonnull String structureName,
          @Nonnull StructureUsage usage,
          @Nonnull Matcher<? super Set<Integer>> subMatcher) {
    return new ConvertConfigurationAnswerElementMatchers.HasUndefinedReferenceWithReferenceLines(
        hostname, type, structureName, usage, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has a
   * structure for {@code hostname} of type {@code type} named {@code structureName} with {@code
   * numReferrers} referrers.
   */
  public static @Nonnull Matcher<ConvertConfigurationAnswerElement> hasNumReferrers(
      @Nonnull String hostname,
      @Nonnull StructureType type,
      @Nonnull String structureName,
      int numReferrers) {
    return new HasNumReferrers(hostname, type, structureName, numReferrers);
  }

  /**
   * Provides a matcher that matches if the object is a {@link DefaultDeniedByAclIpSpace} for the
   * {@link AclIpSpace} named {@code aclName}.
   */
  public static @Nonnull Matcher<TraceEvent> isDefaultDeniedByAclIpSpaceNamed(
      @Nonnull String aclName) {
    return new IsDefaultDeniedByAclIpSpaceThat(
        new DefaultDeniedByAclIpSpaceMatchers.HasName(equalTo(aclName)));
  }

  /**
   * Provides a matcher that matches if the object is a {@link DefaultDeniedByAclIpSpace} matched by
   * the provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<TraceEvent> isDefaultDeniedByAclIpSpaceThat(
      Matcher<? super DefaultDeniedByAclIpSpace> subMatcher) {
    return new IsDefaultDeniedByAclIpSpaceThat(subMatcher);
  }

  /**
   * Provides a matcher that matches if the object is a {@link DefaultDeniedByIpAccessList} for the
   * {@link IpAccessList} named {@code aclName}.
   */
  public static @Nonnull Matcher<TraceEvent> isDefaultDeniedByIpAccessListNamed(
      @Nonnull String aclName) {
    return new IsDefaultDeniedByIpAccessListThat(
        new DefaultDeniedByIpAccessListMatchers.HasName(equalTo(aclName)));
  }

  /**
   * Provides a matcher that matches if the object is a {@link DefaultDeniedByIpAccessList} matched
   * by the provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<TraceEvent> isDefaultDeniedByIpAccessListThat(
      Matcher<? super DefaultDeniedByIpAccessList> subMatcher) {
    return new IsDefaultDeniedByIpAccessListThat(subMatcher);
  }

  /**
   * Provides a matcher that matches if the object is a {@link DeniedByAclIpSpaceLine} matched by
   * the provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<TraceEvent> isDeniedByAclIpSpaceLineThat(
      Matcher<? super DeniedByAclIpSpaceLine> subMatcher) {
    return new IsDeniedByAclIpSpaceLineThat(subMatcher);
  }

  /**
   * Provides a matcher that matches if the object is a {@link DeniedByIpAccessListLine} matched by
   * the provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<TraceEvent> isDeniedByIpAccessListLineThat(
      Matcher<? super DeniedByIpAccessListLine> subMatcher) {
    return new IsDeniedByIpAccessListLineThat(subMatcher);
  }

  /**
   * Provides a matcher that matches if the object is a {@link DeniedByNamedIpSpace} for the {@link
   * IpSpace} named {@code ipSpaceName}.
   */
  public static @Nonnull Matcher<TraceEvent> isDeniedByNamedIpSpace(@Nonnull String ipSpaceName) {
    return new IsDeniedByNamedIpSpaceThat(
        new DeniedByNamedIpSpaceMatchers.HasName(equalTo(ipSpaceName)));
  }

  /**
   * Provides a matcher that matches if the object is a {@link PermittedByAclIpSpaceLine} matched by
   * the provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<TraceEvent> isPermittedByAclIpSpaceLineThat(
      Matcher<? super PermittedByAclIpSpaceLine> subMatcher) {
    return new IsPermittedByAclIpSpaceLineThat(subMatcher);
  }

  /**
   * Provides a matcher that matches if the object is a {@link PermittedByIpAccessListLine} matched
   * by the provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<TraceEvent> isPermittedByIpAccessListLineThat(
      Matcher<? super PermittedByIpAccessListLine> subMatcher) {
    return new IsPermittedByIpAccessListLineThat(subMatcher);
  }

  /**
   * Provides a matcher that matches if the object is a {@link PermittedByNamedIpSpace} for the
   * {@link IpSpace} named {@code ipSpaceName}.
   */
  public static @Nonnull Matcher<TraceEvent> isPermittedByNamedIpSpace(
      @Nonnull String ipSpaceName) {
    return new IsPermittedByNamedIpSpaceThat(
        new PermittedByNamedIpSpaceMatchers.HasName(equalTo(ipSpaceName)));
  }

  /**
   * Provides a matcher that matches if the object is an {@link IpSpaceReference} matched by the
   * provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<IpSpace> isIpSpaceReferenceThat(
      @Nonnull Matcher<? super IpSpaceReference> subMatcher) {
    return new IpSpaceReferenceMatchers.IsIpSpaceReferenceThat(subMatcher);
  }

  /**
   * Provides a matcher that matches if the object is a {@link PermittedByAcl} matched by the
   * provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<AclLineMatchExpr> isPermittedByAclThat(
      @Nonnull Matcher<? super PermittedByAcl> subMatcher) {
    return new PermittedByAclMatchers.IsPermittedByAclThat(subMatcher);
  }

  private DataModelMatchers() {}
}
