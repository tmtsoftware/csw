package csw.param.parameters

import csw.param.Parameters.{ParameterSet, ParameterSetType}
import csw.param.UnitsOfMeasure.{NoUnits, Units}

/**
 * TMT Source Code: 9/28/16.
 */
case class StructParameter(keyName: String, values: Vector[Struct], units: Units) extends Parameter[Struct] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)

}

final case class StructKey(nameIn: String) extends Key[Struct, StructParameter](nameIn) {
  override def set(v: Vector[Struct], units: Units = NoUnits) = StructParameter(keyName, v, units)

  override def set(v: Struct*) = StructParameter(keyName, v.toVector, units = NoUnits)
}

/**
 * A configuration for setting telescope and instrument parameters
 *
 * @param paramSet an optional initial gset of items (keys with values)
 */
case class Struct(paramSet: ParameterSet = Set.empty[Parameter[_]]) extends ParameterSetType[Struct] {

  /**
   * This is here for Java to construct with String
   */
  def this() = this(Set.empty[Parameter[_]])

  override def create(data: ParameterSet) = Struct(data)

  def dataToString1 = paramSet.mkString(", ")

  override def toString = dataToString1
}
