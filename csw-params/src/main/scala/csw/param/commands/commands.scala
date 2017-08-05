package csw.param.commands

import csw.param.models.Prefix
import csw.param.parameters.{Key, Parameter, ParameterSetKeyData, ParameterSetType}

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
case class Setup(info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Setup]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override def create(data: Set[Parameter[_]]) = Setup(info, prefix, data)

  // This is here for Java to construct with String
  def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

  // The following overrides are needed for the Java API and javadocs
  // (Using a Java interface caused various Java compiler errors)
  override def add[P <: Parameter[_]](parameter: P): Setup = super.add(parameter)

  override def remove[S](key: Key[S]): Setup = super.remove(key)
}

/**
 * a parameter set indicating a pause in processing
 *
 * @param info     information related to the parameter set
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Wait(info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Wait]
    with ParameterSetKeyData
    with SequenceCommand {

  override def create(data: Set[Parameter[_]]) = Wait(info, prefix, data)

  // This is here for Java to construct with String
  def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

  // The following overrides are needed for the Java API and javadocs
  // (Using a Java interface caused various Java compiler errors)
  override def add[P <: Parameter[_]](parameter: P): Wait = super.add(parameter)

  override def remove[S](key: Key[S]): Wait = super.remove(key)
}

/**
 * a parameter set for setting observation parameters
 *
 * @param info     information related to the parameter set
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Observe(info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Observe]
    with ParameterSetKeyData
    with SequenceCommand
    with ControlCommand {

  override def create(data: Set[Parameter[_]]) = Observe(info, prefix, data)

  // This is here for Java to construct with String
  def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

  // The following overrides are needed for the Java API and javadocs
  // (Using a Java interface caused various Java compiler errors)
  override def add[P <: Parameter[_]](parameter: P): Observe = super.add(parameter)

  override def remove[S](key: Key[S]): Observe = super.remove(key)
}
