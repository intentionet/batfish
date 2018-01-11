package org.batfish.common;

import org.batfish.common.plugin.DataPlanePluginSettings;

public interface BdpSettings extends DataPlanePluginSettings {

  boolean getBdpDetail();

  int getBdpMaxOscillationRecoveryAttempts();

  int getBdpMaxRecordedIterations();

  boolean getBdpPrintAllIterations();

  boolean getBdpPrintOscillatingIterations();

  boolean getBdpRecordAllIterations();

  void setBdpDetail(boolean bdpDetail);

  void setBdpMaxOscillationRecoveryAttempts(int bdpMaxOscillationRecoveryAttempts);

  void setBdpMaxRecordedIterations(int bdpMaxRecordedIterations);

  void setBdpPrintAllIterations(boolean bdpPrintAllIterations);

  void setBdpPrintOscillatingIterations(boolean bdpPrintErrorIterations);

  void setBdpRecordAllIterations(boolean bdpRecordAllIterations);
}
