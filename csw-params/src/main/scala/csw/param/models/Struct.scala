package csw.param.models

import csw.param.Parameters.{ParameterSet, ParameterSetType}
import csw.param.parameters.Parameter

case class Struct(paramSet: ParameterSet = Set.empty[Parameter[_]]) extends ParameterSetType[Struct] {

  /**
   * This is here for Java to construct with String
   */
  def this() = this(Set.empty[Parameter[_]])

  override def create(data: ParameterSet) = Struct(data)

  def dataToString1 = paramSet.mkString(", ")

  override def toString = dataToString1
}
