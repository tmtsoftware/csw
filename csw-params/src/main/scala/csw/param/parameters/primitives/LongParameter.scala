package csw.param.parameters.primitives

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param.parameters.{Key, Parameter}
import csw.param.UnitsOfMeasure

import scala.collection.immutable.Vector

/**
 * The type of a value for an LongKey
 *
 * @param keyName the name of the key
 * @param values   the value for the key
 * @param units   the units of the value
 */
final case class LongParameter(keyName: String, values: Vector[Long], units: Units) extends Parameter[Long] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key of Long values
 *
 * @param nameIn the name of the key
 */
final case class LongKey(nameIn: String) extends Key[Long, LongParameter](nameIn) {

  override def set(v: Vector[Long], units: Units = NoUnits) = LongParameter(keyName, v, units)

  override def set(v: Long*) = LongParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
