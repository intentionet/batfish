package org.batfish.question.mlag;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Mlag;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.ConstantNameSetSpecifier;
import org.batfish.specifier.SpecifierFactories;
import org.batfish.specifier.parboiled.Grammar;

/** Answerer for the {@link MlagPropertiesQuestion} */
public final class MlagPropertiesAnswerer extends Answerer {
  static final String COL_MLAG_ID = "MLAG_ID";
  static final String COL_MLAG_LOCAL_INTERFACE = "Source_Interface";
  static final String COL_MLAG_PEER_ADDRESS = "Peer_Address";
  static final String COL_MLAG_PEER_INTERFACE = "Local_Interface";
  static final String COL_NODE = "Node";

  MlagPropertiesAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    MlagPropertiesQuestion question = (MlagPropertiesQuestion) _question;
    Set<String> nodes =
        SpecifierFactories.getNodeSpecifierOrDefault(
                question.getNodeSpecInput(), AllNodesNodeSpecifier.INSTANCE)
            .resolve(_batfish.specifierContext());
    Set<String> mlagIds =
        SpecifierFactories.getNameSetSpecifierOrDefault(
                question.getMlagIdSpecInput(),
                Grammar.MLAG_ID_SPECIFIER,
                new ConstantNameSetSpecifier(getAllMlagIds(_batfish.loadConfigurations())))
            .resolve(_batfish.specifierContext());
    SortedMap<String, Configuration> configs = _batfish.loadConfigurations();

    return computeAnswer(nodes, mlagIds, configs);
  }

  @VisibleForTesting
  @Nonnull
  static TableAnswerElement computeAnswer(
      Set<String> nodes, Set<String> mlagIds, SortedMap<String, Configuration> configs) {
    ImmutableList<NodeToMlags> mlagConfigs =
        nodes.stream()
            .map(configs::get)
            .filter(Objects::nonNull)
            // Get matching MLAGs at each node
            .map(
                c ->
                    new NodeToMlags(
                        c.getHostname(),
                        c.getMlags().values().stream()
                            .filter(mlag -> mlagIds.contains(mlag.getId()))
                            .collect(Collectors.toList())))
            .filter(mc -> !mc._mlags.isEmpty())
            .collect(ImmutableList.toImmutableList());
    TableAnswerElement answer = new TableAnswerElement(getMetadata());
    mlagConfigs.forEach(
        c -> c._mlags.forEach(mlag -> answer.addRow(configToRow(c._hostname, mlag))));
    return answer;
  }

  @VisibleForTesting
  @Nonnull
  static TableMetadata getMetadata() {
    Builder<ColumnMetadata> b = ImmutableList.builder();
    b.add(
        new ColumnMetadata(COL_NODE, Schema.NODE, "Node name", true, false),
        new ColumnMetadata(COL_MLAG_ID, Schema.STRING, "MLAG domain ID", true, false),
        new ColumnMetadata(COL_MLAG_PEER_ADDRESS, Schema.IP, "Peer's IP address", false, true),
        new ColumnMetadata(
            COL_MLAG_PEER_INTERFACE,
            Schema.INTERFACE,
            "Local interface used for MLAG peering",
            false,
            true),
        new ColumnMetadata(
            COL_MLAG_LOCAL_INTERFACE,
            Schema.INTERFACE,
            "Local interface used as source-interface for MLAG peering",
            false,
            true));
    return new TableMetadata(b.build());
  }

  @VisibleForTesting
  @Nonnull
  static Row configToRow(String hostname, Mlag mlag) {
    return Row.builder(getMetadata().toColumnMap())
        .put(COL_NODE, new Node(hostname))
        .put(COL_MLAG_ID, mlag.getId())
        .put(COL_MLAG_PEER_ADDRESS, mlag.getPeerAddress())
        .put(
            COL_MLAG_PEER_INTERFACE,
            mlag.getPeerInterface() != null
                ? NodeInterfacePair.of(hostname, mlag.getPeerInterface())
                : null)
        .put(
            COL_MLAG_LOCAL_INTERFACE,
            mlag.getLocalInterface() != null
                ? NodeInterfacePair.of(hostname, mlag.getLocalInterface())
                : null)
        .build();
  }

  private static final class NodeToMlags {
    private String _hostname;
    private Collection<Mlag> _mlags;

    private NodeToMlags(String hostname, Collection<Mlag> mlag) {
      _hostname = hostname;
      _mlags = mlag;
    }
  }

  @VisibleForTesting
  static Set<String> getAllMlagIds(Map<String, Configuration> configs) {
    return configs.values().stream()
        .flatMap(c -> c.getMlags().keySet().stream())
        .collect(ImmutableSet.toImmutableSet());
  }
}
