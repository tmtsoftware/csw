package csw.params.core.generics

import csw.params.core.formats.ParamCodecs._
import csw.params.core.formats.{ParamCodecs, ParamCore}
import csw.params.core.models.Coords._
import csw.params.core.models.Units.{NoUnits, second}
import csw.params.core.models._
import csw.time.core.models.{TAITime, UTCTime}
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.{Decoder, Encoder}

import scala.reflect.ClassTag

/**
 * Generic marker class for creating various types of Keys.
 *
 * @tparam S the type of values that will sit against the key in Parameter
 */
sealed class KeyType[S: ArrayEnc: ArrayDec] extends EnumEntry with Serializable {
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
sealed class SimpleKeyTypeWithUnits[S: ClassTag: ArrayEnc: ArrayDec](defaultUnits: Units) extends KeyType[S] {

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
object KeyType extends Enum[KeyType[_]] {

  /**
   * values return a Seq of all KeyTypes provided by `csw-messages`
   */
  override def values: IndexedSeq[KeyType[_]] = findValues

  case object ChoiceKey extends KeyType[Choice] {
    def make(name: String, units: Units, choices: Choices): GChoiceKey = new GChoiceKey(name, this, units, choices)
    def make(name: String, units: Units, firstChoice: Choice, restChoices: Choice*): GChoiceKey =
      new GChoiceKey(name, this, units, Choices(restChoices.toSet + firstChoice))
  }

  case object StringKey  extends SimpleKeyType[String]
  case object StructKey  extends SimpleKeyType[Struct]
  case object UTCTimeKey extends SimpleKeyTypeWithUnits[UTCTime](second)
  case object TAITimeKey extends SimpleKeyTypeWithUnits[TAITime](second)

  case object RaDecKey            extends SimpleKeyType[RaDec]
  case object EqCoordKey          extends SimpleKeyType[EqCoord]
  case object SolarSystemCoordKey extends SimpleKeyType[SolarSystemCoord]
  case object MinorPlanetCoordKey extends SimpleKeyType[MinorPlanetCoord]
  case object CometCoordKey       extends SimpleKeyType[CometCoord]
  case object AltAzCoordKey       extends SimpleKeyType[AltAzCoord]
  case object CoordKey            extends SimpleKeyType[Coord]

  //scala
  case object BooleanKey extends SimpleKeyTypeWithUnits[Boolean](NoUnits)
  case object CharKey    extends SimpleKeyType[Char]

  case object ByteKey   extends SimpleKeyType[Byte]
  case object ShortKey  extends SimpleKeyType[Short]
  case object LongKey   extends SimpleKeyType[Long]
  case object IntKey    extends SimpleKeyType[Int]
  case object FloatKey  extends SimpleKeyType[Float]
  case object DoubleKey extends SimpleKeyType[Double]

  case object ByteArrayKey   extends ArrayKeyType[Byte]
  case object ShortArrayKey  extends ArrayKeyType[Short]
  case object LongArrayKey   extends ArrayKeyType[Long]
  case object IntArrayKey    extends ArrayKeyType[Int]
  case object FloatArrayKey  extends ArrayKeyType[Float]
  case object DoubleArrayKey extends ArrayKeyType[Double]

  case object ByteMatrixKey   extends MatrixKeyType[Byte]
  case object ShortMatrixKey  extends MatrixKeyType[Short]
  case object LongMatrixKey   extends MatrixKeyType[Long]
  case object IntMatrixKey    extends MatrixKeyType[Int]
  case object FloatMatrixKey  extends MatrixKeyType[Float]
  case object DoubleMatrixKey extends MatrixKeyType[Double]
}
