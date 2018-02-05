package csw.messages.ccs.commands

import java.util.Optional

import csw.messages.params.generics.{Parameter, ParameterSetType}
import csw.messages.params.models.{Id, ObsId, Prefix}

import scala.compat.java8.OptionConverters.{RichOptionForJava8, RichOptionalGeneric}

/**
 * Common trait for Setup, Observe and Wait commands
 */
sealed trait Command { self: ParameterSetType[_] ⇒

  def paramType: ParameterSetType[_] = self

  /**
   * A name identifying the type of parameter set, such as "setup", "observe".
   * This is used in the JSON and toString output.
   */
  def typeName: String

  /**
   * unique ID for command parameter set
   */
  val runId: Id

  /**
   * an optional initial set of parameters (keys with values)
   */
  val paramSet: Set[Parameter[_]]

  /**
   * Prefix representing source of the command
   */
  val source: Prefix

  val commandName: CommandName

  val maybeObsId: Option[ObsId]
  def jMaybeObsId: Optional[ObsId] = maybeObsId.asJava

  override def toString =
    s"$typeName(runId=$runId, paramSet=$paramSet, source=$source, commandName=$commandName, maybeObsId=$maybeObsId)"
}

/**
 * Trait for sequence parameter sets
 */
sealed trait SequenceCommand extends Command { self: ParameterSetType[_] ⇒
}

/**
 * Marker trait for control parameter sets
 */
sealed trait ControlCommand extends Command { self: ParameterSetType[_] ⇒
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param paramSet an optional initial set of parameters (keys with values)
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

  def this(source: Prefix, commandName: CommandName, maybeObsId: Optional[ObsId]) =
    this(Id(), source, commandName, maybeObsId.asScala, Set.empty)

  override protected def create(data: Set[Parameter[_]]): Setup = copy(runId = Id(), paramSet = data)
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

  // The apply method is used to create Setup command by end-user. RunId is not accepted and will be created internally to guarantee unique value.
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId]): Setup =
    apply(Id(), source, commandName, maybeObsId, Set.empty)

  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Setup =
    apply(source, commandName, maybeObsId).madd(paramSet)
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param paramSet an optional initial set of parameters (keys with values)
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

  def this(source: Prefix, commandName: CommandName, maybeObsId: Optional[ObsId]) =
    this(Id(), source, commandName, maybeObsId.asScala, Set.empty)

  override protected def create(data: Set[Parameter[_]]): Observe = copy(runId = Id(), paramSet = data)
}

object Observe {

  private[messages] def apply(
      runId: Id,
      source: Prefix,
      commandName: CommandName,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]]
  ) = new Observe(runId, source, commandName, maybeObsId, paramSet)

  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId]): Observe =
    apply(Id(), source, commandName, maybeObsId, Set.empty)

  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Observe =
    apply(source, commandName, maybeObsId).madd(paramSet)
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Wait private (
    runId: Id,
    source: Prefix,
    commandName: CommandName,
    maybeObsId: Option[ObsId],
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[Wait]
    with SequenceCommand {

  def this(source: Prefix, commandName: CommandName, maybeObsId: Optional[ObsId]) =
    this(Id(), source, commandName, maybeObsId.asScala, Set.empty)

  override protected def create(data: Set[Parameter[_]]): Wait = copy(runId = Id(), paramSet = data)
}

object Wait {

  private[messages] def apply(
      runId: Id,
      source: Prefix,
      commandName: CommandName,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]]
  ) = new Wait(runId, source, commandName, maybeObsId, paramSet)

  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId]): Wait =
    apply(Id(), source, commandName, maybeObsId, Set.empty)

  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Wait =
    apply(source, commandName, maybeObsId).madd(paramSet)
}
