package org.batfish.coordinator;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.SnapshotMetadata;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.storage.StorageProvider;
import org.batfish.storage.TestStorageProvider;
import org.junit.Test;

/** Tests for {@link SnapshotMetadataMgr}. */
public class SnapshotMetadataMgrTest {
  private static final int WRITES = 10000;

  @Test
  public void testThreadSafety() throws IOException, ExecutionException, InterruptedException {
    StorageProvider storage =
        new TestStorageProvider() {
          private volatile String _meta;

          @Override
          public @Nonnull String loadSnapshotMetadata(NetworkId networkId, SnapshotId snapshotId) {
            return _meta;
          }

          @Override
          public void storeSnapshotMetadata(
              SnapshotMetadata snapshotMetadata, NetworkId networkId, SnapshotId snapshotId) {
            _meta = null;
            _meta = BatfishObjectMapper.writeStringRuntimeError(snapshotMetadata);
          }
        };

    SnapshotMetadataMgr manager = new SnapshotMetadataMgr(storage);
    final AtomicBoolean done = new AtomicBoolean(false);
    NetworkId net = new NetworkId("net");
    SnapshotId ss = new SnapshotId("ss");
    manager.writeMetadata(new SnapshotMetadata(Instant.now(), null), net, ss);

    ListeningExecutorService pollerService =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    ListenableFuture<Long> loops =
        pollerService.submit(
            () -> {
              long count = 0;
              Instant last = Instant.MIN;
              while (!done.get()) {
                ++count;
                SnapshotMetadata meta = manager.readMetadata(net, ss);
                assertThat(meta, notNullValue());
                Instant cur = meta.getCreationTimestamp();
                assertThat(cur, greaterThanOrEqualTo(last));
                last = cur;
              }
              return count;
            });
    for (int i = 0; i < WRITES; ++i) {
      manager.writeMetadata(new SnapshotMetadata(Instant.now(), null), net, ss);
    }
    done.set(true);
    assertThat(loops.get(), greaterThanOrEqualTo(1L));
    pollerService.awaitTermination(1, TimeUnit.SECONDS);
  }
}
