package org.batfish.question.definedstructures;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/** Fetches defined structures in config files. */
public class DefinedStructuresQuestion extends Question {
  /** A filter on filenames in which to return structures. */
  private static final String PROP_FILES = "files";
  /** A filter on nodes for which to return files defining structures. */
  private static final String PROP_NODES = "nodes";

  /** A filter on structure names. */
  private static final String PROP_NAMES = "names";

  /** A filter on vendor-specific structure types. */
  private static final String PROP_TYPES = "types";

  @Nonnull private final String _files;
  @Nullable private final String _nodes;
  @Nonnull private final String _types;
  @Nonnull private final String _names;

  @JsonCreator
  private static DefinedStructuresQuestion jsonCreator(
      @Nullable @JsonProperty(PROP_FILES) String files,
      @Nullable @JsonProperty(PROP_NODES) String nodes,
      @Nullable @JsonProperty(PROP_TYPES) String types,
      @Nullable @JsonProperty(PROP_NAMES) String names) {
    String actualFiles = Strings.isNullOrEmpty(files) ? ".*" : files;
    String actualNodes = Strings.isNullOrEmpty(nodes) ? null : nodes;
    String actualTypes = Strings.isNullOrEmpty(types) ? ".*" : types;
    String actualNames = Strings.isNullOrEmpty(names) ? ".*" : names;
    return new DefinedStructuresQuestion(actualFiles, actualNodes, actualTypes, actualNames);
  }

  public DefinedStructuresQuestion(
      @Nonnull String files, @Nullable String nodes, @Nonnull String types, @Nonnull String names) {
    _files = files;
    _nodes = nodes;
    _types = types;
    _names = names;
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "definedStructures";
  }

  @JsonProperty(PROP_FILES)
  @Nonnull
  public String getFiles() {
    return _files;
  }

  @JsonProperty(PROP_NAMES)
  @Nonnull
  public String getNames() {
    return _names;
  }

  @Nullable
  @JsonProperty(PROP_NODES)
  public String getNodes() {
    return _nodes;
  }

  @Nonnull
  @JsonIgnore
  NodeSpecifier getNodeSpecifier() {
    return SpecifierFactories.getNodeSpecifierOrDefault(_nodes, AllNodesNodeSpecifier.INSTANCE);
  }

  @JsonProperty(PROP_TYPES)
  @Nonnull
  public String getTypes() {
    return _types;
  }
}
