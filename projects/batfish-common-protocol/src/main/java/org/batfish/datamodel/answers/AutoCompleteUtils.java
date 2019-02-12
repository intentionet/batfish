package org.batfish.datamodel.answers;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.CompletionMetadata;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.Protocol;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.questions.BgpPeerPropertySpecifier;
import org.batfish.datamodel.questions.BgpProcessPropertySpecifier;
import org.batfish.datamodel.questions.ConfiguredSessionStatus;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.IpsecSessionStatus;
import org.batfish.datamodel.questions.NamedStructureSpecifier;
import org.batfish.datamodel.questions.NodePropertySpecifier;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.questions.Variable;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.role.NodeRoleDimension;
import org.batfish.role.NodeRolesData;
import org.batfish.specifier.DispositionSpecifier;
import org.batfish.specifier.IpProtocolSpecifier;
import org.batfish.specifier.RoutingProtocolSpecifier;
import org.batfish.specifier.parboiled.ParboiledAutoComplete;

@ParametersAreNonnullByDefault
public final class AutoCompleteUtils {

  @Nonnull
  public static List<AutocompleteSuggestion> autoComplete(
      Variable.Type completionType, String query, int maxSuggestions) {
    return autoComplete(null, null, completionType, query, maxSuggestions, null, null, null);
  }

  @Nonnull
  public static List<AutocompleteSuggestion> autoComplete(
      @Nullable String network,
      @Nullable String snapshot,
      Variable.Type completionType,
      String query,
      int maxSuggestions,
      @Nullable CompletionMetadata completionMetadata,
      @Nullable NodeRolesData nodeRolesData,
      @Nullable ReferenceLibrary referenceLibrary) {
    List<AutocompleteSuggestion> suggestions;

    switch (completionType) {
      case BGP_PEER_PROPERTY_SPEC:
        {
          suggestions = baseAutoComplete(query, BgpPeerPropertySpecifier.JAVA_MAP.keySet());
          break;
        }
      case BGP_PROCESS_PROPERTY_SPEC:
        {
          suggestions = baseAutoComplete(query, BgpProcessPropertySpecifier.JAVA_MAP.keySet());
          break;
        }
      case BGP_SESSION_STATUS:
        {
          suggestions =
              baseAutoComplete(
                  query,
                  Stream.of(ConfiguredSessionStatus.values())
                      .map(ConfiguredSessionStatus::name)
                      .collect(Collectors.toSet()));
          break;
        }
      case BGP_SESSION_TYPE:
        {
          suggestions =
              baseAutoComplete(
                  query,
                  Stream.of(SessionType.values())
                      .map(SessionType::name)
                      .collect(Collectors.toSet()));

          break;
        }
      case DISPOSITION_SPEC:
        {
          suggestions = DispositionSpecifier.autoComplete(query);
          break;
        }
      case FILTER:
        {
          if (completionMetadata == null) {
            return null;
          }
          suggestions = baseAutoComplete(query, completionMetadata.getFilterNames());
          break;
        }
      case FLOW_STATE:
        {
          suggestions =
              baseAutoComplete(
                  query,
                  Stream.of(FlowState.values()).map(FlowState::name).collect(Collectors.toSet()));
          break;
        }
      case INTERFACE:
        {
          if (completionMetadata == null) {
            return null;
          }
          suggestions =
              baseAutoComplete(
                  query,
                  completionMetadata.getInterfaces().stream()
                      .map(NodeInterfacePair::toString)
                      .collect(Collectors.toSet()));
          break;
        }
      case INTERFACE_PROPERTY_SPEC:
        {
          suggestions = baseAutoComplete(query, InterfacePropertySpecifier.JAVA_MAP.keySet());
          break;
        }
      case IP:
        {
          checkCompletionMetadata(completionMetadata, network, snapshot);
          suggestions = baseAutoComplete(query, completionMetadata.getIps());
          break;
        }
      case IP_PROTOCOL_SPEC:
        {
          suggestions = IpProtocolSpecifier.autoComplete(query);
          break;
        }
      case IP_SPACE_SPEC:
        {
          suggestions =
              ParboiledAutoComplete.autoCompleteIpSpace(
                  query, maxSuggestions, completionMetadata, nodeRolesData, referenceLibrary);
          break;
        }
      case IPSEC_SESSION_STATUS:
        {
          suggestions =
              baseAutoComplete(
                  query,
                  Stream.of(IpsecSessionStatus.values())
                      .map(IpsecSessionStatus::name)
                      .collect(Collectors.toSet()));
          break;
        }
      case NAMED_STRUCTURE_SPEC:
        {
          suggestions = baseAutoComplete(query, NamedStructureSpecifier.JAVA_MAP.keySet());
          break;
        }
      case NODE_PROPERTY_SPEC:
        {
          suggestions = baseAutoComplete(query, NodePropertySpecifier.JAVA_MAP.keySet());
          break;
        }
      case NODE_ROLE_DIMENSION:
        {
          checkNodeRolesData(nodeRolesData, network);
          suggestions =
              baseAutoComplete(
                  query,
                  nodeRolesData.getNodeRoleDimensions().stream()
                      .map(NodeRoleDimension::getName)
                      .collect(Collectors.toSet()));
          break;
        }
      case NODE_SPEC:
        {
          checkCompletionMetadata(completionMetadata, network, snapshot);
          checkNodeRolesData(nodeRolesData, network);
          suggestions =
              NodesSpecifier.autoComplete(query, completionMetadata.getNodes(), nodeRolesData);
          break;
        }
      case OSPF_PROPERTY_SPEC:
        {
          suggestions = baseAutoComplete(query, OspfPropertySpecifier.JAVA_MAP.keySet());
          break;
        }
      case PREFIX:
        {
          checkCompletionMetadata(completionMetadata, network, snapshot);
          suggestions = baseAutoComplete(query, completionMetadata.getPrefixes());
          break;
        }
      case PROTOCOL:
        {
          suggestions =
              baseAutoComplete(
                  query,
                  Stream.of(Protocol.values()).map(Protocol::name).collect(Collectors.toSet()));
          break;
        }
      case ROUTING_PROTOCOL_SPEC:
        {
          suggestions = RoutingProtocolSpecifier.autoComplete(query);
          break;
        }
      case STRUCTURE_NAME:
        {
          checkCompletionMetadata(completionMetadata, network, snapshot);
          suggestions = baseAutoComplete(query, completionMetadata.getStructureNames());
          break;
        }
      case VRF:
        {
          checkCompletionMetadata(completionMetadata, network, snapshot);
          suggestions = baseAutoComplete(query, completionMetadata.getVrfs());
          break;
        }
      case ZONE:
        {
          checkCompletionMetadata(completionMetadata, network, snapshot);
          suggestions = baseAutoComplete(query, completionMetadata.getZones());
          break;
        }
      default:
        throw new IllegalArgumentException("Unsupported completion type: " + completionType);
    }
    return suggestions.subList(0, Integer.min(suggestions.size(), maxSuggestions));
  }

  /**
   * Returns a list of suggestions based on the query. The current implementation treats the query
   * as a substring of the property string.
   *
   * @param query The query that came to the concrete child class
   * @return The list of suggestions
   */
  public static List<AutocompleteSuggestion> baseAutoComplete(
      @Nullable String query, Set<String> allProperties) {

    String finalQuery = firstNonNull(query, "").toLowerCase();
    ImmutableList.Builder<AutocompleteSuggestion> suggestions = new ImmutableList.Builder<>();
    String queryWithStars = ".*" + (finalQuery.isEmpty() ? "" : finalQuery + ".*");
    Pattern queryPattern = safeGetPattern(queryWithStars);

    /*
     * if queryWithStars is not a valid Pattern, finalQuery must be a funky string that will not
     * match anything as string.contains or regex.matches; so we skip formalities altogether
     */
    if (queryPattern != null) {
      suggestions.addAll(
          allProperties.stream()
              .filter(prop -> queryPattern.matcher(prop.toLowerCase()).matches())
              .map(prop -> new AutocompleteSuggestion(prop, false))
              .collect(Collectors.toList()));
    }
    return suggestions.build();
  }

  private static void checkCompletionMetadata(
      CompletionMetadata completionMetadata, String network, String snapshot) {
    checkArgument(
        completionMetadata != null,
        "Cannot autocomplete because completion metadata not found for %s / %s",
        network,
        snapshot);
  }

  private static void checkNodeRolesData(NodeRolesData nodeRolesData, String network) {
    checkArgument(
        nodeRolesData != null,
        "Cannot autocomplete because node roles data not found for %s",
        network);
  }

  /** Returns the Pattern if {@code candidateRegex} is a valid regex, and null otherwise */
  private static Pattern safeGetPattern(String candidateRegex) {
    try {
      return Pattern.compile(candidateRegex);
    } catch (PatternSyntaxException e) {
      return null;
    }
  }
}
