package csw.messages.ccs.commands

import java.util.Optional

import csw.messages.params.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.params.models.{ObsId, Prefix, RunId}

import scala.compat.java8.OptionConverters.{RichOptionForJava8, RichOptionalGeneric}

/**
 * Common trait for Setup, Observe and Wait commands
 */
sealed trait Command {

  /**
   * A name identifying the type of parameter set, such as "setup", "observe".
   * This is used in the JSON and toString output.
   */
  def typeName: String

  /**
   * unique ID for command parameter set
   */
  val runId: RunId

  /**
   * the observation id
   */
  val maybeObsId: Option[ObsId]

  /**
   * identifies the target subsystem
   */
  val prefix: Prefix

  /**
   * an optional initial set of parameters (keys with values)
   */
  val paramSet: Set[Parameter[_]]

  /**
   * Convert the Option[ObsId] to Optional[ObsID]
   */
  def jMaybeObsId: Optional[ObsId] = maybeObsId.asJava
}

/**
 * Trait for sequence parameter sets
 */
sealed trait SequenceCommand extends Command

/**
 * Marker trait for control parameter sets
 */
sealed trait ControlCommand extends Command

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param prefix identifies the target subsystem
 * @param maybeObsId the observation id
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Setup private (runId: RunId, prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]] = Set.empty)
    extends ParameterSetType[Setup]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]): Setup = copy(paramSet = data)

  // This is here for Java to construct with String
  def this(prefix: String, maybeObsId: Optional[ObsId]) = this(RunId(), Prefix(prefix), maybeObsId.asScala)
}

object Setup {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to setup model
  private[messages] def apply(runId: RunId, prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Setup =
    new Setup(runId, prefix, maybeObsId, paramSet) //madd is not required as this version of apply is only used for reading json

  // The apply method is used to create Setup command by end-user. RunId is not accepted and will be created internally to guarantee unique value.
  def apply(prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]] = Set.empty): Setup =
    new Setup(RunId(), prefix, maybeObsId).madd(paramSet) //madd ensures check for duplicate key
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param prefix identifies the target subsystem
 * @param maybeObsId the observation id
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Observe private (runId: RunId, prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]] = Set.empty)
    extends ParameterSetType[Observe]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]): Observe = copy(paramSet = data)

  def this(prefix: String, maybeObsId: Optional[ObsId]) = this(RunId(), Prefix(prefix), maybeObsId.asScala)
}

object Observe {

  private[messages] def apply(runId: RunId, prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]) =
    new Observe(runId, prefix, maybeObsId, paramSet)

  def apply(prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]] = Set.empty): Observe =
    new Observe(RunId(), prefix, maybeObsId).madd(paramSet)
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param prefix identifies the target subsystem
 * @param maybeObsId the observation id
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Wait private (runId: RunId, prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]] = Set.empty)
    extends ParameterSetType[Wait]
    with ParameterSetKeyData
    with SequenceCommand {

  override protected def create(data: Set[Parameter[_]]): Wait = copy(paramSet = data)

  def this(prefix: String, maybeObsId: Optional[ObsId]) = this(RunId(), Prefix(prefix), maybeObsId.asScala)
}

object Wait {

  private[messages] def apply(runId: RunId, prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]) =
    new Wait(runId, prefix, maybeObsId, paramSet)

  def apply(prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]] = Set.empty): Wait =
    new Wait(RunId(), prefix, maybeObsId).madd(paramSet)
}

/**
 * A command for cancelling an ongoing command. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param prefix identifies the target subsystem
 * @param maybeObsId the observation id
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Cancel private (runId: RunId, prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]] = Set.empty)
    extends ParameterSetType[Cancel]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]): Cancel = copy(paramSet = data)

  def this(prefix: String, maybeObsId: Optional[ObsId], cancelId: RunId) =
    this(RunId(), Prefix(prefix), maybeObsId.asScala, Cancel.paramSet(cancelId))

  def cancelId: RunId = RunId(paramSet.head.head.asInstanceOf[String])
}

object Cancel {
  private[messages] def apply(runId: RunId, prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]) =
    new Cancel(runId, prefix, maybeObsId, paramSet)

  def apply(prefix: Prefix, maybeObsId: Option[ObsId], cancelId: RunId): Cancel =
    new Cancel(RunId(), prefix, maybeObsId).madd(paramSet(cancelId))

  private def paramSet(cancelId: RunId): Set[Parameter[_]] = Set(Keys.CancelKey.set(cancelId.id))
}
