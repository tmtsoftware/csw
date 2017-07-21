package csw.common.ccs

/**
 * Describes the reason for a setup validation failure
 */
object Validation {

  sealed trait ValidationIssue { def reason: String }

  // Returned when a command is missing a required key/parameter
  final case class MissingKeyIssue(reason: String) extends ValidationIssue

  // Returned when an Assembly receives a configuration with a Prefix that it doesn't support
  final case class WrongPrefixIssue(reason: String) extends ValidationIssue

  // Returned when the parameter for a key is not the correct type (i.e. int vs double, etc.)
  final case class WrongParameterTypeIssue(reason: String) extends ValidationIssue

  // Returned when a parameter value does not have the correct units
  final case class WrongUnitsIssue(reason: String) extends ValidationIssue

  // Returned when a command does not have the correct number of parameters
  final case class WrongNumberOfParametersIssue(reason: String) extends ValidationIssue

  //  // Returned when an Assembly receives a ConfigArg with multiple configs, but it can only execute one at a time
  //  final case class SingleConfigOnlyIssue(reason: String) extends ValidationIssue

  // Returned when an Assembly receives a command and one is already executing
  final case class AssemblyBusyIssue(reason: String) extends ValidationIssue

  // Returned when some required location is not available
  final case class UnresolvedLocationsIssue(reason: String) extends ValidationIssue

  // Parameter of a command is out of range
  final case class ParameterValueOutOfRangeIssue(reason: String) extends ValidationIssue

  // The component is in the wrong internal state to handle a command
  final case class WrongInternalStateIssue(reason: String) extends ValidationIssue

  // A command is unsupported in the current state
  final case class UnsupportedCommandInStateIssue(reason: String) extends ValidationIssue

  // A command is unsupported by component
  final case class UnsupportedCommandIssue(reason: String) extends ValidationIssue

  // A required service is not available
  final case class RequiredServiceUnavailableIssue(reason: String) extends ValidationIssue

  // A required HCD is not available
  final case class RequiredHCDUnavailableIssue(reason: String) extends ValidationIssue

  // A required Assembly is not available
  final case class RequiredAssemblyUnavailableIssue(reason: String) extends ValidationIssue

  final case class RequiredSequencerUnavailableIssue(reason: String) extends ValidationIssue

  // Some other issue!
  final case class OtherIssue(reason: String) extends ValidationIssue

  /**
   * Base trait for the results of validating incoming commands
   * Only a subset of CommandStatus entries are also Validation (Valid, Invalid)
   */
  sealed trait Validation

  /**
   * The command was not valid before starting
   * @param issue the reason the setup is invalid
   */
  final case class Invalid(issue: ValidationIssue) extends Validation

  /**
   * The command was valid and started
   */
  case object Valid extends Validation
}
