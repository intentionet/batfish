package org.batfish.bdp;

import org.batfish.common.BdpSettings;

public class TestBdpSettings implements BdpSettings {

  private boolean _bdpDetail;

  private int _bdpMaxOscillationRecoveryAttempts;

  private int _bdpMaxRecordedIterations;

  private boolean _bdpPrintAllIterations;

  private boolean _bdpPrintOscillatingIterations;

  private boolean _bdpRecordAllIterations;

  public TestBdpSettings() {
    _bdpDetail = true;
    _bdpMaxOscillationRecoveryAttempts = 0;
    _bdpMaxRecordedIterations = 2;
    _bdpPrintAllIterations = false;
    _bdpPrintOscillatingIterations = false;
    _bdpRecordAllIterations = false;
  }

  public boolean getBdpDetail() {
    return _bdpDetail;
  }

  public int getBdpMaxOscillationRecoveryAttempts() {
    return _bdpMaxOscillationRecoveryAttempts;
  }

  public int getBdpMaxRecordedIterations() {
    return _bdpMaxRecordedIterations;
  }

  public boolean getBdpPrintAllIterations() {
    return _bdpPrintAllIterations;
  }

  public boolean getBdpPrintOscillatingIterations() {
    return _bdpPrintOscillatingIterations;
  }

  public boolean getBdpRecordAllIterations() {
    return _bdpRecordAllIterations;
  }

  public void setBdpDetail(boolean bdpDetail) {
    _bdpDetail = bdpDetail;
  }

  public void setBdpMaxOscillationRecoveryAttempts(int bdpMaxOscillationRecoveryAttempts) {
    _bdpMaxOscillationRecoveryAttempts = bdpMaxOscillationRecoveryAttempts;
  }

  public void setBdpMaxRecordedIterations(int bdpMaxRecordedIterations) {
    _bdpMaxRecordedIterations = bdpMaxRecordedIterations;
  }

  public void setBdpPrintAllIterations(boolean bdpPrintAllIterations) {
    _bdpPrintAllIterations = bdpPrintAllIterations;
  }

  public void setBdpPrintOscillatingIterations(boolean bdpPrintOscillatingIterations) {
    _bdpPrintOscillatingIterations = bdpPrintOscillatingIterations;
  }

  public void setBdpRecordAllIterations(boolean bdpRecordAllIterations) {
    _bdpRecordAllIterations = bdpRecordAllIterations;
  }
}
