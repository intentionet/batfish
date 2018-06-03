package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.batfish.common.util.ComparableStructure;

public class IpsecProposal extends ComparableStructure<String> {

  public static final IpsecProposal G2_ESP_3DES_SHA = initG2_ESP_3DES_SHA();

  public static final IpsecProposal G2_ESP_AES128_SHA = initG2_ESP_AES128_SHA();

  public static final IpsecProposal NOPFS_ESP_3DES_MD5 = initNOPFS_ESP_3DES_MD5();

  public static final IpsecProposal NOPFS_ESP_3DES_SHA = initNOPFS_ESP_3DES_SHA();

  public static final IpsecProposal NOPFS_ESP_DES_MD5 = initNOPFS_ESP_DES_MD5();

  public static final IpsecProposal NOPFS_ESP_DES_SHA = initNOPFS_ESP_DES_SHA();

  /** */
  private static final long serialVersionUID = 1L;

  private static IpsecProposal initG2_ESP_3DES_SHA() {
    IpsecProposal p = new IpsecProposal("G2_ESP_3DES_SHA");
    p.setProtocol(IpsecProtocol.ESP);
    p.setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_SHA1_96);
    p.setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC);
    return p;
  }

  private static IpsecProposal initG2_ESP_AES128_SHA() {
    IpsecProposal p = new IpsecProposal("G2_ESP_AES128_SHA");
    p.setProtocol(IpsecProtocol.ESP);
    p.setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_SHA1_96);
    p.setEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC);
    return p;
  }

  private static IpsecProposal initNOPFS_ESP_3DES_MD5() {
    IpsecProposal p = new IpsecProposal("NOPFS_ESP_3DES_MD5");
    p.setProtocol(IpsecProtocol.ESP);
    p.setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_MD5_96);
    p.setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC);
    return p;
  }

  private static IpsecProposal initNOPFS_ESP_3DES_SHA() {
    IpsecProposal p = new IpsecProposal("NOPFS_ESP_3DES_SHA");
    p.setProtocol(IpsecProtocol.ESP);
    p.setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_SHA1_96);
    p.setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC);
    return p;
  }

  private static IpsecProposal initNOPFS_ESP_DES_MD5() {
    IpsecProposal p = new IpsecProposal("NOPFS_ESP_DES_MD5");
    p.setProtocol(IpsecProtocol.ESP);
    p.setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_MD5_96);
    p.setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC);
    return p;
  }

  private static IpsecProposal initNOPFS_ESP_DES_SHA() {
    IpsecProposal p = new IpsecProposal("NOPFS_ESP_DES_SHA");
    p.setProtocol(IpsecProtocol.ESP);
    p.setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_SHA1_96);
    p.setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC);
    return p;
  }

  private IpsecAuthenticationAlgorithm _authenticationAlgorithm;

  private final int _definitionLine;

  private EncryptionAlgorithm _encryptionAlgorithm;

  private Integer _lifetimeKilobytes;

  private Integer _lifetimeSeconds;

  private IpsecProtocol _protocol;

  public IpsecProposal(String name) {
    super(name);
    _definitionLine = -1;
  }

  public boolean compatibleWith(IpsecProposal rhs) {
    return (_authenticationAlgorithm == rhs._authenticationAlgorithm
        && _encryptionAlgorithm == rhs._encryptionAlgorithm
        && _protocol == rhs._protocol);
  }

  public IpsecAuthenticationAlgorithm getAuthenticationAlgorithm() {
    return _authenticationAlgorithm;
  }

  @JsonIgnore
  public int getDefinitionLine() {
    return _definitionLine;
  }

  public EncryptionAlgorithm getEncryptionAlgorithm() {
    return _encryptionAlgorithm;
  }

  public Integer getLifetimeKilobytes() {
    return _lifetimeKilobytes;
  }

  public Integer getLifetimeSeconds() {
    return _lifetimeSeconds;
  }

  public IpsecProtocol getProtocol() {
    return _protocol;
  }

  public void setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm alg) {
    _authenticationAlgorithm = alg;
  }

  public void setEncryptionAlgorithm(EncryptionAlgorithm alg) {
    _encryptionAlgorithm = alg;
  }

  public void setLifetimeKilobytes(int lifetimeKilobytes) {
    _lifetimeKilobytes = lifetimeKilobytes;
  }

  public void setLifetimeSeconds(int lifetimeSeconds) {
    _lifetimeSeconds = lifetimeSeconds;
  }

  public void setProtocol(IpsecProtocol protocol) {
    _protocol = protocol;
  }
}
