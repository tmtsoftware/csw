package csw.messages.commands

import csw.messages.params.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.params.models.Prefix
import play.api.libs.json.{Json, OFormat}

/**
 * A result containing parameters for command response
 *
 * @param prefix   identifies the subsystem that received the command and created command response out of it
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Result private (prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[Result]
    with ParameterSetKeyData {

  /**
   * Create a new Result instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of Result with provided data
   */
  override protected def create(data: Set[Parameter[_]]): Result = copy(paramSet = data)

  /**
   * A java helper to construct Result
   */
  def this(prefix: String) = this(Prefix(prefix))

}

object Result {

  /**
   * A helper method to create Result instance
   *
   * @param prefix identifies the subsystem that received the command and created command response out of it
   * @param paramSet an optional initial set of parameters (keys with values)
   * @return a Result instance with provided prefix and paramSet
   */
  def apply(prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Result =
    new Result(prefix).madd(paramSet)

  implicit val format: OFormat[Result] = Json.format[Result]
}
