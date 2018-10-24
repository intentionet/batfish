package org.batfish.question.specifiers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.FilterSpecifier;
import org.batfish.specifier.InterfaceSpecifier;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.Location;
import org.batfish.specifier.SpecifierContext;

public final class SpecifiersAnswerer extends Answerer {

  public static final String COL_FILTER_NAME = "Filter_Name";
  public static final String COL_INTERFACE = "Interface";
  public static final String COL_IP_SPACE = "IP_Space";
  public static final String COL_LOCATION = "Location";
  public static final String COL_LOCATIONS = "Locations";
  public static final String COL_NODE = "Node";

  public SpecifiersAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    SpecifiersQuestion question = (SpecifiersQuestion) _question;
    SpecifierContext context = _batfish.specifierContext();

    switch (question.getQueryType()) {
      case FILTER:
        return resolveFilter(question, context);
      case INTERFACE:
        return resolveInterface(question, context);
      case IP_SPACE:
        return resolveIpSpace(question, context);
      case IP_SPACE_OF_LOCATION:
        return resolveIpSpaceOfLocation(question, context);
      case LOCATION:
        return resolveLocation(question, context);
      case NODE:
        return resolveNode(question, context);
      default:
        throw new IllegalArgumentException("Unhandled query type: " + question.getQueryType());
    }
  }

  @VisibleForTesting
  static TableAnswerElement resolveFilter(SpecifiersQuestion question, SpecifierContext context) {

    List<ColumnMetadata> columns =
        ImmutableList.of(
            new ColumnMetadata(COL_NODE, Schema.NODE, "Node", false, false),
            new ColumnMetadata(COL_FILTER_NAME, Schema.STRING, "Filter name", false, false));

    TableAnswerElement table = new TableAnswerElement(new TableMetadata(columns));
    Map<String, ColumnMetadata> columnMap = table.getMetadata().toColumnMap();

    FilterSpecifier filterSpecifier = question.getFilterSpecifier();
    Set<String> nodes = question.getNodeSpecifier().resolve(context);

    for (String node : nodes) {
      Set<IpAccessList> nodeFilters = filterSpecifier.resolve(node, context);
      for (IpAccessList filter : nodeFilters) {
        table.addRow(
            Row.of(
                columnMap,
                COL_NODE,
                new Node(node),
                COL_FILTER_NAME,
                Objects.toString(filter.getName())));
      }
    }

    return table;
  }

  @VisibleForTesting
  static TableAnswerElement resolveInterface(
      SpecifiersQuestion question, SpecifierContext context) {

    List<ColumnMetadata> columns =
        ImmutableList.of(
            new ColumnMetadata(COL_INTERFACE, Schema.INTERFACE, "Interface", false, false));

    TableAnswerElement table = new TableAnswerElement(new TableMetadata(columns));
    Map<String, ColumnMetadata> columnMap = table.getMetadata().toColumnMap();

    InterfaceSpecifier interfaceSpecifier = question.getInterfaceSpecifier();
    Set<String> nodes = question.getNodeSpecifier().resolve(context);

    Set<Interface> interfaces = interfaceSpecifier.resolve(nodes, context);
    for (Interface iface : interfaces) {
      table.addRow(Row.of(columnMap, COL_INTERFACE, new NodeInterfacePair(iface)));
    }

    return table;
  }

  @VisibleForTesting
  static TableAnswerElement resolveIpSpace(SpecifiersQuestion question, SpecifierContext context) {
    List<ColumnMetadata> columns =
        ImmutableList.of(new ColumnMetadata(COL_IP_SPACE, Schema.STRING, "IP space", false, false));
    TableAnswerElement table = new TableAnswerElement(new TableMetadata(columns));
    Map<String, ColumnMetadata> columnMap = table.getMetadata().toColumnMap();

    // this will yield all default locations for the factory
    Set<Location> locations = question.getLocationSpecifier().resolve(context);
    IpSpaceAssignment ipSpaceAssignment =
        question.getIpSpaceSpecifier().resolve(locations, context);

    for (IpSpaceAssignment.Entry entry : ipSpaceAssignment.getEntries()) {
      table.addRow(Row.of(columnMap, COL_IP_SPACE, Objects.toString(entry.getIpSpace())));
    }
    return table;
  }

  @VisibleForTesting
  static TableAnswerElement resolveIpSpaceOfLocation(
      SpecifiersQuestion question, SpecifierContext context) {

    List<ColumnMetadata> columns =
        ImmutableList.of(
            new ColumnMetadata(COL_LOCATIONS, Schema.STRING, "Resolution", false, false),
            new ColumnMetadata(COL_IP_SPACE, Schema.STRING, "IP space", false, false));
    TableAnswerElement table = new TableAnswerElement(new TableMetadata(columns));
    Map<String, ColumnMetadata> columnMap = table.getMetadata().toColumnMap();

    Set<Location> locations = question.getLocationSpecifier().resolve(context);
    IpSpaceAssignment ipSpaceAssignment =
        question.getIpSpaceSpecifier().resolve(locations, context);

    for (IpSpaceAssignment.Entry entry : ipSpaceAssignment.getEntries()) {
      table.addRow(
          Row.of(
              columnMap,
              COL_LOCATIONS,
              entry.getLocations().toString(),
              COL_IP_SPACE,
              Objects.toString(entry.getIpSpace())));
    }
    return table;
  }

  @VisibleForTesting
  static TableAnswerElement resolveLocation(SpecifiersQuestion question, SpecifierContext context) {

    List<ColumnMetadata> columns =
        ImmutableList.of(new ColumnMetadata(COL_LOCATION, Schema.STRING, "Location", false, false));
    TableAnswerElement table = new TableAnswerElement(new TableMetadata(columns));
    Map<String, ColumnMetadata> columnMap = table.getMetadata().toColumnMap();

    Set<Location> locations = question.getLocationSpecifier().resolve(context);
    for (Location location : locations) {
      table.addRow(Row.of(columnMap, COL_LOCATION, location.toString()));
    }
    return table;
  }

  @VisibleForTesting
  static TableAnswerElement resolveNode(SpecifiersQuestion question, SpecifierContext context) {

    List<ColumnMetadata> columns =
        ImmutableList.of(new ColumnMetadata(COL_NODE, Schema.NODE, "Node", false, false));
    TableAnswerElement table = new TableAnswerElement(new TableMetadata(columns));
    Map<String, ColumnMetadata> columnMap = table.getMetadata().toColumnMap();

    Set<String> nodes = question.getNodeSpecifier().resolve(context);
    for (String node : nodes) {
      table.addRow(Row.of(columnMap, COL_NODE, new Node(node)));
    }
    return table;
  }
}
