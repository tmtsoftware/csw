package csw.messages.params.generics

import java.time.Instant

import csw.messages.params.formats.JsonSupport
import csw.messages.params.models.Units.second
import csw.messages.params.models.{Units, _}
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json._

import scala.collection.immutable
import scala.reflect.ClassTag

/**
 * Generic marker class for creating various types of Keys.
 *
 * @tparam S the type of values that will sit against the key in Parameter
 */
sealed class KeyType[S: Format: ClassTag] extends EnumEntry with Serializable {
  override def hashCode: Int              = toString.hashCode
  override def equals(that: Any): Boolean = that.toString == this.toString

  private[messages] def paramFormat: Format[Parameter[S]] = Parameter[S]
}

/**
 * SimpleKeyType with a name. Holds instances of primitives such as char, int, String etc.
 *
 * @tparam S the type of values that will sit against the key in Parameter
 */
class SimpleKeyType[S: Format: ClassTag] extends KeyType[S] {

  /**
   * Make a Key from provided name
   *
   * @param name represents keyName in Key
   * @return a Key[S] with NoUnits where S is the type of values that will sit against the key in Parameter
   */
  def make(name: String): Key[S] = new Key[S](name, this)
}

/**
 * A KeyType that allows name and unit to be specified during creation. Holds instances of primitives such as
 * char, int, String etc.
 *
 * @param defaultUnits applicable units
 * @tparam S the type of values that will sit against the key in Parameter
 */
sealed class SimpleKeyTypeWithUnits[S: Format: ClassTag](defaultUnits: Units) extends KeyType[S] {

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
sealed class ArrayKeyType[S: Format: ClassTag] extends SimpleKeyType[ArrayData[S]]

/**
 * A KeyType that holds Matrix
 */
sealed class MatrixKeyType[S: Format: ClassTag] extends SimpleKeyType[MatrixData[S]]

/**
 * KeyTypes defined for consumption in Scala code
 */
object KeyType extends Enum[KeyType[_]] with PlayJsonEnum[KeyType[_]] {

  import JsonSupport._

  /**
   * values return a Seq of all KeyTypes provided by `csw-messages`
   */
  override def values: immutable.IndexedSeq[KeyType[_]] = findValues

  case object ChoiceKey extends KeyType[Choice] {
    def make(name: String, choices: Choices): GChoiceKey = new GChoiceKey(name, this, choices)
    def make(name: String, firstChoice: Choice, restChoices: Choice*): GChoiceKey =
      new GChoiceKey(name, this, Choices(restChoices.toSet + firstChoice))
  }

  case object RaDecKey     extends SimpleKeyType[RaDec]
  case object StringKey    extends SimpleKeyType[String]
  case object StructKey    extends SimpleKeyType[Struct]
  case object TimestampKey extends SimpleKeyTypeWithUnits[Instant](second)

  //scala
  case object BooleanKey extends SimpleKeyType[Boolean]
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

  implicit def format2[T]: Format[KeyType[T]] = implicitly[Format[KeyType[_]]].asInstanceOf[Format[KeyType[T]]]
}
