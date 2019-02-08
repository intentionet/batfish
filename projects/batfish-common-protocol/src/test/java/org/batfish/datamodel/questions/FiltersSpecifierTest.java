package org.batfish.datamodel.questions;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.regex.Pattern;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.questions.FiltersSpecifier.Type;
import org.junit.Test;

public class FiltersSpecifierTest {

  @Test
  public void constructorImplicitName() {
    FiltersSpecifier specifier = new FiltersSpecifier("acl.*");
    assertThat(specifier.getType(), equalTo(Type.NAME));
    assertThat(specifier.getRegex().pattern(), equalTo(Pattern.compile("acl.*").pattern()));
  }

  @Test
  public void constructorExplicitName() {
    FiltersSpecifier specifier = new FiltersSpecifier("name:acl.*");
    assertThat(specifier.getType(), equalTo(Type.NAME));
    assertThat(specifier.getRegex().pattern(), equalTo(Pattern.compile("acl.*").pattern()));
  }

  @Test
  public void constructorIpv4() {
    FiltersSpecifier specifier = new FiltersSpecifier("ipv4:secret.*");
    assertThat(specifier.getType(), equalTo(Type.IPV4));
    assertThat(specifier.getRegex().pattern(), equalTo(Pattern.compile("secret.*").pattern()));
  }

  @Test
  public void matchesName() {
    FiltersSpecifier specifier = new FiltersSpecifier("name:acl.*");

    IpAccessList matchingList = IpAccessList.builder().setName("acl-99").build();
    IpAccessList nonMatchingList = IpAccessList.builder().setName("nana").build();

    assertThat(specifier.matches(matchingList, null), equalTo(true));
    assertThat(specifier.matches(nonMatchingList, null), equalTo(false));
  }

  @Test
  public void matchesIpv4() {
    FiltersSpecifier specifier = new FiltersSpecifier("ipv4:acl.*");

    IpAccessList matchingList = IpAccessList.builder().setName("acl-99").build();
    Ip6AccessList nonMatchingList = new Ip6AccessList("acl-99");

    assertThat(specifier.matches(matchingList, null), equalTo(true));
    assertThat(specifier.matches(nonMatchingList), equalTo(false));
  }

  @Test
  public void matchesFilterOn() {
    String inAclName = "inAcl";
    String outAclName = "outAcl";
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf v = nf.vrfBuilder().setOwner(c).build();
    IpAccessList inAcl = IpAccessList.builder().setName(inAclName).setOwner(c).build();
    IpAccessList outAcl = IpAccessList.builder().setName(outAclName).setOwner(c).build();
    Interface i =
        nf.interfaceBuilder()
            .setIncomingFilter(inAcl)
            .setOutgoingFilter(outAcl)
            .setOwner(c)
            .setVrf(v)
            .build();
    String iName = i.getName();
    FiltersSpecifier inputFilterSpecifier =
        new FiltersSpecifier(String.format("%s:%s", Type.INPUTFILTERON.name(), iName));
    FiltersSpecifier outputFilterSpecifier =
        new FiltersSpecifier(String.format("%s:%s", Type.OUTPUTFILTERON.name(), iName));

    assertThat(inputFilterSpecifier.matches(inAcl, c), equalTo(true));
    assertThat(inputFilterSpecifier.matches(outAcl, c), equalTo(false));

    assertThat(outputFilterSpecifier.matches(outAcl, c), equalTo(true));
    assertThat(outputFilterSpecifier.matches(inAcl, c), equalTo(false));
  }

  @Test
  public void defaultWithColons() {
    String expression = "foo::bar::baz";
    FiltersSpecifier specifier = new FiltersSpecifier(expression);
    assertThat(specifier.getType(), is(Type.NAME));
    assertThat(specifier.getRegex().pattern(), equalTo(expression));
  }

  @Test
  public void nameWithColons() {
    String expression = "foo::bar::baz";
    FiltersSpecifier specifier = new FiltersSpecifier("name:" + expression);
    assertThat(specifier.getType(), is(Type.NAME));
    assertThat(specifier.getRegex().pattern(), equalTo(expression));
  }
}
