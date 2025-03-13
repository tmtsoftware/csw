/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.generics

import csw.params.core.formats.ParamCodecs.*
import csw.params.core.formats.{ParamCodecs, ParamCore}
import csw.params.core.models.Coords.*
import csw.params.core.models.Units.{NoUnits, tai, utc}
import csw.params.core.models.*
import csw.time.core.models.{TAITime, UTCTime}
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.{Decoder, Encoder}

import scala.reflect.ClassTag

/**
 * Generic marker class for creating various types of Keys.
 *
 * @tparam S the type of values that will sit against the key in Parameter
 */
sealed trait KeyType[S: ArrayEnc: ArrayDec] extends EnumEntry with Serializable {
  override def hashCode: Int              = toString.hashCode
  override def equals(that: Any): Boolean = that.toString == this.toString

  private[params] lazy val paramEncoder: Encoder[Parameter[S]]     = ParamCodecs.paramCodec[S].encoder
  private[params] lazy val paramCoreDecoder: Decoder[ParamCore[S]] = ParamCodecs.paramCoreCodec[S].decoder
}

/**
 * SimpleKeyType with a name. Holds instances of primitives such as char, int, String etc.
 *
 * @tparam S the type of values that will sit against the key in Parameter
 */
class SimpleKeyType[S: ClassTag: ArrayEnc: ArrayDec] extends KeyType[S] {

  /**
   * Make a Key from provided name
   *
   * @param name represents keyName in Key
   * @return a Key[S] with NoUnits where S is the type of values that will sit against the key in Parameter
   */
  def make(name: String): Key[S] = new Key[S](name, this, NoUnits)

  /**
   * Make a Key from provided name and units
   *
   * @param name represents keyName in Key
   *             @param units represents the units of the values
   * @return a Key[S] with units where S is the type of values that will sit against the key in Parameter
   */
  def make(name: String, units: Units): Key[S] = new Key[S](name, this, units)

}

/**
 * A KeyType that allows name and unit to be specified during creation. Holds instances of primitives such as
 * char, int, String etc.
 *
 * @param defaultUnits applicable units
 * @tparam S the type of values that will sit against the key in Parameter
 */
class SimpleKeyTypeWithUnits[S: ClassTag: ArrayEnc: ArrayDec](defaultUnits: Units) extends KeyType[S] {

  /**
   * Make a Key from provided name
   *
   * @param name represents keyName in Key
   * @return a Key[S] with defaultUnits where S is the type of values that will sit against the key in Parameter
   */
  def make(name: String): Key[S] = new Key[S](name, this, defaultUnits)
}

/**
 * A KeyType that holds array
 */
class ArrayKeyType[S: ClassTag: ArrayEnc: ArrayDec] extends SimpleKeyType[ArrayData[S]]

/**
 * A KeyType that holds Matrix
 */
class MatrixKeyType[S: ClassTag: ArrayEnc: ArrayDec] extends SimpleKeyType[MatrixData[S]]

/**
 * KeyTypes defined for consumption in Scala code
 */
object KeyType extends Enum[KeyType[?]] {

  /**
   * values return a Seq of all KeyTypes provided by `csw-messages`
   */
  override def values: IndexedSeq[KeyType[?]] = findValues

  sealed class SKT[S: ClassTag: ArrayEnc: ArrayDec]                 extends SimpleKeyType[S] with KeyType[S]
  sealed class SKTWU[S: ClassTag: ArrayEnc: ArrayDec](value: Units) extends SimpleKeyTypeWithUnits[S](value) with KeyType[S]
  sealed class AKT[S: ClassTag: ArrayEnc: ArrayDec]                 extends ArrayKeyType[S] with KeyType[ArrayData[S]]
  sealed class MKT[S: ClassTag: ArrayEnc: ArrayDec]                 extends MatrixKeyType[S] with KeyType[MatrixData[S]]

  case object ChoiceKey extends KeyType[Choice] {
    def make(name: String, units: Units, choices: Choices): GChoiceKey = new GChoiceKey(name, this, units, choices)
    def make(name: String, choices: Choices): GChoiceKey               = make(name, NoUnits, choices)

    def make(name: String, units: Units, firstChoice: Choice, restChoices: Choice*): GChoiceKey =
      new GChoiceKey(name, this, units, Choices(restChoices.toSet + firstChoice))

    def make(name: String, firstChoice: Choice, restChoices: Choice*): GChoiceKey =
      make(name, NoUnits, firstChoice, restChoices*)
  }

  case object StringKey  extends SKT[String]
  case object UTCTimeKey extends SKTWU[UTCTime](utc)
  case object TAITimeKey extends SKTWU[TAITime](tai)

  case object EqCoordKey          extends SKT[EqCoord]
  case object SolarSystemCoordKey extends SKT[SolarSystemCoord]
  case object MinorPlanetCoordKey extends SKT[MinorPlanetCoord]
  case object CometCoordKey       extends SKT[CometCoord]
  case object AltAzCoordKey       extends SKT[AltAzCoord]
  case object CoordKey            extends SKT[Coord]

  // scala
  case object BooleanKey extends SKTWU[Boolean](NoUnits)
  case object CharKey    extends SKT[Char]

  case object ByteKey   extends SKT[Byte]
  case object ShortKey  extends SKT[Short]
  case object LongKey   extends SKT[Long]
  case object IntKey    extends SKT[Int]
  case object FloatKey  extends SKT[Float]
  case object DoubleKey extends SKT[Double]

  case object ByteArrayKey   extends AKT[Byte]
  case object ShortArrayKey  extends AKT[Short]
  case object LongArrayKey   extends AKT[Long]
  case object IntArrayKey    extends AKT[Int]
  case object FloatArrayKey  extends AKT[Float]
  case object DoubleArrayKey extends AKT[Double]

  case object ByteMatrixKey   extends MKT[Byte]
  case object ShortMatrixKey  extends MKT[Short]
  case object LongMatrixKey   extends MKT[Long]
  case object IntMatrixKey    extends MKT[Int]
  case object FloatMatrixKey  extends MKT[Float]
  case object DoubleMatrixKey extends MKT[Double]
}
