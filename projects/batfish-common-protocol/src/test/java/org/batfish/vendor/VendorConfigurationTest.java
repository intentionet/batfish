package org.batfish.vendor;

import static org.batfish.common.matchers.WarningMatchers.hasText;
import static org.batfish.common.matchers.WarningsMatchers.hasRedFlag;
import static org.batfish.common.matchers.WarningsMatchers.hasRedFlags;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructureWithDefinitionLines;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasReferencedStructure;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.batfish.common.VendorConversionException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.junit.Test;

/** Test for {@link VendorConfiguration}. */
public final class VendorConfigurationTest {

  private static final String FILENAME = "filename";

  private enum TestStructureType implements StructureType {
    TEST_STRUCTURE_TYPE1("structure type1");

    private final String _description;

    TestStructureType(String description) {
      _description = description;
    }

    @Override
    public String getDescription() {
      return _description;
    }
  }

  private enum TestStructureUsage implements StructureUsage {
    TEST_STRUCTURE_USAGE1("structure usage1");

    private final String _description;

    TestStructureUsage(String description) {
      _description = description;
    }

    @Override
    public String getDescription() {
      return _description;
    }
  }

  private final TestStructureType _testStructureType = TestStructureType.TEST_STRUCTURE_TYPE1;
  private final TestStructureUsage _testStructureUsage = TestStructureUsage.TEST_STRUCTURE_USAGE1;

  private static final class TestVendorConfiguration extends VendorConfiguration {
    @Override
    public String getHostname() {
      return "hostname";
    }

    @Override
    public void setHostname(String hostname) {}

    @Override
    public void setVendor(ConfigurationFormat format) {}

    @Override
    public List<Configuration> toVendorIndependentConfigurations()
        throws VendorConversionException {
      return ImmutableList.of();
    }
  }

  private static VendorConfiguration buildVendorConfiguration() {
    VendorConfiguration c = new TestVendorConfiguration();
    c.setFilename(FILENAME);
    c.setAnswerElement(new ConvertConfigurationAnswerElement());
    c.setWarnings(new Warnings(true, true, true));
    return c;
  }

  @Test
  public void testRenameStructure() {
    String orgName = "orgName";
    int orgLine = 1;
    String newName = "newName";

    int otherLine = 2;
    String unaffectedName = "unaffectedName";

    // Renaming an undefined structure
    {
      VendorConfiguration c = buildVendorConfiguration();

      c.renameStructure(_testStructureType, orgName, newName);
      // Produce an appropriate warning
      assertThat(
          c.getWarnings(), hasRedFlag(hasText("Cannot rename undefined structure orgName.")));
    }

    // Renaming is invalid if it would clash with another structure
    {
      VendorConfiguration c = buildVendorConfiguration();

      c.defineSingleLineStructure(_testStructureType, orgName, orgLine);
      c.defineSingleLineStructure(_testStructureType, newName, otherLine);

      c.renameStructure(_testStructureType, orgName, newName);
      // Produces appropriate warning
      assertThat(c.getWarnings(), hasRedFlag(hasText("New name newName is already in use.")));

      // Both org and new structure defs persist
      assertThat(
          c.getAnswerElement(),
          hasDefinedStructureWithDefinitionLines(
              FILENAME, _testStructureType, orgName, contains(orgLine)));
      assertThat(
          c.getAnswerElement(),
          hasDefinedStructureWithDefinitionLines(
              FILENAME, _testStructureType, newName, contains(otherLine)));
    }

    // Valid structure renaming
    {
      VendorConfiguration c = buildVendorConfiguration();

      c.defineSingleLineStructure(_testStructureType, orgName, orgLine);
      c.defineSingleLineStructure(_testStructureType, unaffectedName, otherLine);
      c.referenceStructure(_testStructureType, unaffectedName, _testStructureUsage, 11);
      c.referenceStructure(_testStructureType, orgName, _testStructureUsage, 22);
      c.renameStructure(_testStructureType, orgName, newName);
      // Need to call setAnswerElement to trigger population of CCAE / answerElement (for refs)
      c.setAnswerElement(new ConvertConfigurationAnswerElement());

      // No warnings
      assertThat(c.getWarnings(), hasRedFlags(emptyIterable()));
      // Has a definition and reference for the new structure name
      assertThat(
          c.getAnswerElement(),
          hasDefinedStructureWithDefinitionLines(
              FILENAME, _testStructureType, newName, contains(orgLine)));
      assertThat(
          c.getAnswerElement(),
          hasReferencedStructure(FILENAME, _testStructureType, newName, _testStructureUsage));
      // Unaffected reference should persist
      assertThat(
          c.getAnswerElement(),
          hasReferencedStructure(
              FILENAME, _testStructureType, unaffectedName, _testStructureUsage));
    }
  }
}
