package org.batfish.common.plugin;

import java.util.HashMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public abstract class AbstractCoordinator extends PluginConsumer implements ICoordinator {

  protected final Map<String, SyncTestrigsPlugin> _testrigSyncers;

  public AbstractCoordinator(boolean serializeToText) {
    super(serializeToText);
    _testrigSyncers = new HashMap<>();
  }

  @Override
  public final PluginClientType getType() {
    return PluginClientType.COORDINATOR;
  }

  @Override
  public final void registerTestrigSyncer(String name, SyncTestrigsPlugin plugin) {
    if (_testrigSyncers.containsKey(name)) {
      throw new BatfishException("Multiple SyncTestrigs plugins are registering for " + name);
    }
    _testrigSyncers.put(name, plugin);
  }
}
