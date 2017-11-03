package org.batfish.grammar.iptables;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.Prefix;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.iptables.IptablesParser.Built_in_targetContext;
import org.batfish.grammar.iptables.IptablesParser.CommandContext;
import org.batfish.grammar.iptables.IptablesParser.Command_tailContext;
import org.batfish.grammar.iptables.IptablesParser.Declaration_chain_policyContext;
import org.batfish.grammar.iptables.IptablesParser.Declaration_tableContext;
import org.batfish.grammar.iptables.IptablesParser.EndpointContext;
import org.batfish.grammar.iptables.IptablesParser.Iptables_configurationContext;
import org.batfish.grammar.iptables.IptablesParser.MatchContext;
import org.batfish.grammar.iptables.IptablesParser.ProtocolContext;
import org.batfish.grammar.iptables.IptablesParser.Rule_specContext;
import org.batfish.representation.iptables.IptablesChain.ChainPolicy;
import org.batfish.representation.iptables.IptablesConfiguration;
import org.batfish.representation.iptables.IptablesMatch.MatchType;
import org.batfish.representation.iptables.IptablesRule;
import org.batfish.representation.iptables.IptablesRule.IptablesActionType;
import org.batfish.representation.iptables.IptablesVendorConfiguration;
import org.batfish.vendor.VendorConfiguration;

public class IptablesControlPlaneExtractor extends IptablesParserBaseListener
    implements ControlPlaneExtractor {

  public static int toInteger(Token t) {
    return Integer.parseInt(t.getText());
  }

  private IptablesConfiguration _configuration;

  private String _fileName;

  private boolean _formatIptablesSave = false;

  private IptablesCombinedParser _parser;

  private String _tableCurrent;

  private String _text;

  private final Set<String> _unimplementedFeatures;

  private IptablesVendorConfiguration _vendorConfiguration;

  private Warnings _w;

  public IptablesControlPlaneExtractor(
      String fileText, IptablesCombinedParser iptablesParser, Warnings warnings, String fileName) {
    _text = fileText;
    _parser = iptablesParser;
    _w = warnings;
    _unimplementedFeatures = new TreeSet<>();
    _fileName = fileName;
  }

  @Override
  public void enterIptables_configuration(Iptables_configurationContext ctx) {
    _vendorConfiguration = new IptablesVendorConfiguration();
    _configuration = _vendorConfiguration;
    _vendorConfiguration.setHostname(_fileName);
  }

  @Override
  public void exitCommand(CommandContext ctx) {

    // default table if not specified in the command
    String table = (_formatIptablesSave) ? _tableCurrent : "filter";

    if (ctx.table() != null) {
      table = ctx.table().getText();
    }

    Command_tailContext tailCtx = ctx.command_tail();

    if (tailCtx.command_append() != null) {
      String chain = tailCtx.command_append().chain().getText();
      IptablesRule rule = extractRule(tailCtx.command_append().rule_spec());
      _configuration.addRule(table, chain, rule, -1);
    } else if (tailCtx.command_check() != null) {
      todo(tailCtx.command_check(), "Command Check");
    } else if (tailCtx.command_delete() != null) {
      todo(tailCtx.command_delete(), "Command Delete");
    } else if (tailCtx.command_delete_chain() != null) {
      todo(tailCtx.command_delete_chain(), "Command Delete Chain");
    } else if (tailCtx.command_flush() != null) {
      todo(tailCtx.command_flush(), "Command Flush");
    } else if (tailCtx.command_help() != null) {
      todo(tailCtx.command_help(), "Command Help");
    } else if (tailCtx.command_insert() != null) {
      String chain = tailCtx.command_append().chain().getText();
      int ruleNum = 1;
      if (tailCtx.command_insert().rulenum != null) {
        ruleNum = toInteger(tailCtx.command_insert().rulenum);
      }
      IptablesRule rule = extractRule(tailCtx.command_insert().rule_spec());
      _configuration.addRule(table, chain, rule, ruleNum);
    } else if (tailCtx.command_list() != null) {
      todo(tailCtx.command_list(), "Command List");
    } else if (tailCtx.command_list_rules() != null) {
      todo(tailCtx.command_list_rules(), "Command List Rules");
    } else if (tailCtx.command_new_chain() != null) {
      String chain = tailCtx.command_new_chain().chain().getText();
      _configuration.addChain(table, chain);
    } else if (tailCtx.command_policy() != null) {
      String chain = tailCtx.command_policy().chain().getText();
      ChainPolicy policy = getBuiltInTarget(tailCtx.command_policy().built_in_target());
      _configuration.setChainPolicy(table, chain, policy);
    } else if (tailCtx.command_rename_chain() != null) {
      todo(tailCtx.command_rename_chain(), "Command Rename Chain");
    } else if (tailCtx.command_replace() != null) {
      todo(tailCtx.command_replace(), "Command Replace");
    } else if (tailCtx.command_zero() != null) {
      todo(tailCtx.command_zero(), "Command Zero");
    } else {
      todo(tailCtx, "Unknown command");
    }
  }

  @Override
  public void exitDeclaration_chain_policy(Declaration_chain_policyContext ctx) {
    String chain = ctx.chain().getText();
    ChainPolicy policy = getBuiltInTarget(ctx.built_in_target());
    _configuration.setChainPolicy(_tableCurrent, chain, policy);
  }

  @Override
  public void exitDeclaration_table(Declaration_tableContext ctx) {
    _formatIptablesSave = true;
    _tableCurrent = ctx.table().getText();
  }

  private IptablesRule extractRule(Rule_specContext ctx) {
    IptablesRule rule = new IptablesRule();

    List<MatchContext> matches = ctx.match_list;

    for (MatchContext mCtx : matches) {

      boolean inverted = (mCtx.NOT() != null);

      if (mCtx.OPTION_IPV4() != null || mCtx.OPTION_IPV6() != null) {
        todo(mCtx, "ipv4 (--4) and ipv6 (--6) options");
      } else if (mCtx.OPTION_DESTINATION() != null) {
        rule.addMatch(inverted, MatchType.DESTINATION, getEndpoint(mCtx.endpoint()));
      } else if (mCtx.OPTION_DESTINATION_PORT() != null) {
        rule.addMatch(inverted, MatchType.DESTINATION_PORT, toInteger(mCtx.port));
      } else if (mCtx.OPTION_IN_INTERFACE() != null) {
        rule.addMatch(inverted, MatchType.IN_INTERFACE, mCtx.interface_name.getText());
      } else if (mCtx.OPTION_PROTOCOL() != null) {
        rule.addMatch(inverted, MatchType.PROTOCOL, toProtocol(mCtx.protocol()));
      } else if (mCtx.OPTION_OUT_INTERFACE() != null) {
        rule.addMatch(inverted, MatchType.OUT_INTERFACE, mCtx.interface_name.getText());
      } else if (mCtx.OPTION_SOURCE() != null) {
        rule.addMatch(inverted, MatchType.SOURCE, getEndpoint(mCtx.endpoint()));
      } else if (mCtx.OPTION_SOURCE_PORT() != null) {
        rule.addMatch(inverted, MatchType.SOURCE_PORT, toInteger(mCtx.port));
      } else {
        todo(mCtx, "Unknown match option");
      }
    }

    if (ctx.action().OPTION_JUMP() != null) {
      if (ctx.action().built_in_target() != null) {
        ChainPolicy policy = getBuiltInTarget(ctx.action().built_in_target());
        rule.setAction(policy);
      } else if (ctx.action().chain() != null) {
        rule.setAction(IptablesActionType.CHAIN, ctx.action().chain().getText());
      }
    } else if (ctx.action().OPTION_GOTO() != null) {
      rule.setAction(IptablesActionType.GOTO, ctx.action().chain().getText());
    } else {
      todo(ctx, "Unknown rule action");
    }
    rule.setName(getFullText(ctx));
    return rule;
  }

  private String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    String text = _text.substring(start, end + 1);
    return text;
  }

  @Nullable
  private ChainPolicy getBuiltInTarget(Built_in_targetContext ctx) {
    if (ctx.ACCEPT() != null) {
      return ChainPolicy.ACCEPT;
    } else if (ctx.DROP() != null) {
      return ChainPolicy.DROP;
    } else if (ctx.RETURN() != null) {
      return ChainPolicy.RETURN;
    } else {
      todo(ctx, "Chain policy (built in target)");
    }
    return null;
  }

  @Nullable
  private Object getEndpoint(EndpointContext endpoint) {
    if (endpoint.IP_ADDRESS() != null) {
      return new Ip(endpoint.IP_ADDRESS().getText());
    } else if (endpoint.IP_PREFIX() != null) {
      return new Prefix(endpoint.IP_PREFIX().getText());
    } else if (endpoint.IPV6_ADDRESS() != null) {
      // return new Ip6(endpoint.IPV6_ADDRESS().getText());
      todo(endpoint, "IPV6 address as endpoint");
    } else if (endpoint.IPV6_PREFIX() != null) {
      // return new Prefix6(endpoint.IPV6_PREFIX().getText());
      todo(endpoint, "IPV6 prefix as endpoint");
    } else if (endpoint.name != null) {
      // return endpoint.name.getText();
      todo(endpoint, "hostname as endpoint");
    } else {
      todo(endpoint, "Unknown endpoint");
    }
    return null;
  }

  @Override
  public Set<String> getUnimplementedFeatures() {
    return _unimplementedFeatures;
  }

  @Override
  public VendorConfiguration getVendorConfiguration() {
    return _vendorConfiguration;
  }

  @Override
  public void processParseTree(ParserRuleContext tree) {
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);
  }

  private void todo(ParserRuleContext ctx, String feature) {
    _w.todo(ctx, feature, _parser, _text);
    _w.unimplemented("Iptables : " + feature);
    _unimplementedFeatures.add("Iptables: " + feature);
  }

  private IpProtocol toProtocol(ProtocolContext protocol) {
    return IpProtocol.fromString(protocol.getText());
  }
}
