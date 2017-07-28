package csw.param.parameters.primitives

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param.parameters.{Key, Parameter}
import csw.param.UnitsOfMeasure

import scala.collection.immutable.Vector

/**
 * The type of a value for an DoubleKey
 *
 * @param keyName the name of the key
 * @param values  the value for the key
 * @param units   the units of the value
 */
final case class DoubleParameter(keyName: String, values: Vector[Double], units: Units) extends Parameter[Double] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key of Double values
 *
 * @param nameIn the name of the key
 */
final case class DoubleKey(nameIn: String) extends Key[Double, DoubleParameter](nameIn) {

  override def set(v: Vector[Double], units: Units = NoUnits) = DoubleParameter(keyName, v, units)

  override def set(v: Double*) = DoubleParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
