package org.batfish.coordinator;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.batfish.common.BatfishException;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AnalysisMetadata;

public class AnalysisMetadataMgr {
  /** Analyses that can be requested for listing */
  public enum AnalysisType {
    /** Selects the suggested analyses */
    SUGGESTED,

    /** Selects the user analyses */
    USER,

    /** Selects both suggested and user analyses */
    ALL
  }

  public static Instant getAnalysisCreationTimeOrMin(String container, String analysis) {
    try {
      return readMetadata(container, analysis).getCreationTimestamp();
    } catch (IOException e) {
      return Instant.MIN;
    }
  }

  /**
   * Returns suggested property of given analysis's metadata, or false if no metadata exists.
   *
   * @param container Container in which to find analysis
   * @param analysis Analysis whose suggested property to return
   * @return suggested property of given analysis's metadata, or false if no metadata exists
   */
  public static boolean getAnalysisSuggestedOrFalse(String container, String analysis) {
    // If metadata file doesn't exist, assume analysis is not suggested
    if (!Files.exists(WorkMgr.getpathAnalysisMetadata(container, analysis))) {
      return false;
    }
    try {
      return readMetadata(container, analysis).getSuggested();
    } catch (IOException e) {
      throw new BatfishException("Unable to read metadata for analysis '" + analysis + "'", e);
    }
  }

  public static AnalysisMetadata readMetadata(String container, String analysis)
      throws IOException {
    return readMetadata(WorkMgr.getpathAnalysisMetadata(container, analysis));
  }

  public static AnalysisMetadata readMetadata(Path metadataPath) throws IOException {
    String jsonStr = CommonUtil.readFile(metadataPath);
    return BatfishObjectMapper.mapper().readValue(jsonStr, AnalysisMetadata.class);
  }

  public static void writeMetadata(AnalysisMetadata metadata, String container, String analysis)
      throws JsonProcessingException {
    writeMetadata(metadata, WorkMgr.getpathAnalysisMetadata(container, analysis));
  }

  public static synchronized void writeMetadata(AnalysisMetadata metadata, Path metadataPath)
      throws JsonProcessingException {
    CommonUtil.writeFile(metadataPath, BatfishObjectMapper.writePrettyString(metadata));
  }
}
