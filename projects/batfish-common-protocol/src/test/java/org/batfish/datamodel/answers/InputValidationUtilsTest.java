package org.batfish.datamodel.answers;

import static org.batfish.datamodel.answers.InputValidationUtils.getErrorMessage;
import static org.batfish.datamodel.answers.InputValidationUtils.validateIp;
import static org.batfish.datamodel.answers.InputValidationUtils.validatePrefix;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.function.Function;
import org.batfish.common.CompletionMetadata;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.answers.InputValidationNotes.Validity;
import org.batfish.datamodel.questions.BgpPeerPropertySpecifier;
import org.batfish.datamodel.questions.BgpProcessPropertySpecifier;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.NodePropertySpecifier;
import org.batfish.datamodel.questions.Variable;
import org.batfish.datamodel.questions.Variable.Type;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.role.NodeRolesData;
import org.batfish.specifier.parboiled.Grammar;
import org.junit.Test;

/** Tests for {@link org.batfish.datamodel.answers.InputValidationUtils} */
public class InputValidationUtilsTest {

  private static final CompletionMetadata emptyCompletionMetadata =
      CompletionMetadata.builder().build();
  private static final NodeRolesData emptyNodeRolesData = NodeRolesData.builder().build();
  private static final ReferenceLibrary emptyReferenceLibrary = new ReferenceLibrary(null);

  private static InputValidationNotes validateQuery(String query, Variable.Type varType) {
    return InputValidationUtils.validate(
        varType, query, emptyCompletionMetadata, emptyNodeRolesData, emptyReferenceLibrary);
  }

  @SuppressWarnings("ReturnValueIgnored")
  private static IllegalArgumentException getException(String query, Function<String, ?> getter) {
    try {
      getter.apply(query);
    } catch (IllegalArgumentException e) {
      return e;
    }
    return null;
  }

  @Test
  public void testBgpPeerPropertySpec() {
    // good case
    assertThat(
        validateQuery(BgpPeerPropertySpecifier.LOCAL_IP, Type.BGP_PEER_PROPERTY_SPEC),
        equalTo(new InputValidationNotes(Validity.VALID, ImmutableList.of())));

    // bad case
    assertThat(
        validateQuery("dumdum", Type.BGP_PEER_PROPERTY_SPEC),
        equalTo(
            new InputValidationNotes(
                Validity.INVALID,
                getErrorMessage(Grammar.BGP_PEER_PROPERTY_SPECIFIER.getFriendlyName(), 6),
                6)));
  }

  @Test
  public void testBgpProcessPropertySpec() {
    // good case
    assertThat(
        validateQuery(BgpProcessPropertySpecifier.TIE_BREAKER, Type.BGP_PROCESS_PROPERTY_SPEC),
        equalTo(new InputValidationNotes(Validity.VALID, ImmutableList.of())));

    // bad case
    assertThat(
        validateQuery("dumdum", Type.BGP_PROCESS_PROPERTY_SPEC),
        equalTo(
            new InputValidationNotes(
                Validity.INVALID,
                getErrorMessage(Grammar.BGP_PROCESS_PROPERTY_SPECIFIER.getFriendlyName(), 6),
                6)));
  }

  @Test
  public void testInterfacePropertySpec() {
    // good case
    assertThat(
        validateQuery(InterfacePropertySpecifier.DESCRIPTION, Type.INTERFACE_PROPERTY_SPEC),
        equalTo(new InputValidationNotes(Validity.VALID, ImmutableList.of())));

    // bad case
    assertThat(
        validateQuery("dumdum", Type.INTERFACE_PROPERTY_SPEC),
        equalTo(
            new InputValidationNotes(
                Validity.INVALID,
                getErrorMessage(Grammar.INTERFACE_PROPERTY_SPECIFIER.getFriendlyName(), 6),
                6)));
  }

  @Test
  public void testIpAddressBad() {
    String query = "1.1.1.345";

    IllegalArgumentException exception = getException(query, Ip::parse);

    assertThat(
        validateQuery(query, Type.IP_SPACE_SPEC),
        equalTo(new InputValidationNotes(Validity.INVALID, getErrorMessage(exception), -1)));
  }

  @Test
  public void testIpAddressGood() {
    String query = "1.1.1.3";
    assertThat(
        validateQuery(query, Type.IP_SPACE_SPEC),
        equalTo(new InputValidationNotes(Validity.VALID, ImmutableList.of())));
  }

  @Test
  public void testNodePropertySpec() {
    // good case
    assertThat(
        validateQuery(NodePropertySpecifier.NTP_SERVERS, Type.NODE_PROPERTY_SPEC),
        equalTo(new InputValidationNotes(Validity.VALID, ImmutableList.of())));

    // bad case
    assertThat(
        validateQuery("dumdum", Type.NODE_PROPERTY_SPEC),
        equalTo(
            new InputValidationNotes(
                Validity.INVALID,
                getErrorMessage(Grammar.NODE_PROPERTY_SPECIFIER.getFriendlyName(), 6),
                6)));
  }

  @Test
  public void testValidateIpValid() {
    InputValidationNotes notes = validateIp("1.1.1.1");
    assertThat(
        notes, equalTo(new InputValidationNotes(Validity.VALID, Ip.parse("1.1.1.1").toString())));
  }

  @Test
  public void testValidateIpInvalid() {
    InputValidationNotes notes = validateIp("1.1.1.1111");
    assertThat(
        notes,
        equalTo(
            new InputValidationNotes(
                Validity.INVALID, "Invalid IPv4 address: 1.1.1.1111. 1111 is an invalid octet")));
  }

  @Test
  public void testValidatePrefixValid() {
    InputValidationNotes notes = validatePrefix("1.1.1.1/23");
    assertThat(
        notes,
        equalTo(new InputValidationNotes(Validity.VALID, Prefix.parse("1.1.1.1/23").toString())));
  }

  @Test
  public void testValidatePrefixInvalid() {
    InputValidationNotes notes = validatePrefix("1.1.1.1111/23");
    assertThat(
        notes,
        equalTo(
            new InputValidationNotes(
                Validity.INVALID, "Invalid IPv4 address: 1.1.1.1111. 1111 is an invalid octet")));
  }
}
