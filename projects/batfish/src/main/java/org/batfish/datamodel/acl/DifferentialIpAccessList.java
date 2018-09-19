package org.batfish.datamodel.acl;

import static org.batfish.datamodel.IpAccessListLine.rejecting;
import static org.batfish.datamodel.acl.AclLineMatchExprs.not;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.visitors.IpSpaceRenamer;

/**
 * Given two {@link IpAccessList ACLs} {@code denyAcl} and {@code permitAcl}, generate a new ACL
 * that permits exactly those flows that are permitted by {@code permitAcl} and denied by {@code
 * denyAcl} -- i.e. the difference between them.
 *
 * <p>Constructing such an ACL is simple when they have the same context (i.e. the same sets of
 * named {@link IpAccessList ACLs} and named {@link IpSpace IP spaces}. Where it gets tricky is when
 * they have possibly different sets, possibly with the name collisions. This is a common situation
 * when comparing two versions of the same ACL (e.g. from two different {@link
 * org.batfish.common.Snapshot snapshots}. We do rename the {@code denyAcl}'s context to avoid
 * collisions.
 */
public final class DifferentialIpAccessList {
  @VisibleForTesting static final String DIFFERENTIAL_ACL_NAME = " ~~ Differential ACL Name ~~ ";

  @VisibleForTesting
  static final Function<String, String> RENAMER = name -> String.format("~~ Deny ACL: %s ~~", name);

  private final IpAccessList _acl;

  private final Map<String, IpAccessList> _namedAcls;

  private final Map<String, IpSpace> _namedIpSpaces;

  private DifferentialIpAccessList(
      IpAccessList acl, Map<String, IpAccessList> namedAcls, Map<String, IpSpace> namedIpSpaces) {
    _acl = acl;
    _namedAcls = ImmutableMap.copyOf(namedAcls);
    _namedIpSpaces = ImmutableMap.copyOf(namedIpSpaces);
  }

  public static DifferentialIpAccessList create(
      AclLineMatchExpr invariantExpr,
      IpAccessList denyAcl,
      Map<String, IpAccessList> denyNamedAcls,
      Map<String, IpSpace> denyNamedIpSpaces,
      IpAccessList permitAcl,
      Map<String, IpAccessList> permitNamedAcls,
      Map<String, IpSpace> permitNamedIpSpaces) {
    IpSpaceRenamer ipSpaceRenamer = new IpSpaceRenamer(RENAMER);
    IpAccessListRenamer aclRenamer = new IpAccessListRenamer(RENAMER, ipSpaceRenamer);
    /*
     * Create a new ACL for "matched by permitAcl but not by denyAcl"
     */
    String denyAclName = RENAMER.apply(denyAcl.getName());
    IpAccessList differentialAcl =
        IpAccessList.builder()
            .setName(DIFFERENTIAL_ACL_NAME)
            .setLines(
                ImmutableList.<IpAccessListLine>builder()
                    // reject if invariant not satisfied
                    .add(rejecting(not(invariantExpr)))
                    // reject if permitted by denyAcl
                    .add(rejecting(new PermittedByAcl(denyAclName)))
                    .addAll(permitAcl.getLines())
                    .build())
            .build();
    /*
     * Create namedAcls map for differentialAcl
     */
    Map<String, IpAccessList> namedAcls =
        ImmutableMap.<String, IpAccessList>builder()
            // include all the renamed denyNamedAcls
            .putAll(
                denyNamedAcls
                    .entrySet()
                    .stream()
                    .map(
                        entry ->
                            Maps.immutableEntry(
                                RENAMER.apply(entry.getKey()), aclRenamer.apply(entry.getValue())))
                    .collect(Collectors.toList()))
            // entry for the renamed denyAcl itself
            .put(denyAclName, aclRenamer.apply(denyAcl))
            // include all the permitNamedAcls (no need to rename).
            .putAll(permitNamedAcls)
            .build();
    /*
     * Create namedIpSpaces map for differentialAcl
     */
    Map<String, IpSpace> namedIpSpaces =
        ImmutableMap.<String, IpSpace>builder()
            // include all the renamed denyIpSpaces
            .putAll(
                denyNamedIpSpaces
                    .entrySet()
                    .stream()
                    .map(
                        entry ->
                            Maps.immutableEntry(
                                RENAMER.apply(entry.getKey()),
                                ipSpaceRenamer.apply(entry.getValue())))
                    .collect(Collectors.toList()))
            // include add the permitIpSpaces (no need to rename).
            .putAll(permitNamedIpSpaces)
            .build();
    return new DifferentialIpAccessList(differentialAcl, namedAcls, namedIpSpaces);
  }

  public IpAccessList getAcl() {
    return _acl;
  }

  public Map<String, IpAccessList> getNamedAcls() {
    return _namedAcls;
  }

  public Map<String, IpSpace> getNamedIpSpaces() {
    return _namedIpSpaces;
  }
}
