package org.batfish.common;

/** Constants used in the coordinator service version 2. */
public class CoordConstsV2 {
  ////// HTTP Headers that clients are expected to configure. //////

  /** The HTTP Header containing the client's API Key. */
  public static String HTTP_HEADER_BATFISH_APIKEY = "X-Batfish-Apikey";
  /** The HTTP Header containing the client's version. */
  public static String HTTP_HEADER_BATFISH_VERSION = "X-Batfish-Version";
}
