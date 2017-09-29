package csw.messages.commands

import csw.messages.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.models.params.Prefix

/**
 * A parameters set for returning results
 *
 * @param info     information related to the parameter set
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Result private (info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Result]
    with ParameterSetKeyData {

  override protected def create(data: Set[Parameter[_]]) = new Result(info, prefix, data)

  // This is here for Java to construct with String
  def this(info: CommandInfo, prefix: String) = this(info, new Prefix(prefix))
}

object Result {
  def apply(info: CommandInfo, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Result =
    new Result(info, prefix).madd(paramSet)
}
