package org.batfish.common.plugin;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.Directory;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NodeRoleSpecifier;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.DataPlaneAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentBgpTablesAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentRoutingTablesAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.assertion.AssertionAst;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSets;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.smt.HeaderLocationQuestion;
import org.batfish.datamodel.questions.smt.HeaderQuestion;
import org.batfish.datamodel.questions.smt.RoleQuestion;
import org.batfish.grammar.BgpTableFormat;
import org.batfish.grammar.GrammarSettings;

public interface IBatfish extends IPluginConsumer {

  AnswerElement answerAclReachability(
      String aclNameRegexStr, NamedStructureEquivalenceSets<?> aclEqSets);

  void checkDataPlane();

  void checkEnvironmentExists();

  Set<NodeInterfacePair> computeFlowSinks(
      Map<String, Configuration> configurations, boolean differentialContext, Topology topology);

  Map<String, BiFunction<Question, IBatfish, Answerer>> getAnswererCreators();

  String getContainerName();

  DataPlanePluginSettings getDataPlanePluginSettings();

  String getDifferentialFlowTag();

  Environment getEnvironment();

  Topology getEnvironmentTopology();

  String getFlowTag();

  GrammarSettings getGrammarSettings();

  FlowHistory getHistory();

  NodeRoleSpecifier getNodeRoleSpecifier(boolean inferred);

  Map<String, String> getQuestionTemplates();

  SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes();

  String getTaskId();

  Directory getTestrigFileTree();

  String getTestrigName();

  void initBgpAdvertisements(Map<String, Configuration> configurations);

  void initBgpOriginationSpaceExplicit(Map<String, Configuration> configurations);

  InitInfoAnswerElement initInfo(boolean summary, boolean verboseError);

  InitInfoAnswerElement initInfoBgpAdvertisements(boolean summary, boolean verboseError);

  InitInfoAnswerElement initInfoRoutes(boolean summary, boolean verboseError);

  void initRemoteOspfNeighbors(
      Map<String, Configuration> configurations, Map<Ip, Set<String>> ipOwners, Topology topology);

  void initRemoteRipNeighbors(
      Map<String, Configuration> configurations, Map<Ip, Set<String>> ipOwners, Topology topology);

  SortedMap<String, Configuration> loadConfigurations();

  ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElement();

  DataPlane loadDataPlane();

  SortedMap<String, BgpAdvertisementsByVrf> loadEnvironmentBgpTables();

  SortedMap<String, RoutesByVrf> loadEnvironmentRoutingTables();

  ParseEnvironmentBgpTablesAnswerElement loadParseEnvironmentBgpTablesAnswerElement();

  ParseEnvironmentRoutingTablesAnswerElement loadParseEnvironmentRoutingTablesAnswerElement();

  ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement();

  AnswerElement multipath(HeaderSpace headerSpace, NodesSpecifier ingressNodeRegex);

  AtomicInteger newBatch(String description, int jobs);

  AssertionAst parseAssertion(String text);

  AnswerElement pathDiff(HeaderSpace headerSpace);

  void popEnvironment();

  Set<BgpAdvertisement> loadExternalBgpAnnouncements(Map<String, Configuration> configurations);

  void processFlows(Set<Flow> flows);

  void pushBaseEnvironment();

  void pushDeltaEnvironment();

  @Nullable
  String readExternalBgpAnnouncementsFile();

  AnswerElement reducedReachability(HeaderSpace headerSpace, NodesSpecifier ingressNodeRegex);

  void registerAnswerer(
      String questionName,
      String questionClassName,
      BiFunction<Question, IBatfish, Answerer> answererCreator);

  void registerBgpTablePlugin(BgpTableFormat format, BgpTablePlugin bgpTablePlugin);

  void registerExternalBgpAdvertisementPlugin(
      ExternalBgpAdvertisementPlugin externalBgpAdvertisementPlugin);

  AnswerElement smtBlackhole(HeaderQuestion q);

  AnswerElement smtBoundedLength(HeaderLocationQuestion q, Integer bound);

  AnswerElement smtDeterminism(HeaderQuestion q);

  AnswerElement smtEqualLength(HeaderLocationQuestion q);

  AnswerElement smtForwarding(HeaderQuestion q);

  AnswerElement smtLoadBalance(HeaderLocationQuestion q, int threshold);

  AnswerElement smtLocalConsistency(Pattern routerRegex, boolean strict, boolean fullModel);

  AnswerElement smtMultipathConsistency(HeaderLocationQuestion q);

  AnswerElement smtReachability(HeaderLocationQuestion q);

  AnswerElement smtRoles(RoleQuestion q);

  AnswerElement smtRoutingLoop(HeaderQuestion q);

  AnswerElement standard(
      HeaderSpace headerSpace,
      Set<ForwardingAction> actions,
      NodesSpecifier ingressNodeRegex,
      NodesSpecifier notIngressNodeRegex,
      NodesSpecifier finalNodeRegex,
      NodesSpecifier notFinalNodeRegex,
      Set<String> transitNodes,
      Set<String> notTransitNodes,
      boolean useCompression);

  void registerDataPlanePlugin(DataPlanePlugin plugin, String name);

  DataPlaneAnswerElement computeDataPlane(boolean differentialContext);
}
