package csw.messages.ccs

/**
 * Describes the reason for a setup validation failure
 */
sealed trait CommandIssue { def reason: String }

object CommandIssue {
  // Returned when a command is missing a required key/parameter
  final case class MissingKeyIssue(reason: String) extends CommandIssue

  // Returned when an Assembly receives a configuration with a Prefix that it doesn't support
  final case class WrongPrefixIssue(reason: String) extends CommandIssue

  // Returned when the parameter for a key is not the correct type (i.e. int vs double, etc.)
  final case class WrongParameterTypeIssue(reason: String) extends CommandIssue

  // Returned when a parameter value does not have the correct units
  final case class WrongUnitsIssue(reason: String) extends CommandIssue

  // Returned when a command does not have the correct number of parameters
  final case class WrongNumberOfParametersIssue(reason: String) extends CommandIssue

  //  // Returned when an Assembly receives a ConfigArg with multiple configs, but it can only execute one at a time
  //  final case class SingleConfigOnlyIssue(reason: String) extends ValidationIssue

  // Returned when an Assembly receives a command and one is already executing
  final case class AssemblyBusyIssue(reason: String) extends CommandIssue

  // Returned when some required location is not available
  final case class UnresolvedLocationsIssue(reason: String) extends CommandIssue

  // Parameter of a command is out of range
  final case class ParameterValueOutOfRangeIssue(reason: String) extends CommandIssue

  // The component is in the wrong internal state to handle a command
  final case class WrongInternalStateIssue(reason: String) extends CommandIssue

  // A command is unsupported in the current state
  final case class UnsupportedCommandInStateIssue(reason: String) extends CommandIssue

  // A command is unsupported by component
  final case class UnsupportedCommandIssue(reason: String) extends CommandIssue

  // A required service is not available
  final case class RequiredServiceUnavailableIssue(reason: String) extends CommandIssue

  // A required HCD is not available
  final case class RequiredHCDUnavailableIssue(reason: String) extends CommandIssue

  // A required Assembly is not available
  final case class RequiredAssemblyUnavailableIssue(reason: String) extends CommandIssue

  final case class RequiredSequencerUnavailableIssue(reason: String) extends CommandIssue

  // Some other issue!
  final case class OtherIssue(reason: String) extends CommandIssue
}
