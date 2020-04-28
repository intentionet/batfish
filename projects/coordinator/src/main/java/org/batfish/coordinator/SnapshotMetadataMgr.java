package org.batfish.coordinator;

// make sure that WorkQueueMgr is never called from this class directly or indirectly
// otherwise, we risk a deadlock, since WorkQueueMgr calls into this class
// currently, this invariant is ensured by never calling out anywhere

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.GuardedBy;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.InitializationMetadata;
import org.batfish.datamodel.InitializationMetadata.ProcessingStatus;
import org.batfish.datamodel.SnapshotMetadata;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.storage.StorageProvider;

@ParametersAreNonnullByDefault
public final class SnapshotMetadataMgr {

  public InitializationMetadata getInitializationMetadata(
      NetworkId networkId, SnapshotId snapshotId) throws IOException {
    SnapshotMetadata trMetadata = readMetadata(networkId, snapshotId);
    return trMetadata.getInitializationMetadata();
  }

  public Instant getSnapshotCreationTimeOrMin(NetworkId networkId, SnapshotId snapshotId) {
    try {
      return readMetadata(networkId, snapshotId).getCreationTimestamp();
    } catch (Exception e) {
      return Instant.MIN;
    }
  }

  public synchronized SnapshotMetadata readMetadata(NetworkId networkId, SnapshotId snapshotId)
      throws IOException {
    String cacheKey = getCacheKey(networkId, snapshotId);
    SnapshotMetadata ret = CACHE.getIfPresent(cacheKey);
    if (ret != null) {
      return ret;
    }
    ret =
        BatfishObjectMapper.mapper()
            .readValue(
                _storage.loadSnapshotMetadata(networkId, snapshotId), SnapshotMetadata.class);
    CACHE.put(cacheKey, ret);
    return ret;
  }

  public void updateInitializationStatus(
      NetworkId networkId,
      SnapshotId snapshotId,
      ProcessingStatus status,
      @Nullable String errMessage)
      throws IOException {
    writeMetadata(
        readMetadata(networkId, snapshotId).updateStatus(status, errMessage),
        networkId,
        snapshotId);
  }

  public synchronized void writeMetadata(
      SnapshotMetadata metadata, NetworkId networkId, SnapshotId snapshotId) throws IOException {
    CACHE.put(getCacheKey(networkId, snapshotId), metadata);
    _storage.storeSnapshotMetadata(metadata, networkId, snapshotId);
  }

  public SnapshotMetadataMgr(StorageProvider storage) {
    _storage = storage;
  }

  private static String getCacheKey(NetworkId networkId, SnapshotId snapshotId) {
    return String.format("%s-%s", networkId.getId(), snapshotId.getId());
  }

  @GuardedBy("this") // keep the cache and storage up to date.
  private @Nonnull StorageProvider _storage;

  // Somewhat arbitrary, but SnapshotMetadata is small so we can pick a lot. This makes paging out
  // an actively used snapshot relatively infrequent.
  private static final int CACHE_SIZE = 16;

  @GuardedBy("this") // keep the cache and storage up to date.
  private final Cache<String, SnapshotMetadata> CACHE =
      CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();
}
