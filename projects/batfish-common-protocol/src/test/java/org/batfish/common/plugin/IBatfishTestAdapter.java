package org.batfish.common.plugin;

import java.util.List;
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
import org.batfish.common.BatfishLogger;
import org.batfish.common.Directory;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AclLinesAnswerElementInterface;
import org.batfish.datamodel.answers.AclLinesAnswerElementInterface.AclSpecs;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.DataPlaneAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentBgpTablesAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentRoutingTablesAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.smt.HeaderLocationQuestion;
import org.batfish.datamodel.questions.smt.HeaderQuestion;
import org.batfish.datamodel.questions.smt.RoleQuestion;
import org.batfish.grammar.BgpTableFormat;
import org.batfish.grammar.GrammarSettings;
import org.batfish.question.ReachabilityParameters;
import org.batfish.role.NodeRoleDimension;
import org.batfish.role.NodeRolesData;
import org.batfish.specifier.SpecifierContext;

/**
 * A helper for tests that need an {@link IBatfish} implementation. Extend this and implement the
 * minimal methods needed.
 */
public class IBatfishTestAdapter implements IBatfish {

  @Override
  public void answerAclReachability(
      List<AclSpecs> aclSpecs, AclLinesAnswerElementInterface emptyAnswer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkDataPlane() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkEnvironmentExists() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataPlaneAnswerElement computeDataPlane(boolean differentialContext) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean debugFlagEnabled(String flag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, BiFunction<Question, IBatfish, Answerer>> getAnswererCreators() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getContainerName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataPlanePlugin getDataPlanePlugin() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDifferentialFlowTag() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Environment getEnvironment() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Topology getEnvironmentTopology() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getFlowTag() {
    throw new UnsupportedOperationException();
  }

  @Override
  public GrammarSettings getGrammarSettings() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FlowHistory getHistory() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Layer1Topology getLayer1Topology() {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public Layer2Topology getLayer2Topology() {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public BatfishLogger getLogger() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<NodeRoleDimension> getNodeRoleDimension(String roleDimension) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NodeRolesData getNodeRolesData() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, String> getQuestionTemplates() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes(
      boolean useCompression) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableConfiguration getSettingsConfiguration() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getTaskId() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Directory getTestrigFileTree() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getTestrigName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void initBgpAdvertisements(Map<String, Configuration> configurations) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void initBgpOriginationSpaceExplicit(Map<String, Configuration> configurations) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InitInfoAnswerElement initInfo(boolean summary, boolean verboseError) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InitInfoAnswerElement initInfoBgpAdvertisements(boolean summary, boolean verboseError) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InitInfoAnswerElement initInfoRoutes(boolean summary, boolean verboseError) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void initRemoteRipNeighbors(
      Map<String, Configuration> configurations, Map<Ip, Set<String>> ipOwners, Topology topology) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedMap<String, Configuration> loadConfigurations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElementOrReparse() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataPlane loadDataPlane() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedMap<String, BgpAdvertisementsByVrf> loadEnvironmentBgpTables() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedMap<String, RoutesByVrf> loadEnvironmentRoutingTables() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<BgpAdvertisement> loadExternalBgpAnnouncements(
      Map<String, Configuration> configurations) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseEnvironmentBgpTablesAnswerElement loadParseEnvironmentBgpTablesAnswerElement() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseEnvironmentRoutingTablesAnswerElement
      loadParseEnvironmentRoutingTablesAnswerElement() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement multipath(ReachabilityParameters reachabilityParameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AtomicInteger newBatch(String description, int jobs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement pathDiff(ReachabilityParameters reachabilityParameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void popEnvironment() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void processFlows(Set<Flow> flows, boolean ignoreAcls) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void pushBaseEnvironment() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void pushDeltaEnvironment() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public String readExternalBgpAnnouncementsFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement reducedReachability(ReachabilityParameters reachabilityParameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerAnswerer(
      String questionName,
      String questionClassName,
      BiFunction<Question, IBatfish, Answerer> answererCreator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerBgpTablePlugin(BgpTableFormat format, BgpTablePlugin bgpTablePlugin) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerDataPlanePlugin(DataPlanePlugin plugin, String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerExternalBgpAdvertisementPlugin(
      ExternalBgpAdvertisementPlugin externalBgpAdvertisementPlugin) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtBlackhole(HeaderQuestion q) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtBoundedLength(HeaderLocationQuestion q, Integer bound) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtDeterminism(HeaderQuestion q) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtEqualLength(HeaderLocationQuestion q) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtForwarding(HeaderQuestion q) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtLoadBalance(HeaderLocationQuestion q, int threshold) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtLocalConsistency(Pattern routerRegex, boolean strict, boolean fullModel) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtMultipathConsistency(HeaderLocationQuestion q) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtReachability(HeaderLocationQuestion q) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtRoles(RoleQuestion q) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement smtRoutingLoop(HeaderQuestion q) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SpecifierContext specifierContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnswerElement standard(ReachabilityParameters reachabilityParameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeDataPlane(DataPlane dp, DataPlaneAnswerElement ae) {
    throw new UnsupportedOperationException();
  }
}
