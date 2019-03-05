package org.batfish.main;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.BfConsts;
import org.batfish.common.util.CommonUtil;

public class TestrigText {

  public static class Builder {

    private static Map<String, String> readTestrigResources(
        String testrigResourcePrefix, @Nullable String subfolder, Iterable<String> filenames) {
      if (filenames != null) {
        List<String> filenameList = ImmutableList.copyOf(filenames);
        SortedSet<String> filenameSet = ImmutableSortedSet.copyOf(filenames);
        if (filenameList.size() != filenameSet.size()) {
          throw new BatfishException("Duplicate filenames provided in: " + filenameList);
        }
        String subfolderText = subfolder != null ? String.format("/%s", subfolder) : "";
        return filenameList.stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Function.identity(),
                    filename ->
                        CommonUtil.readResource(
                            String.format(
                                "%s%s/%s", testrigResourcePrefix, subfolderText, filename))));
      } else {
        return Collections.emptyMap();
      }
    }

    private Map<String, String> _awsText;

    private Map<String, String> _bgpTablesText;

    private Map<String, String> _configurationText;

    private Map<String, String> _hostsText;

    private Map<String, String> _iptablesFilesText;

    private String _layer1TopologyText;

    private Map<String, String> _routingTablesText;

    public TestrigText build() {
      TestrigText testrigText = new TestrigText();
      testrigText.setAwsText(_awsText);
      testrigText.setBgpTablesText(_bgpTablesText);
      testrigText.setConfigurationText(_configurationText);
      testrigText.setHostsText(_hostsText);
      testrigText.setIptablesFilesText(_iptablesFilesText);
      testrigText.setLayer1TopologyText(_layer1TopologyText);
      testrigText.setRoutingTablesText(_routingTablesText);
      return testrigText;
    }

    public Builder setAwsText(Map<String, String> awsText) {
      _awsText = awsText;
      return this;
    }

    public Builder setAwsText(String testrigResourcePrefix, Iterable<String> filenames) {
      _awsText =
          readTestrigResources(testrigResourcePrefix, BfConsts.RELPATH_AWS_CONFIGS_DIR, filenames);
      return this;
    }

    public Builder setBgpTablesText(Map<String, String> bgpTablesText) {
      _bgpTablesText = bgpTablesText;
      return this;
    }

    public Builder setBgpTablesText(String testrigResourcePrefix, Iterable<String> filenames) {
      _bgpTablesText =
          readTestrigResources(
              testrigResourcePrefix, BfConsts.RELPATH_ENVIRONMENT_BGP_TABLES, filenames);
      return this;
    }

    public Builder setConfigurationText(Map<String, String> configurationText) {
      _configurationText = configurationText;
      return this;
    }

    public Builder setConfigurationText(String testrigResourcePrefix, String... filenames) {
      return setConfigurationText(testrigResourcePrefix, Arrays.asList(filenames));
    }

    public Builder setConfigurationText(String testrigResourcePrefix, Iterable<String> filenames) {
      _configurationText =
          readTestrigResources(
              testrigResourcePrefix, BfConsts.RELPATH_CONFIGURATIONS_DIR, filenames);
      return this;
    }

    public Builder setHostsText(Map<String, String> hostsText) {
      _hostsText = hostsText;
      return this;
    }

    public Builder setHostsText(String testrigResourcePrefix, String... filenames) {
      return setHostsText(testrigResourcePrefix, Arrays.asList(filenames));
    }

    public Builder setHostsText(String testrigResourcePrefix, Iterable<String> filenames) {
      _hostsText =
          readTestrigResources(testrigResourcePrefix, BfConsts.RELPATH_HOST_CONFIGS_DIR, filenames);
      return this;
    }

    public Builder setIptablesFilesText(Map<String, String> iptablesFilesText) {
      _iptablesFilesText = iptablesFilesText;
      return this;
    }

    public Builder setIptablesFilesText(String testrigResourcePrefix, Iterable<String> filenames) {
      _iptablesFilesText = readTestrigResources(testrigResourcePrefix, "iptables", filenames);
      return this;
    }

    public @Nonnull Builder setLayer1TopologyText(@Nonnull String testrigResourcePrefix) {
      _layer1TopologyText =
          readTestrigResources(
                  testrigResourcePrefix, null, ImmutableList.of(BfConsts.RELPATH_L1_TOPOLOGY_PATH))
              .values()
              .iterator()
              .next();
      return this;
    }

    public Builder setRoutingTablesText(Map<String, String> routingTablesText) {
      _routingTablesText = routingTablesText;
      return this;
    }

    public Builder setRoutingTablesText(String testrigResourcePrefix, Iterable<String> filenames) {
      _routingTablesText =
          readTestrigResources(
              testrigResourcePrefix, BfConsts.RELPATH_ENVIRONMENT_ROUTING_TABLES, filenames);
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private Map<String, String> _awsText;

  private Map<String, String> _bgpTablesText;

  private Map<String, String> _configurationText;

  private Map<String, String> _hostsText;

  private Map<String, String> _iptablesFilesText;

  private String _layer1TopologyText;

  private Map<String, String> _routingTablesText;

  public Map<String, String> getAwsText() {
    return _awsText;
  }

  public Map<String, String> getBgpTablesText() {
    return _bgpTablesText;
  }

  public Map<String, String> getConfigurationText() {
    return _configurationText;
  }

  public Map<String, String> getHostsText() {
    return _hostsText;
  }

  public Map<String, String> getIptablesFilesText() {
    return _iptablesFilesText;
  }

  public @Nullable String getLayer1TopologyText() {
    return _layer1TopologyText;
  }

  public Map<String, String> getRoutingTablesText() {
    return _routingTablesText;
  }

  public void setAwsText(Map<String, String> awsText) {
    _awsText = awsText;
  }

  public void setBgpTablesText(Map<String, String> bgpTablesText) {
    _bgpTablesText = bgpTablesText;
  }

  public void setConfigurationText(Map<String, String> configurationText) {
    _configurationText = configurationText;
  }

  public void setHostsText(Map<String, String> hostsText) {
    _hostsText = hostsText;
  }

  public void setIptablesFilesText(Map<String, String> iptablesFilesText) {
    _iptablesFilesText = iptablesFilesText;
  }

  public void setLayer1TopologyText(@Nullable String layer1TopologyText) {
    _layer1TopologyText = layer1TopologyText;
  }

  public void setRoutingTablesText(Map<String, String> routingTablesText) {
    _routingTablesText = routingTablesText;
  }
}
