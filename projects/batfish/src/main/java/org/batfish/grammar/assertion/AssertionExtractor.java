package org.batfish.grammar.assertion;

import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.assertion.And;
import org.batfish.datamodel.assertion.AssertionAst;
import org.batfish.datamodel.assertion.BooleanExpr;
import org.batfish.datamodel.assertion.BooleanExprs;
import org.batfish.datamodel.assertion.BooleanIf;
import org.batfish.datamodel.assertion.ComparableExpr;
import org.batfish.datamodel.assertion.ComparableIf;
import org.batfish.datamodel.assertion.DoubleLiteral;
import org.batfish.datamodel.assertion.Eq;
import org.batfish.datamodel.assertion.FloatLiteral;
import org.batfish.datamodel.assertion.Ge;
import org.batfish.datamodel.assertion.Gt;
import org.batfish.datamodel.assertion.IntLiteral;
import org.batfish.datamodel.assertion.Le;
import org.batfish.datamodel.assertion.LongLiteral;
import org.batfish.datamodel.assertion.Lt;
import org.batfish.datamodel.assertion.Not;
import org.batfish.datamodel.assertion.Or;
import org.batfish.datamodel.assertion.PathSize;
import org.batfish.datamodel.assertion.StringExpr;
import org.batfish.datamodel.assertion.StringIf;
import org.batfish.datamodel.assertion.StringLiteral;
import org.batfish.grammar.assertion.AssertionParser.AndContext;
import org.batfish.grammar.assertion.AssertionParser.AssertionContext;
import org.batfish.grammar.assertion.AssertionParser.Boolean_exprContext;
import org.batfish.grammar.assertion.AssertionParser.Boolean_functionContext;
import org.batfish.grammar.assertion.AssertionParser.Boolean_ifContext;
import org.batfish.grammar.assertion.AssertionParser.EqContext;
import org.batfish.grammar.assertion.AssertionParser.GeContext;
import org.batfish.grammar.assertion.AssertionParser.GtContext;
import org.batfish.grammar.assertion.AssertionParser.LeContext;
import org.batfish.grammar.assertion.AssertionParser.LtContext;
import org.batfish.grammar.assertion.AssertionParser.NotContext;
import org.batfish.grammar.assertion.AssertionParser.Num_exprContext;
import org.batfish.grammar.assertion.AssertionParser.Num_functionContext;
import org.batfish.grammar.assertion.AssertionParser.Num_ifContext;
import org.batfish.grammar.assertion.AssertionParser.OrContext;
import org.batfish.grammar.assertion.AssertionParser.PathsizeContext;
import org.batfish.grammar.assertion.AssertionParser.Quoted_stringContext;
import org.batfish.grammar.assertion.AssertionParser.String_exprContext;
import org.batfish.grammar.assertion.AssertionParser.String_functionContext;
import org.batfish.grammar.assertion.AssertionParser.String_ifContext;

public class AssertionExtractor extends AssertionParserBaseListener {

  private AssertionAst _ast;

  private String _text;

  @SuppressWarnings("unused")
  private AssertionParser _parser;

  public AssertionExtractor(String fileText, AssertionParser parser) {
    _parser = parser;
    _text = fileText;
  }

  private BatfishException convError(Class<?> type, ParserRuleContext ctx) {
    String typeName = type.getSimpleName();
    String txt = getFullText(ctx);
    return new BatfishException("Could not convert to " + typeName + ": " + txt);
  }

  @Override
  public void exitAssertion(AssertionContext ctx) {
    _ast = toAst(ctx);
  }

  public AssertionAst getAst() {
    return _ast;
  }

  private String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    String text = _text.substring(start, end + 1);
    return text;
  }

  private String getText(Quoted_stringContext ctx) {
    if (ctx.text != null) {
      return ctx.text.getText();
    } else {
      return "";
    }
  }

  private AssertionAst toAst(AssertionContext ctx) {
    BooleanExpr expr = toBooleanExpr(ctx.boolean_expr());
    return new AssertionAst(expr);
  }

  private BooleanExpr toBooleanExpr(AndContext ctx) {
    List<BooleanExpr> conjuncts =
        ctx.conjuncts.stream().map(cctx -> toBooleanExpr(cctx)).collect(Collectors.toList());
    return new And(conjuncts);
  }

  private BooleanExpr toBooleanExpr(Boolean_exprContext ctx) {
    if (ctx.boolean_application() != null) {
      return toBooleanExpr(ctx.boolean_application().boolean_function());
    } else if (ctx.FALSE() != null) {
      return BooleanExprs.FALSE;
    } else {
      throw convError(BooleanExpr.class, ctx);
    }
  }

  private BooleanExpr toBooleanExpr(Boolean_functionContext ctx) {
    if (ctx.and() != null) {
      return toBooleanExpr(ctx.and());
    } else if (ctx.boolean_if() != null) {
      return toBooleanExpr(ctx.boolean_if());
    } else if (ctx.eq() != null) {
      return toBooleanExpr(ctx.eq());
    } else if (ctx.ge() != null) {
      return toBooleanExpr(ctx.ge());
    } else if (ctx.gt() != null) {
      return toBooleanExpr(ctx.gt());
    } else if (ctx.le() != null) {
      return toBooleanExpr(ctx.le());
    } else if (ctx.lt() != null) {
      return toBooleanExpr(ctx.lt());
    } else if (ctx.not() != null) {
      return toBooleanExpr(ctx.not());
    } else if (ctx.or() != null) {
      return toBooleanExpr(ctx.or());
    } else {
      throw convError(BooleanExpr.class, ctx);
    }
  }

  private BooleanExpr toBooleanExpr(Boolean_ifContext ctx) {
    BooleanExpr guard = toBooleanExpr(ctx.guard);
    BooleanExpr trueExpr = toBooleanExpr(ctx.trueval);
    BooleanExpr falseExpr = toBooleanExpr(ctx.falseval);
    return new BooleanIf(guard, trueExpr, falseExpr);
  }

  private BooleanExpr toBooleanExpr(EqContext ctx) {
    if (ctx.lhs_boolean != null) {
      BooleanExpr lhs = toBooleanExpr(ctx.lhs_boolean);
      BooleanExpr rhs = toBooleanExpr(ctx.rhs_boolean);
      return new Eq(lhs, rhs);
    } else if (ctx.lhs_num != null) {
      ComparableExpr lhs = toComparableExpr(ctx.lhs_num);
      ComparableExpr rhs = toComparableExpr(ctx.rhs_num);
      return new Eq(lhs, rhs);
    } else if (ctx.lhs_string != null) {
      StringExpr lhs = toStringExpr(ctx.lhs_string);
      StringExpr rhs = toStringExpr(ctx.rhs_string);
      return new Eq(lhs, rhs);
    } else {
      throw convError(BooleanExpr.class, ctx);
    }
  }

  private BooleanExpr toBooleanExpr(GeContext ctx) {
    ComparableExpr lhs = toComparableExpr(ctx.lhs);
    ComparableExpr rhs = toComparableExpr(ctx.lhs);
    return new Ge(lhs, rhs);
  }

  private BooleanExpr toBooleanExpr(GtContext ctx) {
    ComparableExpr lhs = toComparableExpr(ctx.lhs);
    ComparableExpr rhs = toComparableExpr(ctx.rhs);
    return new Gt(lhs, rhs);
  }

  private BooleanExpr toBooleanExpr(LeContext ctx) {
    ComparableExpr lhs = toComparableExpr(ctx.lhs);
    ComparableExpr rhs = toComparableExpr(ctx.rhs);
    return new Le(lhs, rhs);
  }

  private BooleanExpr toBooleanExpr(LtContext ctx) {
    ComparableExpr lhs = toComparableExpr(ctx.lhs);
    ComparableExpr rhs = toComparableExpr(ctx.rhs);
    return new Lt(lhs, rhs);
  }

  private BooleanExpr toBooleanExpr(NotContext ctx) {
    BooleanExpr expr = toBooleanExpr(ctx.boolean_expr());
    return new Not(expr);
  }

  private BooleanExpr toBooleanExpr(OrContext ctx) {
    List<BooleanExpr> disjuncts =
        ctx.disjuncts.stream().map(cctx -> toBooleanExpr(cctx)).collect(Collectors.toList());
    return new Or(disjuncts);
  }

  private ComparableExpr toComparableExpr(Num_exprContext ctx) {
    if (ctx.num_application() != null) {
      return toComparableExpr(ctx.num_application().num_function());
    } else if (ctx.num_double() != null) {
      String raw = ctx.num_double().DOUBLE().getText();
      String num = raw.substring(0, raw.length() - 1);
      double d = Double.parseDouble(num);
      return new DoubleLiteral(d);
    } else if (ctx.num_float() != null) {
      String raw = ctx.num_float().FLOAT().getText();
      String num = raw.substring(0, raw.length() - 1);
      float f = Float.parseFloat(num);
      return new FloatLiteral(f);
    } else if (ctx.num_int() != null) {
      String num = ctx.num_int().INT().getText();
      int i = Integer.parseInt(num);
      return new IntLiteral(i);
    } else if (ctx.num_long() != null) {
      String raw = ctx.num_long().LONG().getText();
      String num = raw.substring(0, raw.length() - 1);
      long l = Long.parseLong(num);
      return new LongLiteral(l);
    } else {
      throw convError(ComparableExpr.class, ctx);
    }
  }

  private ComparableExpr toComparableExpr(Num_functionContext ctx) {
    if (ctx.num_if() != null) {
      return toComparableExpr(ctx.num_if());
    } else if (ctx.pathsize() != null) {
      return toComparableExpr(ctx.pathsize());
    } else {
      throw convError(ComparableExpr.class, ctx);
    }
  }

  private ComparableExpr toComparableExpr(Num_ifContext ctx) {
    BooleanExpr guard = toBooleanExpr(ctx.guard);
    ComparableExpr trueExpr = toComparableExpr(ctx.trueval);
    ComparableExpr falseExpr = toComparableExpr(ctx.falseval);
    return new ComparableIf(guard, trueExpr, falseExpr);
  }

  private ComparableExpr toComparableExpr(PathsizeContext ctx) {
    StringExpr pathExpr = toStringExpr(ctx.string_expr());
    return new PathSize(pathExpr);
  }

  private StringExpr toStringExpr(Quoted_stringContext ctx) {
    String text = getText(ctx);
    return new StringLiteral(text);
  }

  private StringExpr toStringExpr(String_exprContext ctx) {
    if (ctx.string_application() != null) {
      return toStringExpr(ctx.string_application().string_function());
    } else if (ctx.quoted_string() != null) {
      return toStringExpr(ctx.quoted_string());
    } else {
      throw convError(StringExpr.class, ctx);
    }
  }

  private StringExpr toStringExpr(String_functionContext ctx) {
    if (ctx.string_if() != null) {
      return toStringExpr(ctx.string_if());
    } else {
      throw convError(StringExpr.class, ctx);
    }
  }

  private StringExpr toStringExpr(String_ifContext ctx) {
    BooleanExpr guard = toBooleanExpr(ctx.guard);
    StringExpr trueExpr = toStringExpr(ctx.trueval);
    StringExpr falseExpr = toStringExpr(ctx.falseval);
    return new StringIf(guard, trueExpr, falseExpr);
  }
}
