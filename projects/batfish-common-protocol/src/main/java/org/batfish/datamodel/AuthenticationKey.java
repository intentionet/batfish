package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.isis.IsisAuthenticationAlgorithm;
import org.batfish.datamodel.isis.IsisOption;

public class AuthenticationKey extends ComparableStructure<String> {

  private static final String PROP_BGP_AUTHENTICATION_ALGORITHM = "bgpAuthenticationAlgorithm";

  private static final String PROP_ISIS_AUTHENTICATION_ALGORITHM = "isisAuthenticationAlgorithm";

  private static final String PROP_ISIS_OPTION = "isisOption";

  private static final String PROP_SECRET = "secret";

  private static final String PROP_START_TIME = "startTime";

  private static final long serialVersionUID = 1L;

  private static final IsisAuthenticationAlgorithm DEFAULT_ISIS_AUTHENTICATION_ALGORITHM =
      IsisAuthenticationAlgorithm.MD5;

  private static final IsisOption DEFAULT_ISIS_OPTION = IsisOption.BASIC;

  private BgpAuthenticationAlgorithm _bgpAuthenticationAlgorithm;

  private IsisAuthenticationAlgorithm _isisAuthenticationAlgorithm;

  private IsisOption _isisOption;

  private String _secret;

  private String _startTime;

  @JsonCreator
  public AuthenticationKey(@JsonProperty(PROP_NAME) String name) {
    super(name);
    _isisAuthenticationAlgorithm = DEFAULT_ISIS_AUTHENTICATION_ALGORITHM;
    _isisOption = DEFAULT_ISIS_OPTION;
  }

  @JsonProperty(PROP_BGP_AUTHENTICATION_ALGORITHM)
  public BgpAuthenticationAlgorithm getBgpAuthenticationAlgorithm() {
    return _bgpAuthenticationAlgorithm;
  }

  @JsonProperty(PROP_ISIS_AUTHENTICATION_ALGORITHM)
  public IsisAuthenticationAlgorithm getIsisAuthenticationAlgorithm() {
    return _isisAuthenticationAlgorithm;
  }

  @JsonProperty(PROP_ISIS_OPTION)
  public IsisOption getIsisOption() {
    return _isisOption;
  }

  @JsonProperty(PROP_SECRET)
  public String getSecret() {
    return _secret;
  }

  @JsonProperty(PROP_START_TIME)
  public String getStartTime() {
    return _startTime;
  }

  @JsonProperty(PROP_BGP_AUTHENTICATION_ALGORITHM)
  public void setBgpAuthenticationAlgorithm(BgpAuthenticationAlgorithm bgpAlgorithm) {
    _bgpAuthenticationAlgorithm = bgpAlgorithm;
  }

  @JsonProperty(PROP_ISIS_AUTHENTICATION_ALGORITHM)
  public void setIsisAuthenticationAlgorithm(IsisAuthenticationAlgorithm isisAlgorithm) {
    _isisAuthenticationAlgorithm = isisAlgorithm;
  }

  @JsonProperty(PROP_ISIS_OPTION)
  public void setIsisOption(IsisOption isisOption) {
    _isisOption = isisOption;
  }

  @JsonProperty(PROP_SECRET)
  public void setSecret(String secret) {
    _secret = secret;
  }

  @JsonProperty(PROP_START_TIME)
  public void setStartTime(String startTime) {
    _startTime = startTime;
  }
}
