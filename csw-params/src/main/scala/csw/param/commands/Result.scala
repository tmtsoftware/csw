package csw.param.commands

import csw.param.models.Prefix
import csw.param.parameters.{Key, Parameter, ParameterSetKeyData, ParameterSetType}

/**
 * A parameters set for returning results
 *
 * @param info     information related to the parameter set
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Result(info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Result]
    with ParameterSetKeyData {

  override def create(data: Set[Parameter[_]]) = Result(info, prefix, data)

  // This is here for Java to construct with String
  def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

  // The following overrides are needed for the Java API and javadocs
  // (Using a Java interface caused various Java compiler errors)
  override def add[P <: Parameter[_]](parameter: P): Result = super.add(parameter)

  override def remove[S](key: Key[S]): Result = super.remove(key)
}
