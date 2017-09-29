package csw.messages.commands

import csw.messages.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.models.params.Prefix

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
   * information related to the parameter set
   */
  val info: CommandInfo

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
 * @param info     information related to the parameter set
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Setup private (info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Setup]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]): Setup = new Setup(info, prefix, data)

  // This is here for Java to construct with String
  def this(info: CommandInfo, prefix: String) = this(info, Prefix(prefix))
}

object Setup {
  def apply(info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Setup =
    new Setup(info, prefix).madd(paramSet)
}

/**
 * a parameter set indicating a pause in processing
 *
 * @param info     information related to the parameter set
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Wait private (info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Wait]
    with ParameterSetKeyData
    with SequenceCommand {

  override protected def create(data: Set[Parameter[_]]) = new Wait(info, prefix, data)

  // This is here for Java to construct with String
  def this(info: CommandInfo, prefix: String) = this(info, Prefix(prefix))
}

object Wait {
  def apply(info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Wait =
    new Wait(info, prefix).madd(paramSet)
}

/**
 * a parameter set for setting observation parameters
 *
 * @param info     information related to the parameter set
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Observe private (info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Observe]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]) = new Observe(info, prefix, data)

  // This is here for Java to construct with String
  def this(info: CommandInfo, prefix: String) = this(info, Prefix(prefix))
}

object Observe {
  def apply(info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Observe =
    new Observe(info, prefix).madd(paramSet)
}
