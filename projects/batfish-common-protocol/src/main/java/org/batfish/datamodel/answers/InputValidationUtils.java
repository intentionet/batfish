package org.batfish.datamodel.answers;

import static org.batfish.datamodel.Names.escapeNameIfNeeded;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.CompletionMetadata;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.answers.InputValidationNotes.Validity;
import org.batfish.datamodel.questions.Variable;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.role.NodeRolesData;
import org.batfish.specifier.parboiled.Grammar;
import org.batfish.specifier.parboiled.ParboiledInputValidator;

/** A utility class to validate question inputs */
@ParametersAreNonnullByDefault
public final class InputValidationUtils {

  /**
   * Validates user-provided input for questions. It is meant to do something interesting only for
   * variable types that are top-level (e.g., nodeSpec), and not sub parts (e.g., address group
   * names).
   */
  @Nonnull
  public static InputValidationNotes validate(
      Variable.Type varType,
      String query,
      CompletionMetadata completionMetadata,
      NodeRolesData nodeRolesData,
      ReferenceLibrary referenceLibrary) {

    switch (varType) {
      case APPLICATION_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.APPLICATION_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case BGP_PEER_PROPERTY_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.BGP_PEER_PROPERTY_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case BGP_PROCESS_PROPERTY_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.BGP_PROCESS_PROPERTY_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case BGP_SESSION_COMPAT_STATUS_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.BGP_SESSION_COMPAT_STATUS_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case OSPF_SESSION_STATUS_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.OSPF_SESSION_STATUS_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case BGP_SESSION_STATUS_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.BGP_SESSION_STATUS_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case BGP_SESSION_TYPE_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.BGP_SESSION_TYPE_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case FILTER_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.FILTER_SPECIFIER, query, completionMetadata, nodeRolesData, referenceLibrary);
      case INTERFACE_PROPERTY_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.INTERFACE_PROPERTY_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case INTERFACES_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.INTERFACE_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case IP:
        return validateIp(query);
      case IP_PROTOCOL_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.IP_PROTOCOL_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case IP_SPACE_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.IP_SPACE_SPECIFIER, query, completionMetadata, nodeRolesData, referenceLibrary);
      case IPSEC_SESSION_STATUS_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.IPSEC_SESSION_STATUS_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case LOCATION_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.LOCATION_SPECIFIER, query, completionMetadata, nodeRolesData, referenceLibrary);
      case MLAG_ID_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.MLAG_ID_SPECIFIER, query, completionMetadata, nodeRolesData, referenceLibrary);
      case NAMED_STRUCTURE_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.NAMED_STRUCTURE_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case NODE_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.NODE_SPECIFIER, query, completionMetadata, nodeRolesData, referenceLibrary);
      case NODE_PROPERTY_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.NODE_PROPERTY_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case OSPF_INTERFACE_PROPERTY_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.OSPF_INTERFACE_PROPERTY_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case OSPF_PROCESS_PROPERTY_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.OSPF_PROCESS_PROPERTY_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case PREFIX:
        return validatePrefix(query);
      case ROUTING_PROTOCOL_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.ROUTING_PROTOCOL_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case ROUTING_POLICY_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.ROUTING_POLICY_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      case VXLAN_VNI_PROPERTY_SPEC:
        return ParboiledInputValidator.validate(
            Grammar.VXLAN_VNI_PROPERTY_SPECIFIER,
            query,
            completionMetadata,
            nodeRolesData,
            referenceLibrary);
      default:
        return new InputValidationNotes(Validity.VALID, ImmutableList.of());
    }
  }

  public static String getErrorMessage(String grammarName, int startIndex) {
    return String.format("Cannot parse input as %s at index %d", grammarName, startIndex);
  }

  public static String getErrorMessage(IllegalArgumentException exception) {
    return exception.getMessage();
  }

  public static String getErrorMessageEmptyNameRegex(String nameRegex, String nameType) {
    return String.format("Regex /%s/ does not match any %s", nameRegex, nameType);
  }

  public static String getErrorMessageMissingName(String name, String nameType) {
    return String.format(
        "%s %s does not exist", capitalizeFirstChar(nameType), escapeNameIfNeeded(name));
  }

  /**
   * This function considers node role dimension as a book type, and node roles as groups within it
   */
  public static String getErrorMessageMissingGroup(
      String group, String groupType, String book, String bookType) {
    return String.format(
        "%s %s does not exist in %s %s",
        capitalizeFirstChar(groupType),
        escapeNameIfNeeded(group),
        bookType,
        escapeNameIfNeeded(book));
  }

  /** This function considers node role dimension as a book type, in addition to reference books */
  public static String getErrorMessageMissingBook(String book, String bookType) {
    return String.format(
        "%s %s does not exist", capitalizeFirstChar(bookType), escapeNameIfNeeded(book));
  }

  private static String capitalizeFirstChar(String nameType) {
    if (nameType.length() > 1) {
      return nameType.substring(0, 1).toUpperCase() + nameType.substring(1);
    }
    return nameType;
  }

  @VisibleForTesting
  static InputValidationNotes validateIp(String query) {
    try {
      Ip ip = Ip.parse(query);
      return new InputValidationNotes(Validity.VALID, ip.toString());
    } catch (Exception e) {
      return new InputValidationNotes(Validity.INVALID, e.getMessage());
    }
  }

  @VisibleForTesting
  static InputValidationNotes validatePrefix(String query) {
    try {
      Prefix pfx = Prefix.parse(query);
      return new InputValidationNotes(Validity.VALID, pfx.toString());
    } catch (Exception e) {
      return new InputValidationNotes(Validity.INVALID, e.getMessage());
    }
  }
}
