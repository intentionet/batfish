package org.batfish.grammar.flatjuniper;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.annotation.Nonnull;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SkipNode;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.common.StringBuilderSink;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;

/**
 * A class that converts a Juniper AS Path regex to a Java regex.
 *
 * @see <a
 *     href="https://www.juniper.net/documentation/en_US/junos/topics/usage-guidelines/policy-configuring-as-path-regular-expressions-to-use-as-routing-policy-match-conditions.html">Juniper
 *     docs</a>
 */
@BuildParseTree
@SuppressWarnings({
  "checkstyle:methodname", // this class uses idiomatic names
  "WeakerAccess", // access of Rule methods is needed for parser auto-generation.
})
public class AsPathRegex extends BaseParser<String> {

  Rule TopLevel() {
    return Sequence(Optional('^'), FirstOf(NullAsPath(), AsPath()), Optional('$'), EOI);
  }

  Rule NullAsPath() {
    return Sequence(String("()"), push("^$"));
  }

  Rule AsPath() {
    return Sequence(
        Term(), // first term, will be pop(1) below.
        ZeroOrMore(
            AtLeastOneSpace(),
            Term(), // pop()
            push(String.format("%s%s", pop(1), pop()))),
        push(String.format("^%s$", pop())));
  }

  Rule Term() {
    return FirstOf(
        BareAsnRange(), // NumberRange without [] around it cannot be followed by an operator
        BareOr(), // Or Without () around it cannot be followed by an operator
        Sequence(T_TopLevel(), Operator()));
  }

  Rule T_TopLevel() {
    return FirstOf(T_Dot(), AsnRange(), ASN(), T_Group(), T_Or());
  }

  @SkipNode
  Rule Operator() {
    return Optional(
        FirstOf(Op_Asterisk(), Op_Plus(), Op_QuestionMark(), Op_Exact(), Op_OrMore(), Op_Range()));
  }

  @SuppressSubnodes
  Rule Op_Asterisk() {
    return Sequence(Ch('*'), push(String.format("(%s)*", pop())));
  }

  @SuppressSubnodes
  Rule Op_Plus() {
    return Sequence(Ch('+'), push(String.format("(%s)+", pop())));
  }

  @SuppressSubnodes
  Rule Op_QuestionMark() {
    return Sequence(Ch('?'), push(String.format("(%s)?", pop())));
  }

  @SuppressSubnodes
  Rule Op_Exact() {
    return Sequence('{', Number(), '}');
  }

  @SuppressSubnodes
  Rule Op_Range() {
    return Sequence(
        '{',
        Number(),
        ',',
        Number(),
        '}',
        push(String.format("(%s){%s,%s}", pop(2), pop(1), pop())));
  }

  @SuppressSubnodes
  Rule Op_OrMore() {
    return Sequence('{', Number(), ',', '}', push(String.format("(%s){%s,}", pop(1), pop())));
  }

  @SuppressSubnodes
  Rule T_Dot() {
    return Sequence(Ch('.'), push(" \\d+"));
  }

  Rule T_Or() {
    return Sequence('(', BareOr(), ')');
  }

  Rule T_Group() {
    return Sequence(
        '(',
        IgnoreSpace(),
        Term(), // pop(1)
        ZeroOrMore(Sequence(AtLeastOneSpace(), Term(), push(String.format("%s%s", pop(1), pop())))),
        IgnoreSpace(),
        ')');
  }

  @SuppressSubnodes
  Rule Digit() {
    return CharRange('0', '9');
  }

  // Like Number(), but pushes the preceding space added in ExplicitAsPathSet when used as an ASN.
  @SuppressSubnodes
  Rule ASN() {
    return Sequence(Number(), push(' ' + pop()));
  }

  // A decimal number.
  @SuppressSubnodes
  Rule Number() {
    return Sequence(OneOrMore(Digit()), push(match()));
  }

  @SuppressSubnodes
  Rule BareAsnRange() {
    return Sequence(Sequence(ASN(), '-', ASN()), push(rangeToOr(pop(1), pop())));
  }

  Rule BareOr() {
    return Sequence(
        T_TopLevel(), // pop(1)
        OneOrMore(
            IgnoreSpace(),
            '|',
            IgnoreSpace(),
            T_TopLevel(), // pop()
            push(String.format("%s|%s", pop(1), pop()))),
        push("(" + pop() + ')'));
  }

  // BareAsnRange, with [] around it.
  Rule AsnRange() {
    return Sequence(
        Sequence('[', ASN(), '-', ASN(), ']'), // will push two numbers on stack
        push(rangeToOr(pop(1), pop())));
  }

  @SuppressNode
  Rule IgnoreSpace() {
    return ZeroOrMore(' ');
  }

  @SuppressNode
  Rule AtLeastOneSpace() {
    return OneOrMore(' ');
  }

  // Must be package-private to be accessible to generated parser.
  static String rangeToOr(String lowAsn, String highAsn) {
    Long start = Long.valueOf(lowAsn.substring(1)); // needed to remove leading space from ASN().
    Long end = Long.valueOf(highAsn.substring(1)); // needed to remove leading space from ASN().
    checkArgument(start <= end, "Invalid range %d-%d", start, end);
    String bigOr =
        LongStream.range(start, end + 1).mapToObj(l -> " " + l).collect(Collectors.joining("|"));
    return "(" + bigOr + ')';
  }

  /** Converts the given Juniper AS Path regular expression to a Java regular expression. */
  @Nonnull
  public static String convertToJavaRegex(String regex) {
    AsPathRegex parser = Parboiled.createParser(AsPathRegex.class);
    BasicParseRunner<String> runner = new BasicParseRunner<>(parser.TopLevel());
    ParsingResult<String> result = runner.run(regex);
    if (!result.matched) {
      throw new IllegalArgumentException("Unhandled input: " + regex);
    }
    return result.resultValue;
  }

  /** Like {@link #convertToJavaRegex(String)}, but for debugging. */
  @SuppressWarnings("unused") // leaving here for future debugging.
  @Nonnull
  static String debugConvertToJavaRegex(String regex) {
    AsPathRegex parser = Parboiled.createParser(AsPathRegex.class);
    TracingParseRunner<String> runner =
        new TracingParseRunner<String>(parser.TopLevel()).withLog(new StringBuilderSink());
    ParsingResult<String> result = runner.run(regex);
    if (!result.matched) {
      throw new IllegalArgumentException("Unhandled input: " + regex + "\n" + runner.getLog());
    }
    return result.resultValue;
  }
}
