package csw.param.parameters.primitives

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param.parameters.{Key, Parameter}
import csw.param.UnitsOfMeasure

import scala.collection.immutable.Vector

/**
 * The type of a value for an Boolean
 *
 * @param keyName the name of the key
 * @param values  the value for the key
 * @param units   the units of the value
 */
final case class BooleanParameter(keyName: String, values: Vector[Boolean], units: Units) extends Parameter[Boolean] {

  override def withUnits(unitsIn: Units): BooleanParameter = copy(units = unitsIn)
}

/**
 * A key of Boolean values
 *
 * @param nameIn the name of the key
 */
final case class BooleanKey(nameIn: String) extends Key[Boolean, BooleanParameter](nameIn) {

  override def set(v: Vector[Boolean], units: Units = NoUnits) = BooleanParameter(keyName, v, units)

  override def set(v: Boolean*) = BooleanParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
