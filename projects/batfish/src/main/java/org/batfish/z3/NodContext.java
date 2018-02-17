package org.batfish.z3;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.FuncDecl;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import org.batfish.z3.expr.visitors.RelationCollector;
import org.batfish.z3.expr.visitors.VariableSizeCollector;
import org.batfish.z3.state.visitors.FuncDeclTransformer;

public class NodContext {

  private Context _context;

  private final Map<String, FuncDecl> _relationDeclarations;

  private final Map<String, BitVecExpr> _variables;

  private final Map<String, BitVecExpr> _variablesAsConsts;

  public NodContext(Context ctx, ReachabilityProgram... programs) {
    _context = ctx;
    AtomicInteger deBruijnIndex = new AtomicInteger(0);
    Map<String, Integer> variableSizes =
        Arrays.stream(programs)
            .flatMap(
                program ->
                    Streams.concat(program.getRules().stream(), program.getQueries().stream())
                        .map(
                            statement ->
                                VariableSizeCollector.collectVariableSizes(
                                    program.getInput(), statement))
                        .map(Map::entrySet)
                        .flatMap(Collection::stream))
            .collect(ImmutableSet.toImmutableSet())
            .stream()
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    _variables =
        variableSizes
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey,
                    variableSizeEntry ->
                        (BitVecExpr)
                            ctx.mkBound(
                                deBruijnIndex.getAndIncrement(),
                                ctx.mkBitVecSort(variableSizeEntry.getValue()))));
    _variablesAsConsts =
        variableSizes
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey,
                    variableSizeEntry ->
                        ctx.mkBVConst(variableSizeEntry.getKey(), variableSizeEntry.getValue())));
    FuncDeclTransformer funcDeclTransformer = new FuncDeclTransformer(ctx);
    _relationDeclarations =
        Arrays.stream(programs)
            .flatMap(
                program ->
                    Streams.concat(program.getRules().stream(), program.getQueries().stream())
                        .map(
                            statement ->
                                RelationCollector.collectRelations(program.getInput(), statement))
                        .map(Map::entrySet)
                        .flatMap(Collection::stream))
            .collect(ImmutableSet.toImmutableSet())
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey,
                    relationsEntry ->
                        funcDeclTransformer.toFuncDecl(
                            relationsEntry.getKey(), relationsEntry.getValue())));
  }

  public Context getContext() {
    return _context;
  }

  public Map<String, FuncDecl> getRelationDeclarations() {
    return _relationDeclarations;
  }

  public Map<String, BitVecExpr> getVariables() {
    return _variables;
  }

  public Map<String, BitVecExpr> getVariablesAsConsts() {
    return _variablesAsConsts;
  }
}
