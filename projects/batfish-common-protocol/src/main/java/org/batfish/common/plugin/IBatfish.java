package org.batfish.common.plugin;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.batfish.common.Answerer;
import org.batfish.common.Directory;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AclLinesAnswerElementInterface;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.DataPlaneAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentBgpTablesAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentRoutingTablesAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSets;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.ReachabilitySettings;
import org.batfish.datamodel.questions.smt.HeaderLocationQuestion;
import org.batfish.datamodel.questions.smt.HeaderQuestion;
import org.batfish.datamodel.questions.smt.RoleQuestion;
import org.batfish.grammar.BgpTableFormat;
import org.batfish.grammar.GrammarSettings;
import org.batfish.role.NodeRoleDimension;
import org.batfish.role.NodeRolesData;

public interface IBatfish extends IPluginConsumer {

  void answerAclReachability(
      String aclNameRegexStr,
      NamedStructureEquivalenceSets<?> aclEqSets,
      AclLinesAnswerElementInterface emptyAnswer);

  void checkDataPlane();

  void checkEnvironmentExists();

  DataPlaneAnswerElement computeDataPlane(boolean differentialContext);

  boolean debugFlagEnabled(String flag);

  Map<String, BiFunction<Question, IBatfish, Answerer>> getAnswererCreators();

  String getContainerName();

  DataPlanePlugin getDataPlanePlugin();

  DataPlanePluginSettings getDataPlanePluginSettings();

  String getDifferentialFlowTag();

  Environment getEnvironment();

  Topology getEnvironmentTopology();

  String getFlowTag();

  GrammarSettings getGrammarSettings();

  FlowHistory getHistory();

  NodeRolesData getNodeRolesData();

  Optional<NodeRoleDimension> getNodeRoleDimension(String roleDimension);

  Map<String, String> getQuestionTemplates();

  SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes(boolean useCompression);

  /**
   * Get batfish settings
   *
   * @return the {@link ImmutableConfiguration} that represents batfish settings.
   */
  ImmutableConfiguration getSettingsConfiguration();

  String getTaskId();

  Directory getTestrigFileTree();

  String getTestrigName();

  void initBgpAdvertisements(Map<String, Configuration> configurations);

  void initBgpOriginationSpaceExplicit(Map<String, Configuration> configurations);

  InitInfoAnswerElement initInfo(boolean summary, boolean verboseError);

  InitInfoAnswerElement initInfoBgpAdvertisements(boolean summary, boolean verboseError);

  InitInfoAnswerElement initInfoRoutes(boolean summary, boolean verboseError);

  void initRemoteRipNeighbors(
      Map<String, Configuration> configurations, Map<Ip, Set<String>> ipOwners, Topology topology);

  SortedMap<String, Configuration> loadConfigurations();

  ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElementOrReparse();

  DataPlane loadDataPlane();

  SortedMap<String, BgpAdvertisementsByVrf> loadEnvironmentBgpTables();

  SortedMap<String, RoutesByVrf> loadEnvironmentRoutingTables();

  ParseEnvironmentBgpTablesAnswerElement loadParseEnvironmentBgpTablesAnswerElement();

  ParseEnvironmentRoutingTablesAnswerElement loadParseEnvironmentRoutingTablesAnswerElement();

  ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement();

  AnswerElement multipath(ReachabilitySettings reachabilitySettings);

  AtomicInteger newBatch(String description, int jobs);

  AnswerElement pathDiff(ReachabilitySettings reachabilitySettings);

  void popEnvironment();

  Set<BgpAdvertisement> loadExternalBgpAnnouncements(Map<String, Configuration> configurations);

  void processFlows(Set<Flow> flows, boolean ignoreAcls);

  void pushBaseEnvironment();

  void pushDeltaEnvironment();

  @Nullable
  String readExternalBgpAnnouncementsFile();

  AnswerElement reducedReachability(ReachabilitySettings reachabilitySettings);

  void registerAnswerer(
      String questionName,
      String questionClassName,
      BiFunction<Question, IBatfish, Answerer> answererCreator);

  void registerBgpTablePlugin(BgpTableFormat format, BgpTablePlugin bgpTablePlugin);

  /**
   * Register a new dataplane plugin
   *
   * @param plugin a {@link DataPlanePlugin} capable of computing a dataplane
   * @param name name of the plugin, will be used to register the plugin and prefixed to all
   *     plugin-specific settings (and hence command line arguments)
   */
  void registerDataPlanePlugin(DataPlanePlugin plugin, String name);

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

  AnswerElement standard(ReachabilitySettings reachabilitySettings);

  void writeDataPlane(DataPlane dp, DataPlaneAnswerElement ae);
}
