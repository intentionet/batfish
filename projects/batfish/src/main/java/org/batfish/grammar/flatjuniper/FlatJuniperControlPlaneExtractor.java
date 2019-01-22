package org.batfish.grammar.flatjuniper;

import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.Warnings;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.representation.juniper.JuniperConfiguration;
import org.batfish.vendor.VendorConfiguration;

public class FlatJuniperControlPlaneExtractor implements ControlPlaneExtractor {

  private final FlatJuniperCombinedParser _parser;
  private final String _text;
  private final Warnings _w;
  private JuniperConfiguration _configuration;

  public FlatJuniperControlPlaneExtractor(
      String fileText, FlatJuniperCombinedParser combinedParser, Warnings warnings) {
    _text = fileText;
    _parser = combinedParser;
    _w = warnings;
  }

  @Override
  public VendorConfiguration getVendorConfiguration() {
    return _configuration;
  }

  @Override
  public void processParseTree(ParserRuleContext tree) {
    Hierarchy hierarchy = new Hierarchy();
    ParseTreeWalker walker = new ParseTreeWalker();
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::DeactivateTreeBuilder").startActive()) {
      assert span != null; // avoid unused warning
      DeactivateTreeBuilder dtb = new DeactivateTreeBuilder(hierarchy);
      walker.walk(dtb, tree);
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::DeactivateLinePruner").startActive()) {
      assert span != null; // avoid unused warning
      DeactivateLinePruner dp = new DeactivateLinePruner();
      walker.walk(dp, tree);
    }
    DeactivatedLinePruner dlp = new DeactivatedLinePruner(hierarchy);
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::DeactivatedLinePruner").startActive()) {
      assert span != null; // avoid unused warning
      walker.walk(dlp, tree);
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::InitialTreeBuilder").startActive()) {
      assert span != null; // avoid unused warning
      InitialTreeBuilder tb = new InitialTreeBuilder(hierarchy);
      walker.walk(tb, tree);
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::GroupTreeBuilder").startActive()) {
      assert span != null; // avoid unused warning
      GroupTreeBuilder gb = new GroupTreeBuilder(_parser, hierarchy);
      walker.walk(gb, tree);
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::ApplyGroupsApplicator").startActive()) {
      assert span != null; // avoid unused warning
      ApplyGroupsApplicator hb;
      do {
        hb = new ApplyGroupsApplicator(hierarchy, _w);
        walker.walk(hb, tree);
      } while (hb.getChanged());
    }
    try (ActiveSpan span = GlobalTracer.get().buildSpan("FlatJuniper::GroupPruner").startActive()) {
      assert span != null; // avoid unused warning
      GroupPruner gp = new GroupPruner();
      walker.walk(gp, tree);
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::WildcardApplicator").startActive()) {
      assert span != null; // avoid unused warning
      WildcardApplicator wa = new WildcardApplicator(hierarchy);
      walker.walk(wa, tree);
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::WildcardPruner").startActive()) {
      assert span != null; // avoid unused warning
      WildcardPruner wp = new WildcardPruner();
      walker.walk(wp, tree);
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::DeactivatedLinePruner again").startActive()) {
      assert span != null; // avoid unused warning
      walker.walk(dlp, tree);
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::ApplyPathApplicator").startActive()) {
      assert span != null; // avoid unused warning
      ApplyPathApplicator ap = new ApplyPathApplicator(hierarchy, _w);
      walker.walk(ap, tree);
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("FlatJuniper::ConfigurationBuilder").startActive()) {
      assert span != null; // avoid unused warning
      ConfigurationBuilder cb =
          new ConfigurationBuilder(_parser, _text, _w, hierarchy.getTokenInputs());
      walker.walk(cb, tree);
      _configuration = cb.getConfiguration();
    }
  }
}
