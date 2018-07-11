package org.batfish.representation.juniper;

import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.isis.IsisAuthenticationAlgorithm;
import org.batfish.datamodel.isis.IsisOption;

public class JuniperAuthenticationKey extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private static final IsisAuthenticationAlgorithm DEFAULT_ISIS_AUTHENTICATION_ALGORITHM =
      IsisAuthenticationAlgorithm.MD5;

  private static final IsisOption DEFAULT_ISIS_OPTION = IsisOption.BASIC;

  private IsisAuthenticationAlgorithm _isisAuthenticationAlgorithm;

  private IsisOption _isisOption;

  private String _secret;

  private String _startTime;

  public JuniperAuthenticationKey(String name) {
    super(name);
    _isisAuthenticationAlgorithm = DEFAULT_ISIS_AUTHENTICATION_ALGORITHM;
    _isisOption = DEFAULT_ISIS_OPTION;
  }

  public IsisAuthenticationAlgorithm getIsisAuthenticationAlgorithm() {
    return _isisAuthenticationAlgorithm;
  }

  public IsisOption getIsisOption() {
    return _isisOption;
  }

  public String getSecret() {
    return _secret;
  }

  public String getStartTime() {
    return _startTime;
  }

  public void setIsisAuthenticationAlgorithm(IsisAuthenticationAlgorithm isisAlgorithm) {
    _isisAuthenticationAlgorithm = isisAlgorithm;
  }

  public void setIsisOption(IsisOption isisOption) {
    _isisOption = isisOption;
  }

  public void setSecret(String secret) {
    _secret = secret;
  }

  public void setStartTime(String startTime) {
    _startTime = startTime;
  }
}
