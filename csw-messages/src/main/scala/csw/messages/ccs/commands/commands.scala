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
 * a parameter set for setting telescope and instrument parameters
 *
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Setup private (
    runId: RunId,
    obsId: ObsId,
    prefix: Prefix,
    paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
) extends ParameterSetType[Setup]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]): Setup = new Setup(runId, obsId, prefix, data)

  // This is here for Java to construct with String
  def this(runId: RunId, obsId: ObsId, prefix: String) = this(runId, obsId, Prefix(prefix))
}

object Setup {
  def apply(runId: RunId, obsId: ObsId, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Setup =
    new Setup(runId, obsId, prefix).madd(paramSet)
}

/**
 * a parameter set indicating a pause in processing
 *
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Wait private (
    runId: RunId,
    obsId: ObsId,
    prefix: Prefix,
    paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
) extends ParameterSetType[Wait]
    with ParameterSetKeyData
    with SequenceCommand {

  override protected def create(data: Set[Parameter[_]]) = new Wait(runId, obsId, prefix, data)

  // This is here for Java to construct with String
  def this(runId: RunId, obsId: ObsId, prefix: String) = this(runId, obsId, Prefix(prefix))
}

object Wait {
  def apply(runId: RunId, obsId: ObsId, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Wait =
    new Wait(runId, obsId, prefix).madd(paramSet)
}

/**
 * a parameter set for setting observation parameters
 *
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Observe private (
    runId: RunId,
    obsId: ObsId,
    prefix: Prefix,
    paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
) extends ParameterSetType[Observe]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]) = new Observe(runId, obsId, prefix, data)

  // This is here for Java to construct with String
  def this(runId: RunId, obsId: ObsId, prefix: String) = this(runId, obsId, Prefix(prefix))
}

object Observe {
  def apply(
      runId: RunId,
      obsId: ObsId,
      prefix: Prefix,
      paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
  ): Observe =
    new Observe(runId, obsId, prefix).madd(paramSet)
}
