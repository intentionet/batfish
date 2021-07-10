package org.batfish.grammar;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.batfish.datamodel.ConfigurationFormat;

public final class VendorConfigurationFormatDetector {
  private static final Logger LOGGER =
      LogManager.getLogger(VendorConfigurationFormatDetector.class);

  public static final String BATFISH_FLATTENED_JUNIPER_HEADER =
      "####BATFISH FLATTENED JUNIPER CONFIG####\n";

  public static final String BATFISH_FLATTENED_PALO_ALTO_HEADER =
      "####BATFISH FLATTENED PALO ALTO CONFIG####\n";

  public static final String BATFISH_FLATTENED_VYOS_HEADER =
      "####BATFISH FLATTENED VYOS CONFIG####\n";

  public static ConfigurationFormat identifyConfigurationFormat(String fileText) {
    return new VendorConfigurationFormatDetector(fileText).identifyConfigurationFormat();
  }

  private static final Pattern BATFISH_CONFIG_FORMAT_PATTERN =
      Pattern.compile("(?m)^[!#] *BATFISH[-_]FORMAT *: *([a-zA-Z0-9_-]+)");

  private static final Pattern BANNER_PATTERN = Pattern.compile("(?m)^banner ");
  private static final Pattern ALCATEL_AOS_PATTERN = Pattern.compile("(?m)^system name");
  private static final Pattern ARUBAOS_PATTERN = Pattern.compile("(?m)^netservice.*$");
  private static final Pattern BLADE_NETWORK_PATTERN = Pattern.compile("(?m)^switch-type");
  private static final Pattern CADANT_NETWORK_PATTERN = Pattern.compile("(?m)^shelfname");
  private static final Pattern CHECK_POINT_GATEWAY_PATTERN =
      Pattern.compile("(?m)^# Configuration of \\w+\n# Language version: ");
  private static final Pattern CUMULUS_CONCATENATED_PATTERN =
      Pattern.compile("(?m)^# This file describes the network interfaces");
  private static final Pattern CUMULUS_NCLU_PATTERN = Pattern.compile("(?m)^net del all$");
  private static final Pattern F5_HOSTNAME_PATTERN = Pattern.compile("(?m)^tmsh .*$");
  private static final Pattern F5_BIGIP_STRUCTURED_HEADER_PATTERN =
      Pattern.compile("(?m)^#TMSH-VERSION: .*$");
  private static final Pattern F5_BIGIP_STRUCTURED_GLOBAL_SETTINGS_PATTERN =
      Pattern.compile("(?m)^sys\\s+global-settings\\s*\\{.*$");
  private static final Pattern METAMAKO_MOS_PATTERN =
      Pattern.compile("(?m)^! device: [^\\n]+ MOS-\\d+\\.\\d+\\.\\d+\\)$");
  private static final Pattern MRV_HOSTNAME_PATTERN =
      Pattern.compile("(?m)^configuration hostname .*$");
  private static final Pattern MSS_PATTERN = Pattern.compile("(?m)^set system name");

  private static final Pattern RANCID_BASE_PATTERN =
      Pattern.compile("(?m)^[!#]RANCID-CONTENT-TYPE: ([a-zA-Z0-9_-]+)");

  // checkCisco patterns
  private static final Pattern ASA_VERSION_LINE_PATTERN = Pattern.compile("(?m)(^ASA Version.*$)");
  private static final Pattern CISCO_LIKE_PATTERN =
      Pattern.compile("(?m)(^boot system flash.*$)|(^interface .*$)");
  private static final Pattern CISCO_STYLE_ACL_PATTERN =
      Pattern.compile("(?m)(^(ip )?access-list.*$)");
  private static final Pattern NEXUS_COMMIT_LINE_PATTERN = Pattern.compile("(?m)^ *commit *$");
  private static final Pattern NEXUS_FEATURE_LINE_PATTERN =
      Pattern.compile("(?m)^\\s*(no\\s*)?feature\\s+[^\\s+].*$");
  private static final Pattern NEXUS_BOOT_NXOS_PATTERN = Pattern.compile("boot nxos");
  private static final Pattern NEXUS_BOOTFLASH_PATTERN =
      Pattern.compile("bootflash:(n\\d+|/?nxos)");

  // checkJuniper patterns
  private static final Pattern FLAT_JUNIPER_HOSTNAME_DECLARATION_PATTERN =
      Pattern.compile("(?m)^set (groups [^ ][^ ]* )?system host-name ");
  private static final Pattern FLATTENED_JUNIPER_PATTERN =
      Pattern.compile(Pattern.quote(BATFISH_FLATTENED_JUNIPER_HEADER));
  private static final Pattern JUNIPER_ACL_PATTERN = Pattern.compile("(?m)^firewall *\\{");
  private static final Pattern JUNIPER_POLICY_OPTIONS_PATTERN =
      Pattern.compile("(?m)^policy-options *\\{");
  private static final Pattern JUNIPER_SNMP_PATTERN = Pattern.compile("(?m)^snmp *\\{");
  private static final Pattern SET_PATTERN = Pattern.compile("(?m)^set ");

  // checkPaloAlto patterns
  private static final Pattern FLAT_PALO_ALTO_PATTERN =
      Pattern.compile(Pattern.quote(BATFISH_FLATTENED_PALO_ALTO_HEADER));
  private static final Pattern PALO_ALTO_DEVICECONFIG_PATTERN = Pattern.compile("(?m)deviceconfig");
  private static final Pattern PALO_ALTO_PANORAMA_PATTERN =
      Pattern.compile("(?m)(send-to-panorama|panorama-server)");

  private String _fileText;

  private char _firstChar;

  private boolean _notJuniper;

  private VendorConfigurationFormatDetector(String fileText) {
    _fileText = fileText;
  }

  private boolean fileTextMatches(Pattern pattern) {
    return pattern.matcher(_fileText).find();
  }

  private void configureHeuristicBlacklist() {
    if (fileTextMatches(BANNER_PATTERN)) {
      _notJuniper = true;
    }
  }

  @Nullable
  private ConfigurationFormat checkAlcatelAos() {
    if (fileTextMatches(ALCATEL_AOS_PATTERN)) {
      return ConfigurationFormat.ALCATEL_AOS;
    }
    return null;
  }

  private static final Pattern ARISTA_EOS_PATTERN =
      Pattern.compile("(?m)^! device: .*\\(.*EOS-\\d.*");
  private static final Pattern ARISTA_FLASH_PATTERN =
      Pattern.compile("(?m)^.*boot system flash.*\\.swi");

  @Nullable
  private ConfigurationFormat checkArista() {
    if (fileTextMatches(ARISTA_FLASH_PATTERN)) {
      return ConfigurationFormat.ARISTA;
    } else if (fileTextMatches(ARISTA_EOS_PATTERN)) {
      return ConfigurationFormat.ARISTA;
    }

    return null;
  }

  @Nullable
  private ConfigurationFormat checkArubaOS() {
    if (fileTextMatches(ARUBAOS_PATTERN)) {
      return ConfigurationFormat.ARUBAOS;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkBlade() {
    if (fileTextMatches(BLADE_NETWORK_PATTERN)) {
      return ConfigurationFormat.BLADENETWORK;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkCadant() {
    if (fileTextMatches(CADANT_NETWORK_PATTERN)) {
      return ConfigurationFormat.CADANT;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkCheckPoint() {
    if (fileTextMatches(CHECK_POINT_GATEWAY_PATTERN)) {
      return ConfigurationFormat.CHECK_POINT_GATEWAY;
    }
    return null;
  }

  /** Assuming Cisco device, try to find things that indicate IOS-XR. */
  private static final Pattern XR_QUALIFIERS =
      Pattern.compile(
          "(?m)^\\s*(interface Bundle-Ether|end-policy\\b|end-set\\b|ipv4 access-list\\b)");

  @Nullable
  private ConfigurationFormat checkCisco() {
    if (fileTextMatches(ASA_VERSION_LINE_PATTERN)) {
      return ConfigurationFormat.CISCO_ASA;
    } else if (fileTextMatches(XR_QUALIFIERS)) {
      return ConfigurationFormat.CISCO_IOS_XR;
    }
    if (fileTextMatches(NEXUS_BOOT_NXOS_PATTERN)
        || fileTextMatches(NEXUS_FEATURE_LINE_PATTERN)
        || fileTextMatches(NEXUS_BOOTFLASH_PATTERN)) {
      return ConfigurationFormat.CISCO_NX;
    }
    if (fileTextMatches(CISCO_LIKE_PATTERN)
        || _firstChar == '!'
        || fileTextMatches(CISCO_STYLE_ACL_PATTERN)) {
      return ConfigurationFormat.CISCO_IOS;
    } else if (fileTextMatches(NEXUS_COMMIT_LINE_PATTERN)) {
      return ConfigurationFormat.CISCO_NX;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkCiscoXr() {
    if (_fileText.contains("IOS XR")) {
      return ConfigurationFormat.CISCO_IOS_XR;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkCumulusConcatenated() {
    if (fileTextMatches(CUMULUS_CONCATENATED_PATTERN)) {
      return ConfigurationFormat.CUMULUS_CONCATENATED;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkCumulusNclu() {
    if (fileTextMatches(CUMULUS_NCLU_PATTERN)) {
      return ConfigurationFormat.CUMULUS_NCLU;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkEmpty() {
    String trimmedText = _fileText.trim();
    if (trimmedText.length() == 0) {
      return ConfigurationFormat.EMPTY;
    }
    _firstChar = trimmedText.charAt(0);
    return null;
  }

  @Nullable
  private ConfigurationFormat checkF5() {
    if (fileTextMatches(F5_BIGIP_STRUCTURED_HEADER_PATTERN)
        && fileTextMatches(F5_BIGIP_STRUCTURED_GLOBAL_SETTINGS_PATTERN)) {
      return ConfigurationFormat.F5_BIGIP_STRUCTURED;
    }
    if (fileTextMatches(F5_HOSTNAME_PATTERN)) {
      return ConfigurationFormat.F5;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkFlatVyos() {
    if (_fileText.contains("set system config-management commit-revisions")) {
      return ConfigurationFormat.FLAT_VYOS;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkIpTables() {
    if (_fileText.contains("INPUT")
        && _fileText.contains("OUTPUT")
        && _fileText.contains("FORWARD")) {
      return ConfigurationFormat.IPTABLES;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkJuniper(boolean preMatch) {
    if (_notJuniper) {
      return null;
    } else if (FLATTENED_JUNIPER_PATTERN.matcher(_fileText).find(0)) {
      return ConfigurationFormat.FLAT_JUNIPER;
    } else if (_fileText.contains("set hostname")) {
      return ConfigurationFormat.JUNIPER_SWITCH;
    }

    // Decide whether we believe this is a Juniper file.
    boolean isJuniper =
        preMatch
            || FLAT_JUNIPER_HOSTNAME_DECLARATION_PATTERN.matcher(_fileText).find(0)
            || (_fileText.contains("apply-groups") && SET_PATTERN.matcher(_fileText).find(0))
            || fileTextMatches(JUNIPER_ACL_PATTERN)
            || fileTextMatches(JUNIPER_POLICY_OPTIONS_PATTERN)
            || fileTextMatches(JUNIPER_SNMP_PATTERN)
            || _fileText.contains("system")
                && _fileText.contains("{")
                && _fileText.contains("}")
                && _fileText.contains("host-name")
                && _fileText.contains("interfaces");
    if (isJuniper) {
      return (_fileText.contains("{"))
          ? ConfigurationFormat.JUNIPER
          : ConfigurationFormat.FLAT_JUNIPER;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkMetamako() {
    if (_fileText.contains("application metamux")
        || _fileText.contains("application metawatch")
        || fileTextMatches(METAMAKO_MOS_PATTERN)) {
      return ConfigurationFormat.METAMAKO;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkMrv() {
    if (_fileText.contains("System.SystemName")) {
      return ConfigurationFormat.MRV;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkMrvCommands() {
    if (fileTextMatches(MRV_HOSTNAME_PATTERN)) {
      return ConfigurationFormat.MRV_COMMANDS;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkMss() {
    if (fileTextMatches(MSS_PATTERN)) {
      return ConfigurationFormat.MSS;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkPaloAlto(boolean preMatch) {
    if (fileTextMatches(FLAT_PALO_ALTO_PATTERN)) {
      return ConfigurationFormat.PALO_ALTO;
    } else if (preMatch
        || fileTextMatches(PALO_ALTO_DEVICECONFIG_PATTERN)
        || fileTextMatches(PALO_ALTO_PANORAMA_PATTERN)) {
      if (_fileText.contains("{")) {
        return ConfigurationFormat.PALO_ALTO_NESTED;
      }
      return ConfigurationFormat.PALO_ALTO;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkBatfish() {
    Matcher m = BATFISH_CONFIG_FORMAT_PATTERN.matcher(_fileText);
    if (!m.find()) {
      return null;
    }
    String format = m.group(1);
    try {
      return ConfigurationFormat.valueOf(format.toUpperCase());
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Unknown Batfish configuration format: {}", format);
      // This is not a known enum value, force unknown.
      return ConfigurationFormat.UNKNOWN;
    }
  }

  @Nullable
  private ConfigurationFormat checkRancid() {
    Matcher m = RANCID_BASE_PATTERN.matcher(_fileText);
    if (!m.find()) {
      return null;
    }
    switch (m.group(1)) {
      case "arista":
        return ConfigurationFormat.ARISTA;
      case "bigip":
        return ConfigurationFormat.F5_BIGIP_STRUCTURED;
      case "cisco":
        // unfortunately, old RANCID cannot distinguish subtypes and also often does other vendors
        // like Arista
        return null;
      case "cisco-nx":
        return ConfigurationFormat.CISCO_NX;
      case "cisco-xr":
        return ConfigurationFormat.CISCO_IOS_XR;
      case "force10":
        return ConfigurationFormat.FORCE10;
      case "foundry":
        return ConfigurationFormat.FOUNDRY;
      case "ibmbnt":
        return ConfigurationFormat.IBM_BNT;
      case "juniper":
      case "juniper-srx":
        return checkJuniper(true);
      case "mrv":
        return ConfigurationFormat.MRV;
      case "paloalto":
        return checkPaloAlto(true);
      default:
        // We don't recognize the RANCID string, assert this config is unknown.
        return ConfigurationFormat.UNKNOWN;
    }
  }

  private static final Pattern RUCKUS_ICX_MODULE_PATTERN =
      Pattern.compile("module \\d+ icx", Pattern.MULTILINE);

  @Nullable
  private ConfigurationFormat checkRuckusIcx() {
    if (RUCKUS_ICX_MODULE_PATTERN.matcher(_fileText).find()) {
      return ConfigurationFormat.RUCKUS_ICX;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkVxWorks() {
    if (_firstChar == '!' && _fileText.contains("set prompt")) {
      return ConfigurationFormat.VXWORKS;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkVyos() {
    if (_fileText.contains("system")
        && _fileText.contains("{")
        && _fileText.contains("}")
        && _fileText.contains("config-management")
        && _fileText.contains("commit-revisions")) {
      return ConfigurationFormat.VYOS;
    }
    return null;
  }

  @Nullable
  private ConfigurationFormat checkFortios() {
    if (_fileText.contains("config system global")) {
      return ConfigurationFormat.FORTIOS;
    }
    return null;
  }

  private ConfigurationFormat identifyConfigurationFormat() {
    ConfigurationFormat format;
    format = checkEmpty();
    if (format != null) {
      return format;
    }
    format = checkBatfish();
    if (format != null) {
      return format;
    }
    format = checkRancid();
    if (format != null) {
      return format;
    }

    // Heuristics are somewhat brittle. This function adds information about which configuration
    // formats we know this file does not match.
    configureHeuristicBlacklist();

    return firstNonNull(
        checkCheckPoint(),
        checkFortios(),
        checkRuckusIcx(),
        checkCadant(),
        checkCumulusConcatenated(),
        checkCumulusNclu(),
        checkF5(),
        checkCiscoXr(),
        checkFlatVyos(),
        checkIpTables(),
        checkMetamako(),
        checkMrv(),
        checkMrvCommands(),
        checkPaloAlto(false),
        checkVyos(),
        checkArista(),
        checkBlade(),
        checkVxWorks(),
        checkJuniper(false),
        checkAlcatelAos(),
        checkMss(),
        checkArubaOS(),
        checkCisco(),
        ConfigurationFormat.UNKNOWN);
  }
}
