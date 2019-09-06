package org.batfish.specifier.parboiled;

import static org.batfish.datamodel.answers.InputValidationUtils.getErrorMessageEmptyNameRegex;
import static org.batfish.datamodel.answers.InputValidationUtils.getErrorMessageMissingBook;
import static org.batfish.datamodel.answers.InputValidationUtils.getErrorMessageMissingGroup;
import static org.batfish.datamodel.answers.InputValidationUtils.getErrorMessageMissingName;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import org.batfish.common.CompletionMetadata;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.referencelibrary.InterfaceGroup;
import org.batfish.referencelibrary.ReferenceBook;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.role.NodeRolesData;
import org.junit.Test;

/** Tests for {@link InterfaceNoMatchMessages} */
public class InterfaceNoMatchMessagesTest {

  private static List<String> getMessages(
      InterfaceNoMatchMessages interfaceNoMatchMessages, CompletionMetadata completionMetadata) {
    return interfaceNoMatchMessages.get(
        completionMetadata, NodeRolesData.builder().build(), new ReferenceLibrary(null));
  }

  private static List<String> getMessages(
      InterfaceNoMatchMessages interfaceNoMatchMessages, ReferenceLibrary referenceLibrary) {
    return interfaceNoMatchMessages.get(
        CompletionMetadata.builder().build(), NodeRolesData.builder().build(), referenceLibrary);
  }

  @Test
  public void testName() {
    CompletionMetadata completionMetadata =
        CompletionMetadata.builder()
            .setInterfaces(
                ImmutableSet.of(NodeInterfacePair.of("h1", "i1"), NodeInterfacePair.of("h2", "i2")))
            .build();
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new NameInterfaceAstNode("a")), completionMetadata),
        equalTo(ImmutableList.of(getErrorMessageMissingName("a", "interface"))));
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new NameInterfaceAstNode("i1")), completionMetadata),
        equalTo(ImmutableList.of()));
  }

  @Test
  public void testNameRegex() {
    CompletionMetadata completionMetadata =
        CompletionMetadata.builder()
            .setInterfaces(
                ImmutableSet.of(NodeInterfacePair.of("h1", "i1"), NodeInterfacePair.of("h2", "i2")))
            .build();
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new NameRegexInterfaceAstNode("a")), completionMetadata),
        equalTo(ImmutableList.of((getErrorMessageEmptyNameRegex("a", "interface")))));
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new NameRegexInterfaceAstNode("i")), completionMetadata),
        equalTo(ImmutableList.of()));
  }

  @Test
  public void testVrf() {
    CompletionMetadata completionMetadata =
        CompletionMetadata.builder().setVrfs(ImmutableSet.of("v1", "v2")).build();
    assertThat(
        getMessages(new InterfaceNoMatchMessages(new VrfInterfaceAstNode("a")), completionMetadata),
        equalTo(ImmutableList.of(getErrorMessageMissingName("a", "VRF"))));
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new VrfInterfaceAstNode("v1")), completionMetadata),
        equalTo(ImmutableList.of()));
  }

  @Test
  public void testZone() {
    CompletionMetadata completionMetadata =
        CompletionMetadata.builder().setZones(ImmutableSet.of("v1", "v2")).build();
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new ZoneInterfaceAstNode("a")), completionMetadata),
        equalTo(ImmutableList.of(getErrorMessageMissingName("a", "zone"))));
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new ZoneInterfaceAstNode("v1")), completionMetadata),
        equalTo(ImmutableList.of()));
  }

  @Test
  public void testInterfaceGroup() {
    ReferenceLibrary referenceLibrary =
        new ReferenceLibrary(
            ImmutableList.of(
                ReferenceBook.builder("book")
                    .setInterfaceGroups(
                        ImmutableList.of(new InterfaceGroup(ImmutableSortedSet.of(), "g1")))
                    .build()));
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new InterfaceGroupInterfaceAstNode("nobook", "g1")),
            referenceLibrary),
        equalTo(ImmutableList.of(getErrorMessageMissingBook("nobook", "Reference book"))));
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new InterfaceGroupInterfaceAstNode("book", "norole")),
            referenceLibrary),
        equalTo(
            ImmutableList.of(
                getErrorMessageMissingGroup(
                    "norole", "interface group", "book", "reference book"))));
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(new InterfaceGroupInterfaceAstNode("book", "g1")),
            referenceLibrary),
        equalTo(ImmutableList.of()));
  }

  @Test
  public void testWithNode() {
    CompletionMetadata completionMetadata =
        CompletionMetadata.builder()
            .setNodes(ImmutableSet.of("h1", "h2"))
            .setInterfaces(
                ImmutableSet.of(NodeInterfacePair.of("h1", "i1"), NodeInterfacePair.of("h2", "i2")))
            .build();
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(
                new InterfaceWithNodeInterfaceAstNode(
                    new NameNodeAstNode("no"), new NameInterfaceAstNode("no"))),
            completionMetadata),
        equalTo(
            ImmutableList.of(
                getErrorMessageMissingName("no", "device"),
                getErrorMessageMissingName("no", "interface"))));
    // no no_match messages since the logic is not context sensitive
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(
                new InterfaceWithNodeInterfaceAstNode(
                    new NameNodeAstNode("h1"), new NameInterfaceAstNode("i2"))),
            completionMetadata),
        equalTo(ImmutableList.of()));
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(
                new InterfaceWithNodeInterfaceAstNode(
                    new NameNodeAstNode("h1"), new NameInterfaceAstNode("i1"))),
            completionMetadata),
        equalTo(ImmutableList.of()));
  }

  @Test
  public void testSetOp() {
    CompletionMetadata completionMetadata =
        CompletionMetadata.builder()
            .setInterfaces(
                ImmutableSet.of(NodeInterfacePair.of("h1", "i1"), NodeInterfacePair.of("h2", "i2")))
            .build();
    List<String> expected =
        ImmutableList.of(
            getErrorMessageMissingName("a1", "interface"),
            getErrorMessageMissingName("a2", "interface"));
    // union
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(
                new UnionInterfaceAstNode(
                    new NameInterfaceAstNode("a1"), new NameInterfaceAstNode("a2"))),
            completionMetadata),
        equalTo(expected));
    // difference
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(
                new UnionInterfaceAstNode(
                    new NameInterfaceAstNode("a1"), new NameInterfaceAstNode("a2"))),
            completionMetadata),
        equalTo(expected));
    // intersection
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(
                new UnionInterfaceAstNode(
                    new NameInterfaceAstNode("a1"), new NameInterfaceAstNode("a2"))),
            completionMetadata),
        equalTo(expected));
  }

  /** a subset of the input is empty */
  @Test
  public void testEmptySubset() {
    CompletionMetadata completionMetadata =
        CompletionMetadata.builder()
            .setInterfaces(
                ImmutableSet.of(NodeInterfacePair.of("h1", "i1"), NodeInterfacePair.of("h2", "i2")))
            .build();
    assertThat(
        getMessages(
            new InterfaceNoMatchMessages(
                new UnionInterfaceAstNode(
                    new NameInterfaceAstNode("i1"), new NameInterfaceAstNode("b1"))),
            completionMetadata),
        equalTo(ImmutableList.of(getErrorMessageMissingName("b1", "interface"))));
  }
}
