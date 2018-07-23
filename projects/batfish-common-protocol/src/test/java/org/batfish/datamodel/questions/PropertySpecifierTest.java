package org.batfish.datamodel.questions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.answers.AutocompleteSuggestion;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.PropertySpecifier.PropertyDescriptor;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PropertySpecifierTest {
  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void baseAutoComplete() {
    Set<String> properties = ImmutableSet.of("abc", "ntp-servers", "ntp-source-interface");

    // null or empty string should yield all options, with .* as the first one
    assertThat(
        PropertySpecifier.baseAutoComplete(null, properties)
            .stream()
            .map(s -> s.getText())
            .collect(Collectors.toList()),
        equalTo(ImmutableList.builder().add(".*").addAll(properties).build()));

    // the capital P shouldn't matter and this should autoComplete to three entries
    assertThat(
        new ArrayList<>(PropertySpecifier.baseAutoComplete("ntP", properties)),
        equalTo(
            ImmutableList.of(
                new AutocompleteSuggestion(".*ntp.*", false),
                new AutocompleteSuggestion("ntp-servers", false),
                new AutocompleteSuggestion("ntp-source-interface", false))));
  }

  @Test
  public void convertTypeIfNeeded() {
    // map should be converted to set of strings when the schema is string list
    Object propertyValueMap =
        PropertySpecifier.convertTypeIfNeeded(
            ImmutableSortedMap.of("k1", "v1", "k2", "v2"),
            new PropertyDescriptor<Configuration>(null, Schema.list(Schema.STRING)));
    assertThat(propertyValueMap, equalTo(ImmutableSet.of("k1", "k2")));

    // other objects should be mapped to their string forms
    Object propertyValueObject =
        PropertySpecifier.convertTypeIfNeeded(
            new Configuration("cname", ConfigurationFormat.CISCO_IOS),
            new PropertyDescriptor<Configuration>(null, Schema.STRING));
    assertThat(propertyValueObject, equalTo("cname"));

    // null should remain null and not crash
    Object propertyValueNull =
        PropertySpecifier.convertTypeIfNeeded(
            null, new PropertyDescriptor<Configuration>(null, Schema.STRING));
    assertThat(propertyValueNull, equalTo(null));

    // protect other objects
    Object propertyValueOther =
        PropertySpecifier.convertTypeIfNeeded(
            new Node("node"), new PropertyDescriptor<Configuration>(null, Schema.NODE));
    assertThat(propertyValueOther, equalTo(new Node("node")));
  }

  @Test
  public void fillPropertyFail() {
    PropertyDescriptor<Configuration> propDescriptor =
        new PropertyDescriptor<>(null, Schema.list(Schema.STRING));

    _thrown.expect(ClassCastException.class);
    _thrown.expectMessage("Cannot recover object");

    PropertySpecifier.fillProperty("col", "stringNotList", Row.builder(), propDescriptor);
  }

  @Test
  public void fillPropertyForcedString() {
    Configuration configuration = new Configuration("hostname", ConfigurationFormat.CISCO_IOS);
    configuration.setDefaultInboundAction(LineAction.ACCEPT);
    String property = "default-inbound-action";
    PropertyDescriptor<Configuration> propertyDescriptor =
        NodePropertySpecifier.JAVA_MAP.get(property);
    RowBuilder row = Row.builder();

    PropertySpecifier.fillProperty(propertyDescriptor, configuration, property, row);

    // the row should be filled out with the String value
    assertThat(
        row.build(), equalTo(Row.builder().put(property, LineAction.ACCEPT.toString()).build()));
  }

  @Test
  public void fillPropertyListEmpty() {
    Configuration configuration = new Configuration("hostname", ConfigurationFormat.CISCO_IOS);
    String property = "ntp-servers";
    PropertyDescriptor<Configuration> propertyDescriptor =
        NodePropertySpecifier.JAVA_MAP.get(property);
    RowBuilder row = Row.builder();

    PropertySpecifier.fillProperty(propertyDescriptor, configuration, property, row);

    // the row should be filled out with an empty list
    assertThat(row.build(), equalTo(Row.builder().put(property, new LinkedList<String>()).build()));
  }

  @Test
  public void fillPropertyListNonEmpty() {
    Configuration configuration = new Configuration("hostname", ConfigurationFormat.CISCO_IOS);
    configuration.setNtpServers(ImmutableSortedSet.of("sa", "sb"));
    String property = "ntp-servers";
    PropertyDescriptor<Configuration> propertyDescriptor =
        NodePropertySpecifier.JAVA_MAP.get(property);
    RowBuilder row = Row.builder();

    PropertySpecifier.fillProperty(propertyDescriptor, configuration, property, row);

    // the row should be filled out with the right list and the schemas map should be List<String>
    assertThat(
        row.build(), equalTo(Row.builder().put(property, ImmutableList.of("sa", "sb")).build()));
  }

  @Test
  public void fillPropertyMap() {
    Configuration configuration = new Configuration("hostname", ConfigurationFormat.CISCO_IOS);
    configuration.setInterfaces(ImmutableSortedMap.of("i1", new Interface("i1")));
    String property = "interfaces";
    PropertyDescriptor<Configuration> propertyDescriptor =
        NodePropertySpecifier.JAVA_MAP.get(property);
    RowBuilder row = Row.builder();

    PropertySpecifier.fillProperty(propertyDescriptor, configuration, property, row);

    // the row should be filled out with the right list and the schemas map should be List<String>
    assertThat(row.build(), equalTo(Row.builder().put(property, ImmutableList.of("i1")).build()));
  }

  @Test
  public void fillPropertyNull() {
    Configuration configuration = new Configuration("hostname", ConfigurationFormat.CISCO_IOS);
    String property = "ntp-source-interface";
    PropertyDescriptor<Configuration> propertyDescriptor =
        NodePropertySpecifier.JAVA_MAP.get(property);
    RowBuilder row = Row.builder();

    PropertySpecifier.fillProperty(propertyDescriptor, configuration, property, row);

    // the row should be filled out with null and the schemas shouldn't be
    assertThat(row.build(), equalTo(Row.builder().put(property, null).build()));
  }
}
