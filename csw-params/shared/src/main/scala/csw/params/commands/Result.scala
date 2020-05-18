package csw.params.commands

import csw.params.core.generics.{Parameter, ParameterSetType}

/**
 * A result containing parameters for command response
 */
case class Result private[params] (paramSet: Set[Parameter[_]]) extends ParameterSetType[Result] {

  def nonEmpty: Boolean = paramSet.nonEmpty

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
  def this() = this(Set.empty)

  /**
   * A String representation for concrete implementation of this trait
   */
  override def toString: String = s"$typeName($dataToString)"
}

object Result {

  def emptyResult = new Result()

  /**
   * A helper method to create Result instance
   *
   * @param paramSet a Set of parameters (keys with values)
   * @return a Result instance with provided paramSet
   */
  def apply(paramSet: Set[Parameter[_]]): Result = emptyResult.madd(paramSet)

  /**
   * A helper method to create Result instance
   *
   * @param params an optional list of parameters (keys with values)
   * @return a Result instance with provided paramSet
   */
  def apply(params: Parameter[_]*): Result = Result(params.toSet)
}
