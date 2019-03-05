package org.batfish.question.interfaceproperties;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.PropertySpecifier;
import org.batfish.datamodel.questions.PropertySpecifier.PropertyDescriptor;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.InterfaceSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierContext;

public class InterfacePropertiesAnswerer extends Answerer {

  public static final String COL_INTERFACE = "Interface";

  public InterfacePropertiesAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  /**
   * Creates {@link ColumnMetadata}s that the answer should have based on the {@code
   * propertySpecifier}.
   *
   * @param propertySpecifier The {@link InterfacePropertySpecifier} that describes the set of
   *     properties
   * @return The {@link List} of {@link ColumnMetadata}s
   */
  public static List<ColumnMetadata> createColumnMetadata(
      InterfacePropertySpecifier propertySpecifier) {
    return ImmutableList.<ColumnMetadata>builder()
        .add(new ColumnMetadata(COL_INTERFACE, Schema.INTERFACE, "Interface", true, false))
        .addAll(
            propertySpecifier.getMatchingProperties().stream()
                .map(
                    prop ->
                        new ColumnMetadata(
                            getColumnName(prop),
                            InterfacePropertySpecifier.JAVA_MAP.get(prop).getSchema(),
                            "Property " + prop,
                            false,
                            true))
                .collect(Collectors.toList()))
        .build();
  }

  /**
   * Creates a {@link TableMetadata} object from the question.
   *
   * @param question The question
   * @return The resulting {@link TableMetadata} object
   */
  private static TableMetadata createTableMetadata(InterfacePropertiesQuestion question) {
    String textDesc = String.format("Properties of interface ${%s}.", COL_INTERFACE);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(createColumnMetadata(question.getProperties()), textDesc);
  }

  @Override
  public TableAnswerElement answer() {
    InterfacePropertiesQuestion question = (InterfacePropertiesQuestion) _question;

    TableMetadata tableMetadata = createTableMetadata(question);
    TableAnswerElement answer = new TableAnswerElement(tableMetadata);

    Multiset<Row> propertyRows =
        getProperties(
            question.getProperties(),
            _batfish.specifierContext(),
            question.getNodeSpecifier(),
            question.getInterfaceSpecifier(),
            question.getOnlyActive(),
            tableMetadata.toColumnMap());

    answer.postProcessAnswer(question, propertyRows);
    return answer;
  }

  /** Returns the name of the column that contains the value of property {@code property} */
  public static String getColumnName(String property) {
    return property;
  }

  public static Multiset<Row> getProperties(
      InterfacePropertySpecifier propertySpecifier,
      SpecifierContext ctxt,
      NodeSpecifier nodeSpecifier,
      InterfaceSpecifier interfaceSpecifier,
      Map<String, ColumnMetadata> columns) {
    return getProperties(
        propertySpecifier,
        ctxt,
        nodeSpecifier,
        interfaceSpecifier,
        InterfacePropertiesQuestion.DEFAULT_EXCLUDE_SHUT_INTERFACES,
        columns);
  }

  /**
   * Gets properties of interfaces.
   *
   * @param propertySpecifier Specifies which properties to get
   * @param ctxt Specifier context
   * @param nodeSpecifier Specifiers which nodes to consider
   * @param interfaceSpecifier Specifies which interfaces to consider
   * @param columns a map from column name to {@link ColumnMetadata}
   * @return A multiset of {@link Row}s where each row corresponds to a node and columns correspond
   *     to property values.
   */
  public static Multiset<Row> getProperties(
      InterfacePropertySpecifier propertySpecifier,
      SpecifierContext ctxt,
      NodeSpecifier nodeSpecifier,
      InterfaceSpecifier interfaceSpecifier,
      boolean excludeShutInterfaces,
      Map<String, ColumnMetadata> columns) {
    Multiset<Row> rows = HashMultiset.create();

    for (String nodeName : nodeSpecifier.resolve(ctxt)) {
      for (Interface iface : interfaceSpecifier.resolve(ImmutableSet.of(nodeName), ctxt)) {
        if (excludeShutInterfaces && !iface.getActive()) {
          continue;
        }
        RowBuilder row =
            Row.builder(columns)
                .put(COL_INTERFACE, new NodeInterfacePair(nodeName, iface.getName()));

        for (String property : propertySpecifier.getMatchingProperties()) {
          PropertyDescriptor<Interface> propertyDescriptor =
              InterfacePropertySpecifier.JAVA_MAP.get(property);
          try {
            PropertySpecifier.fillProperty(propertyDescriptor, iface, property, row);
          } catch (ClassCastException e) {
            throw new BatfishException(
                String.format(
                    "Type mismatch between property value ('%s') and Schema ('%s') for property '%s' for interface '%s': %s",
                    propertyDescriptor.getGetter().apply(iface),
                    propertyDescriptor.getSchema(),
                    property,
                    iface,
                    e.getMessage()),
                e);
          }
        }

        rows.add(row.build());
      }
    }

    return rows;
  }
}
