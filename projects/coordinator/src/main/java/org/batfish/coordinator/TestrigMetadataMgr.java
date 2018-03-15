package org.batfish.coordinator;

// make sure that WorkQueueMgr is never called from this class directly or indirectly
// otherwise, we risk a deadlock, since WorkQueueMgr calls into this class
// currently, this invariant is ensured by never calling out anywhere

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.EnvironmentMetadata;
import org.batfish.datamodel.EnvironmentMetadata.ProcessingStatus;
import org.batfish.datamodel.TestrigMetadata;

public class TestrigMetadataMgr {

  public static EnvironmentMetadata getEnvironmentMetadata(
      String container, String testrig, String envName) throws IOException {
    TestrigMetadata trMetadata = readMetadata(container, testrig);
    return trMetadata.getEnvironments().get(envName);
  }

  public static Instant getTestrigCreationTimeOrMin(String container, String testrig) {
    try {
      return readMetadata(container, testrig).getCreationTimestamp();
    } catch (Exception e) {
      return Instant.MIN;
    }
  }

  public static synchronized void initializeEnvironment(
      String container, String testrig, String envName) throws IOException {
    Path metadataPath = WorkMgr.getpathTestrigMetadata(container, testrig);
    TestrigMetadata metadata = readMetadata(metadataPath);
    metadata.initializeEnvironment(envName);
    writeMetadata(metadata, metadataPath);
  }

  public static TestrigMetadata readMetadata(String container, String testrig) throws IOException {
    return readMetadata(WorkMgr.getpathTestrigMetadata(container, testrig));
  }

  public static TestrigMetadata readMetadata(Path metadataPath) throws IOException {
    String jsonStr = CommonUtil.readFile(metadataPath);
    return BatfishObjectMapper.mapper().readValue(jsonStr, TestrigMetadata.class);
  }

  public static void writeMetadata(TestrigMetadata metadata, String container, String testrig)
      throws JsonProcessingException {
    writeMetadata(metadata, WorkMgr.getpathTestrigMetadata(container, testrig));
  }

  public static synchronized void writeMetadata(TestrigMetadata metadata, Path metadataPath)
      throws JsonProcessingException {
    CommonUtil.writeFile(metadataPath, BatfishObjectMapper.writePrettyString(metadata));
  }

  public static synchronized void updateEnvironmentStatus(
      String container, String testrig, String envName, ProcessingStatus status, String errMessage)
      throws IOException {
    Path metadataPath = WorkMgr.getpathTestrigMetadata(container, testrig);
    TestrigMetadata trMetadata = readMetadata(metadataPath);
    EnvironmentMetadata environmentMetadata = trMetadata.getEnvironments().get(envName);
    environmentMetadata.updateStatus(status, errMessage);
    writeMetadata(trMetadata, metadataPath);
  }
}
