package org.batfish.representation.cisco;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.vendor.StructureType;
import org.batfish.vendor.StructureUsage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CiscoConfigurationTest {
  private CiscoConfiguration _config;
  private Interface _interface;
  private static final String ACL = "acl";
  private static final String POOL = "pool";
  private static final Ip IP = new Ip("1.2.3.4");

  // Helper to assert that the given structure is defined. Use with caution: arguments matter as
  // we're simply asserting that the values do not appear as unused structures.
  private void assertDefined(StructureType type, String name, StructureUsage usage) {
    SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>> hostRefs =
        _config.getAnswerElement().getUndefinedReferences().get(_config.getHostname());
    if (hostRefs == null) {
      // Nothing undefined for host.
      return;
    }

    SortedMap<String, SortedMap<String, SortedSet<Integer>>> typeRefs =
        hostRefs.get(type.getDescription());
    if (typeRefs == null) {
      // Nothing undefined for this type.
      return;
    }

    SortedMap<String, SortedSet<Integer>> usageRefs = typeRefs.get(name);
    if (usage == null) {
      // Nothing undefined for this usage.
      return;
    }

    SortedSet<Integer> linesUsed = usageRefs.get(usage.getDescription());
    if (linesUsed == null) {
      // No lines used for this usage.
      return;
    }

    fail("Expected reference to be defined, but it appears in the undefined list");
  }

  // Helper to assert that the given structure is undefined.
  private void assertUndefined(StructureType type, String name, StructureUsage usage) {
    assertThat(
        "no undefined refs for host",
        _config.getAnswerElement().getUndefinedReferences(),
        hasKey(_config.getHostname()));
    SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>> hostRefs =
        _config.getAnswerElement().getUndefinedReferences().get(_config.getHostname());

    assertThat("no undefined refs for type", hostRefs, hasKey(type.getDescription()));
    SortedMap<String, SortedMap<String, SortedSet<Integer>>> typeRefs =
        hostRefs.get(type.getDescription());

    assertThat("no undefined refs for name", typeRefs, hasKey(name));
    SortedMap<String, SortedSet<Integer>> usageRefs = typeRefs.get(name);

    assertThat("no lines used for usage", usageRefs, hasKey(usage.getDescription()));
    SortedSet<Integer> linesUsed = usageRefs.get(usage.getDescription());

    assertThat(linesUsed, not(empty()));
  }

  // Initializes an empty CiscoConfiguration with a single Interface and minimal settings to not
  // crash.
  @Before
  public void before() {
    _config = new CiscoConfiguration(Collections.emptySet());
    _config.setVendor(ConfigurationFormat.ARISTA);
    _config.setHostname("host");
    _config.setAnswerElement(new ConvertConfigurationAnswerElement());
    _interface = new Interface("iface", _config);
  }

  @Test
  public void processSourceNatIsConverted() {
    CiscoSourceNat nat = new CiscoSourceNat();
    nat.setAclName(ACL);
    nat.setNatPool(POOL);
    NatPool pool = new NatPool(POOL);
    pool.setFirst(IP);
    pool.setLast(IP);
    _config.getNatPools().put(POOL, pool);

    SourceNat convertedNat =
        _config.processSourceNat(
            nat,
            _interface,
            Collections.singletonMap(ACL, new IpAccessList(ACL, Collections.emptyList())));

    assertThat(convertedNat, notNullValue());
    assertThat(convertedNat.getAcl().getName(), equalTo(ACL));
    assertThat(convertedNat.getPoolIpFirst(), equalTo(IP));
    assertThat(_config.getAnswerElement().getUndefinedReferences().size(), equalTo(0));
  }
}
