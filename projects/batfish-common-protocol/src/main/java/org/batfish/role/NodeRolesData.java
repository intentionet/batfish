package org.batfish.role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.role.NodeRoleDimension.Type;

/** Class that captures the node roles */
public class NodeRolesData {

  private static final String PROP_DEFAULT_DIMENSION = "defaultDimension";
  private static final String PROP_LAST_MODIFIED_TIME = "lastModifiedTime";
  private static final String PROP_ROLE_DIMENSIONS = "roleDimensions";

  @Nullable private String _defaultDimension;

  @Nullable private Instant _lastModifiedTime;

  @Nonnull private SortedSet<NodeRoleDimension> _roleDimensions;

  @JsonCreator
  public NodeRolesData(
      @JsonProperty(PROP_DEFAULT_DIMENSION) String defaultDimension,
      @JsonProperty(PROP_LAST_MODIFIED_TIME) Instant lastModifiedTime,
      @JsonProperty(PROP_ROLE_DIMENSIONS) SortedSet<NodeRoleDimension> roleDimensions) {
    _defaultDimension = defaultDimension;
    _lastModifiedTime = lastModifiedTime;
    _roleDimensions = roleDimensions == null ? new TreeSet<>() : roleDimensions;
  }

  @JsonProperty(PROP_DEFAULT_DIMENSION)
  public String getDefaultDimension() {
    return _defaultDimension;
  }

  @JsonProperty(PROP_LAST_MODIFIED_TIME)
  public Instant getLastModifiedTime() {
    return _lastModifiedTime;
  }

  private Optional<NodeRoleDimension> getNodeRoleDimension(String dimension) {
    return _roleDimensions.stream().filter(d -> d.getName().equals(dimension)).findFirst();
  }

  /**
   * Get the {@link NodeRoleDimension} object for the specified dimension. If dimension is null,
   * returns {@link #getNodeRoleDimension(Path)}.
   *
   * @param dataPath Path from where to read {@link NodeRolesData}
   * @param dimension The name of the dimension to fetch
   * @return The {@link NodeRoleDimension} object if one exists or throws {@link
   *     java.util.NoSuchElementException} if {@code dimension} is non-null and not found.
   * @throws IOException If the contents of the file could not be cast to {@link NodeRolesData}
   */
  public static Optional<NodeRoleDimension> getNodeRoleDimension(Path dataPath, String dimension)
      throws IOException {
    if (dimension == null) {
      return getNodeRoleDimension(dataPath);
    }
    NodeRolesData data = read(dataPath);
    return data.getNodeRoleDimension(dimension);
  }

  /**
   * Get some "reasonable" {@link NodeRoleDimension} object for analysis. Preference order: the
   * default dimension if set and exists, the auto-inferred primary dimension if it exists, the
   * dimension that is lexicographically first, and null if no dimensions exist.
   *
   * @param dataPath Path from where to read {@link NodeRolesData}
   * @throws IOException If the contents of the file could not be cast to {@link NodeRolesData}
   */
  @Nullable
  private static Optional<NodeRoleDimension> getNodeRoleDimension(Path dataPath)
      throws IOException {
    NodeRolesData data = read(dataPath);
    // check default
    if (data.getDefaultDimension() != null) {
      Optional<NodeRoleDimension> opt = data.getNodeRoleDimension(data.getDefaultDimension());
      if (opt.isPresent()) {
        return opt;
      }
    }
    // check auto primary
    Optional<NodeRoleDimension> optAuto =
        data.getNodeRoleDimension(NodeRoleDimension.AUTO_DIMENSION_PRIMARY);
    if (optAuto.isPresent()) {
      return optAuto;
    }
    // check first
    return data.getNodeRoleDimensions()
        .stream()
        .sorted(Comparator.comparing(d -> d.getName()))
        .findFirst();
  }

  @JsonProperty(PROP_ROLE_DIMENSIONS)
  public SortedSet<NodeRoleDimension> getNodeRoleDimensions() {
    return _roleDimensions;
  }

  /**
   * Merge the dimensions in current file with new data. If the same dimension is present in both
   * data sources, the new data wins. If defaultDimension is non-null, it is deemed as the default
   * dimension. Optionally, delete all dimensions of type AUTO before adding new data.
   *
   * @param dataPath Location of the old data
   * @param newDimensions The new role data. Null values are treated as if the map were empty.
   * @param defaultDimension Dimension to deem as default after merger
   * @param deleteAutoFirst If dimensions of type AUTO should be deleted first
   */
  public static synchronized void mergeNodeRoleDimensions(
      Path dataPath,
      SortedSet<NodeRoleDimension> newDimensions,
      String defaultDimension,
      boolean deleteAutoFirst)
      throws IOException {

    NodeRolesData oldRolesData = read(dataPath);

    final SortedSet<NodeRoleDimension> finalNewDimensions =
        newDimensions == null ? new TreeSet<>() : newDimensions;

    SortedSet<NodeRoleDimension> newRoles = new TreeSet<>();

    // add the old role dimensions that are not in common with new dimensions
    newRoles.addAll(
        oldRolesData
            ._roleDimensions
            .stream()
            .filter(d -> !finalNewDimensions.contains(d))
            .collect(Collectors.toSet()));

    // delete the auto dimensions if needed
    if (deleteAutoFirst) {
      newRoles.removeIf(d -> d.getType() == Type.AUTO);
    }

    // add the new dimensions
    newRoles.addAll(finalNewDimensions);

    write(
        new NodeRolesData(
            defaultDimension == null ? oldRolesData.getDefaultDimension() : defaultDimension,
            new Date().toInstant(),
            newRoles),
        dataPath);
  }

  /**
   * Reads the {@link NodeRolesData} object from the provided Path. If the path does not exist,
   * initializes a new object.
   *
   * @param dataPath The Path to read from
   * @return The read data
   * @throws IOException If file exists but its contents could not be cast to {@link NodeRolesData}
   */
  public static NodeRolesData read(Path dataPath) throws IOException {
    if (Files.exists(dataPath)) {
      String jsonStr = CommonUtil.readFile(dataPath);
      return BatfishObjectMapper.mapper().readValue(jsonStr, NodeRolesData.class);
    } else {
      return new NodeRolesData(null, new Date().toInstant(), null);
    }
  }

  public static synchronized void write(NodeRolesData data, Path dataPath)
      throws JsonProcessingException {
    CommonUtil.writeFile(dataPath, BatfishObjectMapper.writePrettyString(data));
  }
}
