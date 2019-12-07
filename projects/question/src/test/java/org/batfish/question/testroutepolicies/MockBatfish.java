package org.batfish.question.testroutepolicies;

import com.google.common.collect.ImmutableSortedMap;
import java.util.SortedMap;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.Configuration;

final class MockBatfish extends IBatfishTestAdapter {
  private final SortedMap<String, Configuration> _baseConfigs;
  private final SortedMap<String, Configuration> _deltaConfigs;

  MockBatfish(
      SortedMap<String, Configuration> baseConfigs, SortedMap<String, Configuration> deltaConfigs) {
    _baseConfigs = ImmutableSortedMap.copyOf(baseConfigs);
    _deltaConfigs = ImmutableSortedMap.copyOf(deltaConfigs);
  }

  @Override
  public SortedMap<String, Configuration> loadConfigurations(NetworkSnapshot snapshot) {
    if (getSnapshot().equals(snapshot)) {
      return _baseConfigs;
    } else if (getReferenceSnapshot().equals(snapshot)) {
      return _deltaConfigs;
    }
    throw new IllegalArgumentException("Unknown snapshot: " + snapshot);
  }
}
