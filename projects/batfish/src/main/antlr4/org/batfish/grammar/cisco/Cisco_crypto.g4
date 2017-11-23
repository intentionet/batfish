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
   LOOKUP ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
;

cctpool_null
:
   NO?
   (
      AUTO_IMPORT
   ) ~NEWLINE* NEWLINE
;

cd_null
:
   NO?
   (
      VERSION
   ) ~NEWLINE* NEWLINE
;

cd_set
:
   SET ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
;

ci2prf_null
:
   NO?
   (
      AUTHENTICATION
      | KEYRING
      | MATCH
   ) ~NEWLINE* NEWLINE
;

ci2prp_null
:
   NO?
   (
      ENCRYPTION
      | GROUP
      | INTEGRITY
   ) ~NEWLINE* NEWLINE
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
      | SECURITY_ASSOCIATION
   ) ~NEWLINE* NEWLINE
;

cip_profile
:
   PROFILE name = variable_permissive NEWLINE
   (
      cipprf_null
   )*
;

cip_transform_set
:
   TRANSFORM_SET ~NEWLINE* NEWLINE
   (
      cipt_null
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
   ) ~NEWLINE* NEWLINE
;

cipprf_null
:
   NO?
   (
      SET
   ) ~NEWLINE* NEWLINE
;

cipt_null
:
   NO?
   (
      MODE
   ) ~NEWLINE* NEWLINE
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
      | NAT_TRAVERSAL
   ) ~NEWLINE* NEWLINE
;

cis_policy
:
   POLICY name = variable NEWLINE
   (
      cis_proposal
   )*
;

cis_profile
:
   PROFILE name = variable NEWLINE
   (
      cisprf_null
   )*
;

cis_proposal
:
   (
      cispro_authentication
      | cispro_encr
      | cispro_group
      | cispro_hash
      | cispro_lifetime
      | cispro_null
   )
;

cispro_authentication
:
   AUTHENTICATION PRE_SHARE NEWLINE
;

cispro_encr
:
   ENCR
   (
      AES
      | THREE_DES
   ) NEWLINE
;

cispro_group
:
   GROUP DEC NEWLINE
;

cispro_hash
:
   GROUP MD5 NEWLINE
;

cispro_lifetime
:
    LIFETIME DEC NEWLINE
;

cispro_null
:
   NO?
   (
      | ENCRYPTION
      | HASH
      | PRF
      | VERSION
   ) ~NEWLINE* NEWLINE
;

cisprf_null
:
   NO?
   (
      KEYRING
      | MATCH
      | REVERSE_ROUTE
      | VRF
   ) ~NEWLINE* NEWLINE
;

ck_null
:
   (
      GENERATE
      | PARAM
   ) ~NEWLINE* NEWLINE
;

ck_pubkey_chain
:
   PUBKEY_CHAIN ~NEWLINE* NEWLINE ckp_named_key*
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
   ) ~NEWLINE* NEWLINE
;

ckr_null
:
   NO?
   (
      PRE_SHARED_KEY
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
   ENGINE ~NEWLINE* NEWLINE
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
      ckr_null
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
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
   ) ~NEWLINE* NEWLINE
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
