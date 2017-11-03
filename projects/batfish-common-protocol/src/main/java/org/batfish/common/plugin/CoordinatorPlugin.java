package org.batfish.common.plugin;

import org.batfish.common.BatfishLogger;

public abstract class CoordinatorPlugin extends Plugin {

  protected ICoordinator _coordinator;

  protected BatfishLogger _logger;

  protected abstract void coordinatorPluginInitialize();

  @Override
  protected final void pluginInitialize() {
    switch (_pluginConsumer.getType()) {
      case COORDINATOR:
        _coordinator = (ICoordinator) _pluginConsumer;
        _logger = _coordinator.getLogger();
        coordinatorPluginInitialize();
        break;
      default:
        break;
    }
  }
}
