package org.batfish.common;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

public class CompletionMetadataTest {

  @Test
  public void testJavaSerialization() {
    CompletionMetadata completionMetadata =
        CompletionMetadata.builder()
            .setFilterNames(ImmutableSet.of("filter"))
            .setInterfaces(ImmutableSet.of(NodeInterfacePair.of("node", "interface")))
            .setIps(ImmutableSet.of("ip"))
            .setMlagIds(ImmutableSet.of("mlag"))
            .setNodes(ImmutableSet.of("node"))
            .setPrefixes(ImmutableSet.of("prefix"))
            .setStructureNames(ImmutableSet.of("structure"))
            .setVrfs(ImmutableSet.of("vrf"))
            .setZones(ImmutableSet.of("zone"))
            .build();

    assertThat(SerializationUtils.clone(completionMetadata), equalTo(completionMetadata));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    CompletionMetadata completionMetadata =
        CompletionMetadata.builder()
            .setFilterNames(ImmutableSet.of("filter"))
            .setInterfaces(ImmutableSet.of(NodeInterfacePair.of("node", "interface")))
            .setIps(ImmutableSet.of("ip"))
            .setMlagIds(ImmutableSet.of("mlag"))
            .setNodes(ImmutableSet.of("node"))
            .setPrefixes(ImmutableSet.of("prefix"))
            .setStructureNames(ImmutableSet.of("structure"))
            .setVrfs(ImmutableSet.of("vrf"))
            .setZones(ImmutableSet.of("zone"))
            .build();

    assertThat(
        BatfishObjectMapper.clone(completionMetadata, CompletionMetadata.class),
        equalTo(completionMetadata));
  }

  @Test
  public void testEquals() {
    CompletionMetadata.Builder builder = CompletionMetadata.builder();
    CompletionMetadata initial = builder.build();

    new EqualsTester()
        .addEqualityGroup(new Object())
        .addEqualityGroup(initial, initial, builder.build(), CompletionMetadata.EMPTY)
        .addEqualityGroup(builder.setFilterNames(ImmutableSet.of("filter")).build())
        .addEqualityGroup(
            builder
                .setInterfaces(ImmutableSet.of(NodeInterfacePair.of("node", "interface")))
                .build())
        .addEqualityGroup(builder.setIps(ImmutableSet.of("ip")).build())
        .addEqualityGroup(builder.setMlagIds(ImmutableSet.of("mlag")).build())
        .addEqualityGroup(builder.setNodes(ImmutableSet.of("node")).build())
        .addEqualityGroup(builder.setPrefixes(ImmutableSet.of("prefix")).build())
        .addEqualityGroup(builder.setStructureNames(ImmutableSet.of("structure")).build())
        .addEqualityGroup(builder.setVrfs(ImmutableSet.of("vrf")).build())
        .addEqualityGroup(builder.setZones(ImmutableSet.of("zone")).build())
        .testEquals();
  }
}
