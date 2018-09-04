package csw.messages.commands

import csw.messages.params.generics.{Parameter, ParameterSetType}
import csw.messages.params.models.{Id, ObsId, Prefix}

/**
 * Common trait representing commands in TMT like Setup, Observe and Wait
 */
sealed trait Command { self: ParameterSetType[_] ⇒

  /**
   * A helper to give access of public members of ParameterSetType
   *
   * @return a handle to ParameterSetType extended by concrete implementation of this class
   */
  def paramType: ParameterSetType[_] = self

  /**
   * A name identifying the type of parameter set, such as "setup", "observe".
   * This is used in toString output and while de-serializing from JSON.
   *
   * @return a string representation of concrete type of this class
   */
  def typeName: String

  /**
   * unique Id for command parameter set
   */
  val runId: Id

  /**
   * An optional initial set of parameters (keys with values)
   */
  val paramSet: Set[Parameter[_]]

  /**
   * Prefix representing source of the command
   */
  val source: Prefix

  /**
   * The name of command
   */
  val commandName: CommandName

  /**
   * An optional obsId for command
   */
  val maybeObsId: Option[ObsId]

  /**
   * A common toString method for all concrete implementation
   *
   * @return the string representation of command
   */
  override def toString: String =
    s"$typeName(runId=$runId, paramSet=$paramSet, source=$source, commandName=$commandName, maybeObsId=$maybeObsId)"
}

/**
 * Marker trait for sequence parameter sets which is applicable to Sequencer type of components
 */
sealed trait SequenceCommand extends Command { self: ParameterSetType[_] ⇒
}

/**
 * Marker trait for control parameter sets which i applicable to Assembly and HCD type of components
 */
sealed trait ControlCommand extends Command { self: ParameterSetType[_] ⇒
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 */
case class Setup private (
    runId: Id,
    source: Prefix,
    commandName: CommandName,
    maybeObsId: Option[ObsId],
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[Setup]
    with SequenceCommand
    with ControlCommand {

  /**
   * Create a new Setup instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of Setup with new runId and provided data
   */
  override protected def create(data: Set[Parameter[_]]): Setup = copy(runId = Id(), paramSet = data)

  /**
   * Create a new Setup instance from an existing instance
   *
   * @return a new instance of Setup with new runId and copied data
   */
  def cloneCommand: Setup = copy(Id())
}

object Setup {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to setup model
  private[messages] def apply(
      runId: Id,
      source: Prefix,
      commandName: CommandName,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]]
  ): Setup = new Setup(runId, source, commandName, maybeObsId, paramSet)

  /**
   * The apply method is used to create Setup command by end-user. runId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @return a new instance of Setup with auto-generated runId and empty paramSet
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId]): Setup =
    apply(Id(), source, commandName, maybeObsId, Set.empty)

  /**
   * The apply method is used to create Setup command by end-user. runId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @param paramSet an initial set of parameters (keys with values)
   * @return a new instance of Setup with auto-generated runId
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Setup =
    apply(source, commandName, maybeObsId).madd(paramSet)
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 */
case class Observe private (
    runId: Id,
    source: Prefix,
    commandName: CommandName,
    maybeObsId: Option[ObsId],
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[Observe]
    with SequenceCommand
    with ControlCommand {

  /**
   * Create a new Observe instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of Observe with new runId and provided data
   */
  override protected def create(data: Set[Parameter[_]]): Observe = copy(runId = Id(), paramSet = data)

  /**
   * Create a new Observer instance from an existing instance
   *
   * @return a new instance of Observe with new runId and copied data
   */
  def cloneCommand: Observe = copy(Id())
}

object Observe {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to observe model
  private[messages] def apply(
      runId: Id,
      source: Prefix,
      commandName: CommandName,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]]
  ) = new Observe(runId, source, commandName, maybeObsId, paramSet)

  /**
   * The apply method is used to create Observe command by end-user. runId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @return a new instance of Observe with auto-generated runId and empty paramSet
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId]): Observe =
    apply(Id(), source, commandName, maybeObsId, Set.empty)

  /**
   * The apply method is used to create Observe command by end-user. runId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @param paramSet an initial set of parameters (keys with values)
   * @return a new instance of Observe with auto-generated runId
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Observe =
    apply(source, commandName, maybeObsId).madd(paramSet)
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 */
case class Wait private (
    runId: Id,
    source: Prefix,
    commandName: CommandName,
    maybeObsId: Option[ObsId],
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[Wait]
    with SequenceCommand {

  /**
   * Create a new Wait instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of Wait with new runId and provided data
   */
  override protected def create(data: Set[Parameter[_]]): Wait = copy(runId = Id(), paramSet = data)

  /**
   * Create a new Wait instance from an existing instance
   *
   * @return a new instance of Wait with new runId and copied data
   */
  def cloneCommand: Wait = copy(Id())
}

object Wait {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to wait model
  private[messages] def apply(
      runId: Id,
      source: Prefix,
      commandName: CommandName,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]]
  ) = new Wait(runId, source, commandName, maybeObsId, paramSet)

  /**
   * The apply method is used to create Wait command by end-user. runId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @return a new instance of Wait with auto-generated runId and empty paramSet
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId]): Wait =
    apply(Id(), source, commandName, maybeObsId, Set.empty)

  /**
   * The apply method is used to create Wait command by end-user. runId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @param paramSet an initial set of parameters (keys with values)
   * @return a new instance of Wait with auto-generated runId
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Wait =
    apply(source, commandName, maybeObsId).madd(paramSet)
}
