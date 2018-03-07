package csw.messages.commands

import csw.messages.params.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.params.models.Prefix

/**
 * A parameters set for returning results
 *
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
//TODO: add doc with what, why and how of result model
case class Result private (prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Result]
    with ParameterSetKeyData {

  //TODO: add doc why we need create and from where it is coming
  override protected def create(data: Set[Parameter[_]]): Result = copy(paramSet = data)

  // This is here for Java to construct with String
  def this(prefix: String) = this(Prefix(prefix))

}

object Result {
  def apply(prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Result =
    new Result(prefix).madd(paramSet)
}
