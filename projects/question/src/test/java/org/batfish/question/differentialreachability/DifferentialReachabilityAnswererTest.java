package org.batfish.question.differentialreachability;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.common.topology.IpOwners;
import org.batfish.common.util.CollectionUtil;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.referencelibrary.ReferenceBook;
import org.batfish.role.NodeRoleDimension;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.Location;
import org.batfish.specifier.SpecifierContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Test for {@link DifferentialReachabilityAnswerer}. */
public final class DifferentialReachabilityAnswererTest {
  @Rule public TemporaryFolder _tmp = new TemporaryFolder();

  private static Configuration getResolveStartLocationsConfig(
      String hostname, String i0Name, String i1Name) {
    NetworkFactory nf = new NetworkFactory();
    Configuration node =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(hostname)
            .build();
    Vrf vrf = nf.vrfBuilder().setOwner(node).build();
    Interface.Builder ib = nf.interfaceBuilder().setOwner(node).setVrf(vrf).setActive(true);
    ib.setName(i0Name).setAddress(ConcreteInterfaceAddress.parse("1.1.1.1/24")).build();
    ib.setName(i1Name).setAddress(ConcreteInterfaceAddress.parse("2.2.2.2/24")).build();
    return node;
  }

  @Test
  public void testResolveStartLocations() {
    String hostname = "hostname";
    String i0Name = "i0";
    String i1Name = "i1";

    Configuration snapshotConfig = getResolveStartLocationsConfig(hostname, i0Name, i1Name);
    snapshotConfig.getAllInterfaces().get(i0Name).setActive(false); // fail i0

    Configuration referenceConfig = getResolveStartLocationsConfig(hostname, i0Name, i1Name);

    SortedMap<String, Configuration> snapshotConfigs =
        ImmutableSortedMap.of(snapshotConfig.getHostname(), snapshotConfig);

    SortedMap<String, Configuration> referenceConfigs =
        ImmutableSortedMap.of(referenceConfig.getHostname(), referenceConfig);

    SpecifierContext snapshotSpecifierContext = getMockSpecifierContext(snapshotConfigs);
    SpecifierContext referenceSpecifierContext = getMockSpecifierContext(referenceConfigs);

    NetworkId network = new NetworkId("network");
    NetworkSnapshot snapshot = new NetworkSnapshot(network, new SnapshotId("snapshot"));
    NetworkSnapshot reference = new NetworkSnapshot(network, new SnapshotId("reference"));

    IBatfish mockBatfish =
        new IBatfishTestAdapter() {
          @Override
          public SpecifierContext specifierContext(NetworkSnapshot networkSnapshot) {
            return networkSnapshot.equals(snapshot)
                ? snapshotSpecifierContext
                : referenceSpecifierContext;
          }
        };

    DifferentialReachabilityParameters parameters =
        new DifferentialReachabilityAnswerer(new DifferentialReachabilityQuestion(), mockBatfish)
            .parameters(snapshot, reference);

    Set<Location> startLocations =
        parameters.getIpSpaceAssignment().getEntries().stream()
            .map(IpSpaceAssignment.Entry::getLocations)
            .flatMap(Collection::stream)
            .collect(ImmutableSet.toImmutableSet());

    Set<Location> expected = ImmutableSet.of(new InterfaceLocation(hostname, i1Name));
    assertEquals(expected, startLocations);
  }

  @Nonnull
  private static SpecifierContext getMockSpecifierContext(
      SortedMap<String, Configuration> snapshotConfigs) {
    return new SpecifierContext() {
      @Nonnull
      @Override
      public Map<String, Configuration> getConfigs() {
        return snapshotConfigs;
      }

      @Override
      public Optional<ReferenceBook> getReferenceBook(String bookName) {
        return Optional.empty();
      }

      @Nonnull
      @Override
      public Optional<NodeRoleDimension> getNodeRoleDimension(@Nullable String dimension) {
        return Optional.empty();
      }

      @Override
      public Map<String, Map<String, IpSpace>> getInterfaceOwnedIps() {
        return IpOwners.computeInterfaceOwnedIpSpaces(
            IpOwners.computeIpInterfaceOwners(
                CollectionUtil.toImmutableMap(
                    snapshotConfigs,
                    Entry::getKey,
                    entry -> ImmutableSet.copyOf(entry.getValue().getAllInterfaces().values())),
                true));
      }

      @Override
      public IpSpace getSnapshotDeviceOwnedIps() {
        return null;
      }
    };
  }
}
