package org.batfish.datamodel;

public enum EncryptionAlgorithm {
  AES_128_CBC,
  AES_192_CBC,
  AES_256_CBC,
  AES_128_GCM,
  AES_192_GCM,
  AES_256_GCM,
  AES_128_GMAC,
  AES_192_GMAC,
  AES_256_GMAC,
  DES_CBC,
  NULL,
  SEAL_160,
  THREEDES_CBC
}
