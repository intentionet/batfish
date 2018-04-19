package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.vendor.StructureType;
import org.batfish.vendor.StructureUsage;
import org.hamcrest.Matcher;

public final class DataModelMatchers {

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IpSpaceReference}'s {@code name}.
   */
  public static @Nonnull Matcher<PermittedByAcl> hasAclName(
      @Nonnull Matcher<? super String> subMatcher) {
    return new PermittedByAclMatchers.HasAclName(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code name} is that of the {@link
   * IpSpaceReference}.
   */
  public static @Nonnull Matcher<PermittedByAcl> hasAclName(@Nonnull String name) {
    return hasAclName(equalTo(name));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IpSpaceReference}'s {@code name}.
   */
  public static @Nonnull Matcher<IpSpaceReference> hasName(
      @Nonnull Matcher<? super String> subMatcher) {
    return new IpSpaceReferenceMatchers.HasName(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code name} is that of the {@link
   * IpSpaceReference}.
   */
  public static @Nonnull Matcher<IpSpaceReference> hasName(@Nonnull String name) {
    return hasName(equalTo(name));
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has
   * an undefined refrence in {@code hostname} to a structure of type {@code type} named {@code
   * structureName}.
   */
  public static @Nonnull Matcher<ConvertConfigurationAnswerElement> hasUndefinedReference(
      @Nonnull String hostname, @Nonnull StructureType type, @Nonnull String structureName) {
    return new ConvertConfigurationAnswerElementMatchers.HasUndefinedReference(
        hostname, type, structureName);
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has
   * an undefined refrence in {@code hostname} to a structure of type {@code type} named {@code
   * structureName} of usage type {@code usage}.
   */
  public static @Nonnull Matcher<ConvertConfigurationAnswerElement> hasUndefinedReference(
      @Nonnull String hostname,
      @Nonnull StructureType type,
      @Nonnull String structureName,
      @Nonnull StructureUsage usage) {
    return new ConvertConfigurationAnswerElementMatchers.HasUndefinedReferenceWithUsage(
        hostname, type, structureName, usage);
  }

  /**
   * Provides a matcher that matches if the provided {@link ConvertConfigurationAnswerElement} has
   * an unused structure for {@code hostname} of type {@code type} named {@code structureName}.
   */
  public static @Nonnull Matcher<ConvertConfigurationAnswerElement> hasUnusedStructure(
      @Nonnull String hostname, @Nonnull StructureType type, @Nonnull String structureName) {
    return new ConvertConfigurationAnswerElementMatchers.HasUnusedStructure(
        hostname, type, structureName);
  }

  /**
   * Provides a matcher that matches if the object is an {@link IpSpaceReference} matched by the
   * provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<IpSpace> isIpSpaceReferenceThat(
      @Nonnull Matcher<? super IpSpaceReference> subMatcher) {
    return new IpSpaceReferenceMatchers.IsIpSpaceReferenceThat(subMatcher);
  }

  /**
   * Provides a matcher that matches if the object is a {@link PermittedByAcl} matched by the
   * provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<AclLineMatchExpr> isPermittedByAclThat(
      @Nonnull Matcher<? super PermittedByAcl> subMatcher) {
    return new PermittedByAclMatchers.IsPermittedByAclThat(subMatcher);
  }

  private DataModelMatchers() {}
}
