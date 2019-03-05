package org.batfish.question.bgpproperties;

import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.CLUSTER_ID;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.EXPORT_POLICY;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.IMPORT_POLICY;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.IS_PASSIVE;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.LOCAL_AS;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.LOCAL_IP;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.PEER_GROUP;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.REMOTE_AS;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.ROUTE_REFLECTOR_CLIENT;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.SEND_COMMUNITY;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.answers.SelfDescribingObject;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.BgpPeerPropertySpecifier;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.PropertySpecifier;
import org.batfish.datamodel.questions.PropertySpecifier.PropertyDescriptor;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierContext;

public class BgpPeerConfigurationAnswerer extends Answerer {

  public static final String COL_NODE = "Node";
  public static final String COL_VRF = "VRF";
  public static final String COL_REMOTE_IP = "Remote_IP";

  private static final List<String> COLUMN_ORDER =
      ImmutableList.of(
          COL_NODE,
          COL_VRF,
          LOCAL_AS,
          LOCAL_IP,
          REMOTE_AS,
          COL_REMOTE_IP,
          ROUTE_REFLECTOR_CLIENT,
          CLUSTER_ID,
          PEER_GROUP,
          IMPORT_POLICY,
          EXPORT_POLICY,
          SEND_COMMUNITY,
          IS_PASSIVE);

  public BgpPeerConfigurationAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  /**
   * Creates {@link ColumnMetadata}s that the answer should have.
   *
   * @return The {@link List} of {@link ColumnMetadata}s
   */
  public static List<ColumnMetadata> createColumnMetadata(
      BgpPeerPropertySpecifier propertySpecifier) {
    Map<String, ColumnMetadata> columnMetadatas =
        propertySpecifier.getMatchingProperties().stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    prop ->
                        new ColumnMetadata(
                            getColumnName(prop),
                            BgpPeerPropertySpecifier.JAVA_MAP.get(prop).getSchema(),
                            "Property " + prop,
                            false,
                            true)));
    columnMetadatas.put(COL_NODE, new ColumnMetadata(COL_NODE, Schema.NODE, "Node", true, false));
    columnMetadatas.put(COL_VRF, new ColumnMetadata(COL_VRF, Schema.STRING, "VRF", true, false));
    columnMetadatas.put(
        COL_REMOTE_IP,
        new ColumnMetadata(COL_REMOTE_IP, Schema.SELF_DESCRIBING, "Remote IP", true, false));

    // Check for unknown columns (present in BgpPeerPropertySpecifier but not COLUMN_ORDER)
    List<ColumnMetadata> unknownColumns =
        columnMetadatas.entrySet().stream()
            .filter(e -> !COLUMN_ORDER.contains(e.getKey()))
            .map(Entry::getValue)
            .collect(ImmutableList.toImmutableList());

    // List the metadatas in order, with any unknown columns tacked onto the end of the table
    return Stream.concat(
            COLUMN_ORDER.stream()
                .filter(prop -> columnMetadatas.containsKey(prop))
                .map(prop -> columnMetadatas.get(prop)),
            unknownColumns.stream())
        .collect(ImmutableList.toImmutableList());
  }

  /** Creates a {@link TableMetadata} object from the question. */
  static TableMetadata createTableMetadata(BgpPeerConfigurationQuestion question) {
    String textDesc =
        String.format(
            "Properties of BGP peer ${%s}:${%s}: ${%s}", COL_NODE, COL_VRF, COL_REMOTE_IP);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(createColumnMetadata(question.getProperties()), textDesc);
  }

  @Override
  public AnswerElement answer() {
    BgpPeerConfigurationQuestion question = (BgpPeerConfigurationQuestion) _question;

    TableMetadata tableMetadata = createTableMetadata(question);
    TableAnswerElement answer = new TableAnswerElement(tableMetadata);

    Multiset<Row> propertyRows =
        getAnswerRows(
            _batfish.specifierContext(),
            question.getNodeSpecifier(),
            tableMetadata.toColumnMap(),
            question.getProperties());

    answer.postProcessAnswer(question, propertyRows);
    return answer;
  }

  @VisibleForTesting
  public static Multiset<Row> getAnswerRows(
      SpecifierContext ctxt,
      NodeSpecifier nodeSpecifier,
      Map<String, ColumnMetadata> columnMetadata,
      BgpPeerPropertySpecifier propertySpecifier) {

    Multiset<Row> rows = HashMultiset.create();

    for (String nodeName : nodeSpecifier.resolve(ctxt)) {
      for (Vrf vrf : ctxt.getConfigs().get(nodeName).getVrfs().values()) {
        BgpProcess bgpProcess = vrf.getBgpProcess();
        if (bgpProcess == null) {
          continue;
        }
        Node node = new Node(nodeName);
        for (BgpActivePeerConfig peer : bgpProcess.getActiveNeighbors().values()) {
          rows.add(getRow(node, vrf.getName(), peer, columnMetadata, propertySpecifier));
        }
        for (BgpPassivePeerConfig peer : bgpProcess.getPassiveNeighbors().values()) {
          rows.add(getRow(node, vrf.getName(), peer, columnMetadata, propertySpecifier));
        }
      }
    }
    return rows;
  }

  private static Row getRow(
      Node node,
      String vrfName,
      BgpPeerConfig peer,
      Map<String, ColumnMetadata> columnMetadata,
      BgpPeerPropertySpecifier propertySpecifier) {
    RowBuilder rowBuilder =
        Row.builder(columnMetadata)
            .put(COL_NODE, node)
            .put(COL_VRF, vrfName)
            .put(COL_REMOTE_IP, getRemoteIp(peer));
    for (String property : propertySpecifier.getMatchingProperties()) {
      PropertyDescriptor<BgpPeerConfig> propertyDescriptor =
          BgpPeerPropertySpecifier.JAVA_MAP.get(property);
      try {
        PropertySpecifier.fillProperty(propertyDescriptor, peer, property, rowBuilder);
      } catch (ClassCastException e) {
        throw new BatfishException(
            String.format(
                "Type mismatch between property value ('%s') and Schema ('%s') for property '%s' for BGP peer '%s->%s-%s': %s",
                propertyDescriptor.getGetter().apply(peer),
                propertyDescriptor.getSchema(),
                property,
                node.getName(),
                vrfName,
                peer,
                e.getMessage()),
            e);
      }
    }
    return rowBuilder.build();
  }

  @VisibleForTesting
  static SelfDescribingObject getRemoteIp(@Nonnull BgpPeerConfig peer) {
    if (peer instanceof BgpActivePeerConfig) {
      return new SelfDescribingObject(Schema.IP, ((BgpActivePeerConfig) peer).getPeerAddress());
    }
    if (peer instanceof BgpPassivePeerConfig) {
      return new SelfDescribingObject(Schema.PREFIX, ((BgpPassivePeerConfig) peer).getPeerPrefix());
    }
    throw new IllegalArgumentException(
        String.format("Peer is neither Active nor Passive: %s", peer));
  }

  /** Returns the name of the column that contains the value of property {@code property} */
  public static String getColumnName(String property) {
    return property;
  }
}
