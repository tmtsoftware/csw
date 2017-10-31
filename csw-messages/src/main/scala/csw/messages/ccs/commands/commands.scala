package csw.messages.ccs.commands

import csw.messages.params.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.params.models.{ObsId, Prefix, RunId}

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
  val obsId: ObsId

  /**
   * identifies the target subsystem
   */
  val prefix: Prefix

  /**
   * an optional initial set of parameters (keys with values)
   */
  val paramSet: Set[Parameter[_]]
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
 * @param obsId the observation id
 * @param prefix identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Setup private (
    runId: RunId,
    obsId: ObsId,
    prefix: Prefix,
    paramSet: Set[Parameter[_]] = Set.empty
) extends ParameterSetType[Setup]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]): Setup = new Setup(runId, obsId, prefix, data)

  // This is here for Java to construct with String
  def this(obsId: ObsId, prefix: String) = this(RunId(), obsId, Prefix(prefix))
}

object Setup {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to setup model
  private[messages] def apply(
      runId: RunId,
      obsId: ObsId,
      prefix: Prefix,
      paramSet: Set[Parameter[_]]
  ): Setup =
    new Setup(runId, obsId, prefix, paramSet) //madd is not required as this version of apply is only used for reading json

  // The apply method is used to create Setup command by end-user. RunId is not accepted and will be created internally to guarantee unique value.
  def apply(
      obsId: ObsId,
      prefix: Prefix,
      paramSet: Set[Parameter[_]] = Set.empty
  ): Setup = new Setup(RunId(), obsId, prefix).madd(paramSet) //madd ensures check for duplicate key
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param obsId the observation id
 * @param prefix identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Wait private (
    runId: RunId,
    obsId: ObsId,
    prefix: Prefix,
    paramSet: Set[Parameter[_]] = Set.empty
) extends ParameterSetType[Wait]
    with ParameterSetKeyData
    with SequenceCommand {

  override protected def create(data: Set[Parameter[_]]) = new Wait(runId, obsId, prefix, data)

  // This is here for Java to construct with String
  def this(obsId: ObsId, prefix: String) = this(RunId(), obsId, Prefix(prefix))
}

object Wait {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to observe model
  private[messages] def apply(
      runId: RunId,
      obsId: ObsId,
      prefix: Prefix,
      paramSet: Set[Parameter[_]]
  ): Wait =
    new Wait(runId, obsId, prefix, paramSet) //madd is not required as this version of apply is only used for reading json

  // The apply method is used to create Observe command by end-user. RunId is not accepted and will be created internally to guarantee unique value.
  def apply(
      obsId: ObsId,
      prefix: Prefix,
      paramSet: Set[Parameter[_]] = Set.empty
  ): Wait = new Wait(RunId(), obsId, prefix).madd(paramSet) //madd ensures check for duplicate key
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param obsId the observation id
 * @param prefix identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Observe private (
    runId: RunId,
    obsId: ObsId,
    prefix: Prefix,
    paramSet: Set[Parameter[_]] = Set.empty
) extends ParameterSetType[Observe]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]) = new Observe(runId, obsId, prefix, data)

  // This is here for Java to construct with String
  def this(obsId: ObsId, prefix: String) = this(RunId(), obsId, Prefix(prefix))
}

object Observe {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to observe model
  private[messages] def apply(
      runId: RunId,
      obsId: ObsId,
      prefix: Prefix,
      paramSet: Set[Parameter[_]]
  ): Observe =
    new Observe(runId, obsId, prefix, paramSet) //madd is not required as this version of apply is only used for reading json

  // The apply method is used to create Observe command by end-user. RunId is not accepted and will be created internally to guarantee unique value.
  def apply(
      obsId: ObsId,
      prefix: Prefix,
      paramSet: Set[Parameter[_]] = Set.empty
  ): Observe =
    new Observe(RunId(), obsId, prefix).madd(paramSet) //madd ensures check for duplicate key
}
