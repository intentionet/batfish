package org.batfish.datamodel;

/** Vendor configuration file format. */
public enum ConfigurationFormat {
  ALCATEL_AOS("alcatel_aos"),
  ARISTA("arista"),
  ARUBAOS("aruba"),
  AWS("aws"),
  BLADENETWORK("bladenetwork"),
  CADANT("cadant"),
  CISCO_ASA("cisco"),
  CISCO_IOS("cisco"),
  CISCO_IOS_XR("cisco"),
  CISCO_NX("cisco"),
  EMPTY("empty"),
  F5("f5"),
  F5_BIGIP_STRUCTURED("f5_bigip_structured"),
  FLAT_JUNIPER("juniper"),
  FLAT_VYOS("vyos"),
  FORCE10("force10"),
  FOUNDRY("foundry"),
  HOST("host"),
  IGNORED("ignored"),
  IPTABLES("iptables"),
  JUNIPER("juniper"),
  JUNIPER_SWITCH("juniper"),
  METAMAKO("metamako"),
  MRV("mrv"),
  MRV_COMMANDS("mrv_commands"),
  MSS("mss"),
  PALO_ALTO("paloalto"),
  PALO_ALTO_NESTED("paloalto"),
  UNKNOWN("unknown"),
  VXWORKS("vxworks"),
  VYOS("vyos");

  private String _vendorString;

  ConfigurationFormat(String vendorString) {
    _vendorString = vendorString;
  }

  public String getVendorString() {
    return _vendorString;
  }
}
