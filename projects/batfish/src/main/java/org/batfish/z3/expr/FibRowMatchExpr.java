package org.batfish.z3.expr;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.collections.FibRow;
import org.batfish.z3.BasicHeaderField;
import org.batfish.z3.state.PostInVrf;
import org.batfish.z3.state.PreOut;

public class FibRowMatchExpr {

  public static BooleanExpr getFibRowConditions(
      String hostname, String vrfName, List<FibRow> fib, int i, FibRow currentRow) {
    Set<FibRow> notRows = new TreeSet<>();
    for (int j = i + 1; j < fib.size(); j++) {
      FibRow specificRow = fib.get(j);
      long currentStart = currentRow.getPrefix().getStartIp().asLong();
      long currentEnd = currentRow.getPrefix().getEndIp().asLong();
      long specificStart = specificRow.getPrefix().getStartIp().asLong();
      long specificEnd = specificRow.getPrefix().getEndIp().asLong();
      // check whether later prefix is contained in this one
      if (currentStart <= specificStart && specificEnd <= currentEnd) {
        if (currentStart == specificStart && currentEnd == specificEnd) {
          // load balancing
          continue;
        }
        if (currentRow.getInterface().equals(specificRow.getInterface())
            && currentRow.getNextHop().equals(specificRow.getNextHop())
            && currentRow.getNextHopInterface().equals(specificRow.getNextHopInterface())) {
          // no need to exclude packets matching the more specific
          // prefix,
          // since they would go out same edge
          continue;
        }
        // exclude packets that match a more specific prefix that
        // would go out a different interface
        notRows.add(specificRow);
      } else {
        break;
      }
    }
    ImmutableList.Builder<BooleanExpr> conditionsBuilder = ImmutableList.builder();
    BooleanExpr postInVrf = new PostInVrf(hostname, vrfName);
    conditionsBuilder.add(postInVrf);
    BooleanExpr destRoute = new PreOut(hostname);
    conditionsBuilder.add(destRoute);

    // must not match more specific routes
    for (FibRow notRow : notRows) {
      int prefixLength = notRow.getPrefix().getPrefixLength();
      long prefix = notRow.getPrefix().getStartIp().asLong();
      int first = Prefix.MAX_PREFIX_LENGTH - prefixLength;
      if (first >= Prefix.MAX_PREFIX_LENGTH) {
        continue;
      }
      int last = Prefix.MAX_PREFIX_LENGTH - 1;
      LitIntExpr prefixFragmentLit = new LitIntExpr(prefix, first, last);
      IntExpr prefixFragmentExt = ExtractExpr.newExtractExpr(BasicHeaderField.DST_IP, first, last);
      EqExpr prefixMatch = new EqExpr(prefixFragmentExt, prefixFragmentLit);
      NotExpr noPrefixMatch = new NotExpr(prefixMatch);
      conditionsBuilder.add(noPrefixMatch);
    }

    // must match route
    int prefixLength = currentRow.getPrefix().getPrefixLength();
    long prefix = currentRow.getPrefix().getStartIp().asLong();
    int first = Prefix.MAX_PREFIX_LENGTH - prefixLength;
    if (first < Prefix.MAX_PREFIX_LENGTH) {
      int last = Prefix.MAX_PREFIX_LENGTH - 1;
      LitIntExpr prefixFragmentLit = new LitIntExpr(prefix, first, last);
      IntExpr prefixFragmentExt = ExtractExpr.newExtractExpr(BasicHeaderField.DST_IP, first, last);
      EqExpr prefixMatch = new EqExpr(prefixFragmentExt, prefixFragmentLit);
      conditionsBuilder.add(prefixMatch);
    }
    BooleanExpr conditions = new AndExpr(conditionsBuilder.build());
    return conditions;
  }
}
