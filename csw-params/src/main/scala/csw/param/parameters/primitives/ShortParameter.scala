package csw.param.parameters.primitives

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param.parameters.{Key, Parameter}
import csw.param.UnitsOfMeasure

import scala.collection.immutable.Vector

/**
 * The type of a value for an ShortKey
 *
 * @param keyName the name of the key
 * @param values   the value for the key
 * @param units   the units of the value
 */
final case class ShortParameter(keyName: String, values: Vector[Short], units: Units) extends Parameter[Short] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key of Short values
 *
 * @param nameIn the name of the key
 */
final case class ShortKey(nameIn: String) extends Key[Short, ShortParameter](nameIn) {

  override def set(v: Vector[Short], units: Units = NoUnits) = ShortParameter(keyName, v, units)

  override def set(v: Short*) = ShortParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
