package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.question.OspfStatusQuestionPlugin.OspfStatusAnswerElement.OspfStatus;
import org.batfish.specifier.AllInterfacesInterfaceSpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.InterfaceSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

@AutoService(Plugin.class)
public class OspfStatusQuestionPlugin extends QuestionPlugin {

  public static class OspfStatusAnswerElement extends AnswerElement {

    public enum OspfStatus {
      DISABLED_EXPORTED,
      DISABLED_NOT_EXPORTED,
      ENABLED_ACTIVE,
      ENABLED_PASSIVE,
      SWITCHPORT
    }

    public static class OspfInfo implements Comparable<OspfInfo> {
      private static final String PROP_INTERFACE = "interface";
      private static final String PROP_OSPF_STATUS = "ospfStatus";

      private NodeInterfacePair _iface;
      private OspfStatus _ospfStatus;

      @JsonCreator
      private OspfInfo(
          @JsonProperty(PROP_INTERFACE) NodeInterfacePair iface,
          @JsonProperty(PROP_OSPF_STATUS) OspfStatus status) {
        _iface = iface;
        _ospfStatus = status;
      }

      public OspfInfo(String hostname, String interfaceName, OspfStatus status) {
        this(NodeInterfacePair.of(hostname, interfaceName), status);
      }

      @JsonProperty(PROP_INTERFACE)
      public NodeInterfacePair getInterface() {
        return _iface;
      }

      @JsonProperty(PROP_OSPF_STATUS)
      public OspfStatus getOspfStatus() {
        return _ospfStatus;
      }

      @Override
      public int compareTo(OspfInfo o) {
        return Comparator.comparing(OspfInfo::getInterface)
            .thenComparing(OspfInfo::getOspfStatus)
            .compare(this, o);
      }
    }

    private static final String PROP_OSPF_STATUSES = "ospfStatuses";

    private SortedSet<OspfInfo> _ospfStatuses;

    private OspfStatusAnswerElement() {
      this(null);
    }

    @JsonCreator
    private OspfStatusAnswerElement(
        @JsonProperty(PROP_OSPF_STATUSES) SortedSet<OspfInfo> ospfStatuses) {
      _ospfStatuses = (ospfStatuses == null) ? new TreeSet<>() : ospfStatuses;
    }

    public void add(String hostname, String ifaceName, OspfStatus status) {
      _ospfStatuses.add(new OspfInfo(hostname, ifaceName, status));
    }

    @JsonProperty(PROP_OSPF_STATUSES)
    public SortedSet<OspfInfo> getOspfStatus() {
      return _ospfStatuses;
    }
  }

  public static class OspfStatusAnswerer extends Answerer {

    public OspfStatusAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {

      OspfStatusQuestion question = (OspfStatusQuestion) _question;

      OspfStatusAnswerElement answerElement = new OspfStatusAnswerElement();

      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      Set<String> includeNodes = question.getNodeSpecifier().resolve(_batfish.specifierContext());

      for (String hostname : includeNodes) {
        Configuration c = configurations.get(hostname);
        Set<NodeInterfacePair> includeInterfaces =
            question
                .getInterfaceSpecifier()
                .resolve(ImmutableSet.of(hostname), _batfish.specifierContext());
        for (Vrf vrf : c.getVrfs().values()) {
          for (Entry<String, Interface> e2 : vrf.getInterfaces().entrySet()) {
            String interfaceName = e2.getKey();
            Interface iface = e2.getValue();
            if (includeInterfaces.contains(NodeInterfacePair.of(iface))) {
              if (iface.getSwitchport()) {
                // it's a layer2 interface
                conditionalAdd(
                    question, answerElement, hostname, interfaceName, OspfStatus.SWITCHPORT);
              } else if (iface.getOspfEnabled()) {
                // ospf is running either passively or actively
                if (iface.getOspfPassive()) {
                  conditionalAdd(
                      question, answerElement, hostname, interfaceName, OspfStatus.ENABLED_PASSIVE);
                } else {
                  conditionalAdd(
                      question, answerElement, hostname, interfaceName, OspfStatus.ENABLED_ACTIVE);
                }
              } else {
                // check if exported as external ospf route
                boolean exported = false;
                if (iface.getOspfProcess() != null
                    && vrf.getOspfProcesses().containsKey(iface.getOspfProcess())) {
                  OspfProcess proc = vrf.getOspfProcesses().get(iface.getOspfProcess());
                  String exportPolicyName = proc.getExportPolicy();
                  if (exportPolicyName != null) {
                    RoutingPolicy exportPolicy = c.getRoutingPolicies().get(exportPolicyName);
                    if (exportPolicy != null) {
                      for (ConcreteInterfaceAddress address : iface.getAllConcreteAddresses()) {
                        ConnectedRoute route =
                            new ConnectedRoute(
                                Prefix.create(address.getIp(), address.getNetworkBits()),
                                interfaceName);
                        if (exportPolicy.process(
                            route,
                            OspfExternalRoute.builder(),
                            null,
                            vrf.getName(),
                            Direction.OUT)) {
                          exported = true;
                        }
                      }
                    }
                  }
                  if (exported) {
                    conditionalAdd(
                        question,
                        answerElement,
                        hostname,
                        interfaceName,
                        OspfStatus.DISABLED_EXPORTED);
                  } else {
                    conditionalAdd(
                        question,
                        answerElement,
                        hostname,
                        interfaceName,
                        OspfStatus.DISABLED_NOT_EXPORTED);
                  }
                }
              }
            }
          }
        }
      }

      return answerElement;
    }

    private static void conditionalAdd(
        OspfStatusQuestion question,
        OspfStatusAnswerElement answerElement,
        String hostname,
        String interfaceName,
        OspfStatus status) {
      if (question.matchesStatus(status)) {
        answerElement.add(hostname, interfaceName, status);
      }
    }
  }

  /** Lists the OSPF status of interfaces. */
  @ParametersAreNonnullByDefault
  public static class OspfStatusQuestion extends Question {
    private static final String PROP_INTERFACES = "interfaces";
    private static final String PROP_NODES = "nodes";
    private static final String PROP_STATUS = "status";

    @Nullable private String _interfaces;

    @Nullable private String _nodes;

    @Nonnull private Pattern _statusRegex;

    @JsonCreator
    private static OspfStatusQuestion create(
        @Nullable @JsonProperty(PROP_INTERFACES) String ifaceSpec,
        @Nullable @JsonProperty(PROP_NODES) String nodeSpec,
        @Nullable @JsonProperty(PROP_STATUS) String status) {
      return new OspfStatusQuestion(ifaceSpec, nodeSpec, status);
    }

    private OspfStatusQuestion(
        @Nullable @JsonProperty(PROP_INTERFACES) String ifaceSpec,
        @Nullable @JsonProperty(PROP_NODES) String nodeSpec,
        @JsonProperty(PROP_STATUS) String status) {
      _interfaces = ifaceSpec;
      _nodes = nodeSpec;
      _statusRegex =
          Strings.isNullOrEmpty(status)
              ? Pattern.compile(".*")
              : Pattern.compile(status.toUpperCase());
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "ospfstatus";
    }

    @Nullable
    @JsonProperty(PROP_INTERFACES)
    public String getInterfaces() {
      return _interfaces;
    }

    @Nonnull
    @JsonIgnore
    InterfaceSpecifier getInterfaceSpecifier() {
      return SpecifierFactories.getInterfaceSpecifierOrDefault(
          _interfaces, AllInterfacesInterfaceSpecifier.INSTANCE);
    }

    @Nullable
    @JsonProperty(PROP_NODES)
    public String getNodes() {
      return _nodes;
    }

    @JsonIgnore
    NodeSpecifier getNodeSpecifier() {
      return SpecifierFactories.getNodeSpecifierOrDefault(_nodes, AllNodesNodeSpecifier.INSTANCE);
    }

    @JsonProperty(PROP_STATUS)
    private String getStatus() {
      return _statusRegex.toString();
    }

    public boolean matchesStatus(OspfStatus status) {
      return _statusRegex.matcher(status.toString()).matches();
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new OspfStatusAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new OspfStatusQuestion(null, null, null);
  }
}
