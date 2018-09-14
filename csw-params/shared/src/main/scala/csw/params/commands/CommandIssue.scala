package csw.params.commands

/**
 * Describes a command issue with appropriate reason for validation failure
 */
sealed trait CommandIssue {

  /**
   * A method to access the reason of command issue
   *
   * @return the reason for a command issue
   */
  def reason: String
}

object CommandIssue {

  /**
   * Returned when a command is missing a required key/parameter
   *
   * @param reason describing the cause of this issue
   */
  final case class MissingKeyIssue(reason: String) extends CommandIssue

  /**
   * Returned when an Assembly receives a configuration with a Prefix that it doesn't support
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongPrefixIssue(reason: String) extends CommandIssue

  /**
   * Returned when the parameter for a key is not the correct type (i.e. int vs double, etc.)
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongParameterTypeIssue(reason: String) extends CommandIssue

  /**
   * Returned when a parameter value does not have the correct units
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongUnitsIssue(reason: String) extends CommandIssue

  /**
   * Returned when a command does not have the correct number of parameters
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongNumberOfParametersIssue(reason: String) extends CommandIssue

  //  // Returned when an Assembly receives a ConfigArg with multiple configs, but it can only execute one at a time
  //  final case class SingleConfigOnlyIssue(reason: String) extends ValidationIssue

  /**
   * Returned when an Assembly receives a command and one is already executing
   *
   * @param reason describing the cause of this issue
   */
  final case class AssemblyBusyIssue(reason: String) extends CommandIssue

  /**
   * Returned when some required location is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class UnresolvedLocationsIssue(reason: String) extends CommandIssue

  /**
   *  Parameter of a command is out of range
   *
   * @param reason describing the cause of this issue
   */
  final case class ParameterValueOutOfRangeIssue(reason: String) extends CommandIssue

  /**
   * The component is in the wrong internal state to handle a command
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongInternalStateIssue(reason: String) extends CommandIssue

  /**
   * A command is unsupported in the current state
   *
   * @param reason describing the cause of this issue
   */
  final case class UnsupportedCommandInStateIssue(reason: String) extends CommandIssue

  /**
   * A command is unsupported by component
   *
   * @param reason describing the cause of this issue
   */
  final case class UnsupportedCommandIssue(reason: String) extends CommandIssue

  /**
   * A required service is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class RequiredServiceUnavailableIssue(reason: String) extends CommandIssue

  /**
   *  sA required HCD is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class RequiredHCDUnavailableIssue(reason: String) extends CommandIssue

  /**
   * A required Assembly is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class RequiredAssemblyUnavailableIssue(reason: String) extends CommandIssue

  /**
   * A required Sequencer is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class RequiredSequencerUnavailableIssue(reason: String) extends CommandIssue

  /**
   * Returned when command received by one component when it is locked by other component
   *
   * @param reason describing the cause of this issue
   */
  final case class ComponentLockedIssue(reason: String) extends CommandIssue

  /**
   * Returned when some other issue occurred apart from those already defined
   *
   * @param reason describing the cause of this issue
   */
  final case class OtherIssue(reason: String) extends CommandIssue

}
