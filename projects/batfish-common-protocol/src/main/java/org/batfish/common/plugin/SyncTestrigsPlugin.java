package org.batfish.common.plugin;

import java.util.Map;

/**
 * {@link SyncTestrigsPlugin} sync testrig from an external data source (e.g., git, cvs, ..)
 *
 * <p>The intended model that concrete impelementations should follow is:
 *
 * <p>In their implementation of the {@link SyncTestrigsPlugin#syncTestrigsPluginInitialize()} the
 * concrete plugin should register with the coordinator by calling {@link
 * ICoordinator#registerTestrigSyncer(String, SyncTestrigsPlugin)}
 *
 * <p>{@link SyncTestrigsPlugin#updateSettings(String, Map)} will invoked by the coordinator when
 * the user provides or updates the settings needed to sync the testrigs (e.g., repositoryUrl,
 * username, password). It is up to the plugin implementation to define what settings are needed and
 * the assumption is that their users know what is needed.
 *
 * <p>{@link SyncTestrigsPlugin#syncNow(String, boolean)} will be invoked by the coordinator upon
 * user request, and its concrete implementation should create testrigs in the usual place. The
 * force flag is intended to overwrite any previous testrigs that conflict with those that will be
 * newly installed.
 *
 * <p>If regular polling is needed to keep things in sync, the plugin should set things up properly
 * when initialized and when settings are updated.
 */
public abstract class SyncTestrigsPlugin extends CoordinatorPlugin {

  @Override
  protected final void coordinatorPluginInitialize() {
    syncTestrigsPluginInitialize();
  }

  public abstract int syncNow(String container, boolean force);

  public abstract void syncTestrigsPluginInitialize();

  public abstract boolean updateSettings(String container, Map<String, String> settings);
}
