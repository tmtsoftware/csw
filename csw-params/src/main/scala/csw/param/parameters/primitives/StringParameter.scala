package csw.param.parameters.primitives

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param.parameters.{Key, Parameter}
import csw.param.UnitsOfMeasure

import scala.collection.immutable.Vector

/**
 * The type of a value for an StringKey
 *
 * @param keyName the name of the key
 * @param values   the value for the key
 * @param units   the units of the value
 */
final case class StringParameter(keyName: String, values: Vector[String], units: Units) extends Parameter[String] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key of String values
 *
 * @param nameIn the name of the key
 */
final case class StringKey(nameIn: String) extends Key[String, StringParameter](nameIn) {

  override def set(v: Vector[String], units: Units = NoUnits) = StringParameter(keyName, v, units)

  override def set(v: String*) = StringParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
