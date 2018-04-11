parser grammar Cisco_crypto;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

cc_certificate_chain
:
   CERTIFICATE CHAIN name = variable NEWLINE cccc_certificate*
;

cc_lookup
:
   LOOKUP null_rest_of_line
;

cc_server
:
   SERVER NEWLINE
   (
      ccs_null
   )*
;

cc_trustpoint
:
   TRUSTPOINT name = variable NEWLINE
   (
      cctpoint_null
   )*
;

cc_trustpool
:
   TRUSTPOOL name = variable NEWLINE
   (
      cctpool_null
   )*
;

cccc_certificate
:
   CERTIFICATE
   (
      CA
      | SELF_SIGNED
   )? certificate QUIT NEWLINE
;

ccs_null
:
   NO?
   (
      CDP_URL
      | ISSUER_NAME
      | SHUTDOWN
      | SMTP
   ) null_rest_of_line
;

cctpoint_null
:
   NO?
   (
      CRL
      | ENROLLMENT
      | FQDN
      | KEYPAIR
      | SUBJECT_NAME
      | VALIDATION_USAGE
   ) null_rest_of_line
;

cctpool_null
:
   NO?
   (
      AUTO_IMPORT
   ) null_rest_of_line
;

cd_null
:
   NO?
   (
      VERSION
   ) null_rest_of_line
;

cd_set
:
   SET null_rest_of_line
;

certificate
:
   ~QUIT+
;

ci1_null
:
   (
      AM_DISABLE
      | ENABLE
      | IPSEC_OVER_TCP
   ) null_rest_of_line
;

ci1_policy
:
   POLICY name = variable NEWLINE
   (
      ci1p_null
   )*
;

ci1p_null
:
   NO?
   (
      AUTHENTICATION
      | ENCRYPTION
      | GROUP
      | HASH
      | LIFETIME
   ) null_rest_of_line
;

ci2_keyring
:
   KEYRING name = variable NEWLINE
   (
      ci2k_peer
   )*
;

ci2_null
:
   (
      ENABLE
      | REMOTE_ACCESS
   ) null_rest_of_line
;

ci2_policy
:
   POLICY name = variable NEWLINE
   (
      ci2pol_null
   )*
;

ci2_profile
:
   PROFILE name = variable NEWLINE
   (
      ci2prf_null
   )*
;

ci2_proposal
:
   PROPOSAL name = variable NEWLINE
   (
      ci2prp_null
   )*
;

ci2k_peer
:
   PEER name = variable NEWLINE
   (
      ci2kp_null
   )*
;

ci2kp_null
:
   NO?
   (
      ADDRESS
      | PRE_SHARED_KEY
   ) null_rest_of_line
;

ci2pol_null
:
   NO?
   (
      ENCRYPTION
      | GROUP
      | INTEGRITY
      | LIFETIME
      | PRF
      | PROPOSAL
   ) null_rest_of_line
;

ci2prf_null
:
   NO?
   (
      AUTHENTICATION
      | KEYRING
      | MATCH
   ) null_rest_of_line
;

ci2prp_null
:
   NO?
   (
      ENCRYPTION
      | GROUP
      | INTEGRITY
   ) null_rest_of_line
;

cip_ikev2
:
   IKEV2 cipi2_ipsec_proposal
;

cip_null
:
   (
      DF_BIT
      | IKEV1
      | NAT_TRANSPARENCY
      | SECURITY_ASSOCIATION
   ) null_rest_of_line
;

cip_profile
:
   PROFILE name = variable_permissive NEWLINE
   (
      cipprf_set
   )*
;

cip_transform_set
:
   TRANSFORM_SET name = variable ipsec_encryption ipsec_authentication NEWLINE
   (
      cipt_mode
   )*
;

cipi2_ipsec_proposal
:
   IPSEC_PROPOSAL name = variable_permissive NEWLINE
   (
      cipi2ip_null
   )*
;

cipi2ip_null
:
   NO?
   (
      PROTOCOL
   ) null_rest_of_line
;

cipprf_set
:
   SET
   (
      cipprf_set_null
      | cipprf_set_pfs
      | cipprf_set_transform_set
   )
;

cipprf_set_null
:
   (
      IKEV2_PROFILE
   ) null_rest_of_line
;

cipprf_set_pfs
:
   PFS dh_group NEWLINE
;

cipprf_set_transform_set
:
   TRANSFORM_SET name = variable NEWLINE
;

cipt_mode
:
   MODE
   (
     TRANSPORT
     | TUNNEL
   ) NEWLINE
;

cis_null
:
   (
      EAP_PASSTHROUGH
      | ENABLE
      | IDENTITY
      | INVALID_SPI_RECOVERY
      | KEEPALIVE
      | KEY
      | NAT
      | NAT_TRAVERSAL
   ) null_rest_of_line
;

cis_policy
:
   POLICY name = variable NEWLINE
   (
      cispol_authentication
      | cispol_encr        //cisco
      | cispol_encryption  //aruba
      | cispol_group
      | cispol_hash
      | cispol_lifetime
      | cispol_null
   )*
;

cis_profile
:
   PROFILE name = variable NEWLINE
   (
      cisprf_keyring
      | cisprf_local_address
      | cisprf_match
      | cisprf_null
   )*
;

cispol_authentication
:
   AUTHENTICATION
   (
      PRE_SHARE
      | RSA_SIG
   ) NEWLINE
;

cispol_encr
:
   ENCR ike_encryption NEWLINE
;

cispol_encryption
:
   ENCRYPTION ike_encryption_aruba NEWLINE
;

cispol_group
:
   GROUP DEC NEWLINE
;

cispol_hash
:
   HASH
   (
      MD5
      | SHA2_256_128
   ) NEWLINE
;

cispol_lifetime
:
    LIFETIME DEC NEWLINE
;

cispol_null
:
   NO?
   (
      PRF
      | VERSION
   ) null_rest_of_line
;

cisprf_keyring
:
   KEYRING name = variable_permissive NEWLINE
;

cisprf_local_address
:
   LOCAL_ADDRESS IP_ADDRESS NEWLINE
;

cisprf_match
:
   MATCH IDENTITY ADDRESS address = IP_ADDRESS mask = IP_ADDRESS NEWLINE
;

cisprf_null
:
   NO?
   (
      REVERSE_ROUTE
      | VRF
   ) null_rest_of_line
;

ck_null
:
   (
      GENERATE
      | PARAM
   ) null_rest_of_line
;

ck_pubkey_chain
:
   PUBKEY_CHAIN null_rest_of_line ckp_named_key*
;

ckp_named_key
:
   NAMED_KEY name = variable_permissive NEWLINE
   (
      ckpn_key_string
      | ckpn_null
   )*
;

ckpn_key_string
:
   KEY_STRING certificate QUIT NEWLINE
;

ckpn_null
:
   NO?
   (
      ADDRESS
   ) null_rest_of_line
;

ckr_local_address
:
   LOCAL_ADDRESS IP_ADDRESS NEWLINE
;

ckr_psk
:
   PRE_SHARED_KEY ADDRESS IP_ADDRESS KEY variable_permissive NEWLINE
;

cpki_certificate_chain
:
   CERTIFICATE CHAIN name = variable_permissive NEWLINE
   (
      cpkicc_certificate
   )*
;

cpki_null
:
   (
      TOKEN
   ) null_rest_of_line
;

cpki_server
:
   SERVER name = variable_permissive NEWLINE
   (
      cpkis_null
   )*
;

cpki_trustpoint
:
   TRUSTPOINT name = variable_permissive NEWLINE
   (
      cpkit_null
   )*
;

cpkicc_certificate
:
   CERTIFICATE
   (
      SELF_SIGNED
   )? certificate QUIT NEWLINE
;

cpkis_null
:
   NO?
   (
      CDP_URL
      | DATABASE
      | GRANT
      | ISSUER_NAME
   ) null_rest_of_line
;

cpkit_null
:
   NO?
   (
      ENROLLMENT
      | REVOCATION_CHECK
      | RSAKEYPAIR
      | SERIAL_NUMBER
      | SUBJECT_NAME
      | VALIDATION_USAGE
   ) null_rest_of_line
;

crypto_ca
:
   CA
   (
      cc_certificate_chain
      | cc_lookup
      | cc_server
      | cc_trustpoint
      | cc_trustpool
   )
;

crypto_dynamic_map
:
   DYNAMIC_MAP name = variable num = DEC
   (
      cd_set
      |
      (
         NEWLINE
         (
            cd_null
            | cd_set
         )*
      )
   )
;

crypto_engine
:
   ENGINE null_rest_of_line
;

crypto_ikev1
:
   IKEV1
   (
      ci1_null
      | ci1_policy
   )
;

crypto_ikev2
:
   IKEV2
   (
      ci2_keyring
      | ci2_null
      | ci2_policy
      | ci2_profile
      | ci2_proposal
   )
;

crypto_ipsec
:
   IPSEC
   (
      cip_ikev2
      | cip_null
      | cip_profile
      | cip_transform_set
   )
;

crypto_isakmp
:
   ISAKMP
   (
      cis_null
      | cis_policy
      | cis_profile
   )
;

crypto_key
:
   KEY
   (
      ck_null
      | ck_pubkey_chain
   )
;

crypto_keyring
:
   KEYRING name = variable_permissive NEWLINE
   (
      ckr_local_address
      | ckr_psk
   )*
;

crypto_map
:
   MAP name = variable num = DEC?
   (
      crypto_map_ipsec_isakmp
      | crypto_map_null
   )
;

crypto_map_ii_match_address
:
   MATCH ADDRESS name = variable NEWLINE
;

crypto_map_ii_null
:
   NO?
   (
      DESCRIPTION
      | REVERSE_ROUTE
      | SET
   ) null_rest_of_line
;

crypto_map_ipsec_isakmp
:
   IPSEC_ISAKMP NEWLINE
   (
      crypto_map_ii_match_address
      | crypto_map_ii_null
   )*
;

crypto_map_null
:
   (
      INTERFACE
      |
      (
         IPSEC_ISAKMP DYNAMIC
      )
      | MATCH
      | REDUNDANCY
      | SET
   ) null_rest_of_line
;

crypto_pki
:
   PKI
   (
      cpki_certificate_chain
      | cpki_null
      | cpki_server
      | cpki_trustpoint
   )
;

dh_group
:
   GROUP1
   | GROUP2
;

key_key
:
   KEY name = variable NEWLINE
   (
      kk_null
   )*
;

kk_null
:
   NO?
   (
      ACCEPT_LIFETIME
      | CRYPTOGRAPHIC_ALGORITHM
      | KEY_STRING
      | SEND_LIFETIME
   ) null_rest_of_line
;

ike_encryption
:
   (
      AES
      | THREE_DES
   )
;

ike_encryption_aruba
:
   (
      AES128
      | AES192
      | AES256
   )
;

ipsec_authentication
:
   (
      ESP_MD5_HMAC
      | ESP_SHA_HMAC
      | ESP_SHA256_HMAC
   )
;

ipsec_encryption
:
   (
      ESP_AES (strength = DEC)?
      | ESP_DES
      | ESP_3DES
      | ESP_GCM
      | ESP_GMAC
      | ESP_NULL
      | ESP_SEAL
   )
;

s_crypto
:
   NO? CRYPTO
   (
      crypto_ca
      | crypto_dynamic_map
      | crypto_engine
      | crypto_ikev1
      | crypto_ikev2
      | crypto_ipsec
      | crypto_isakmp
      | crypto_key
      | crypto_keyring
      | crypto_map
      | crypto_pki
   )
;

s_key
:
   KEY CHAIN name = variable_permissive NEWLINE
   (
      key_key
   )*
;
