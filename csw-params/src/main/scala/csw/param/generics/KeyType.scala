package csw.param.generics

import csw.param.formats.JsonSupport
import csw.param.models._
import enumeratum.{Enum, EnumEntry}
import spray.json.JsonFormat

import scala.collection.immutable
import scala.reflect.ClassTag

sealed class KeyType[S: JsonFormat: ClassTag] extends EnumEntry with Serializable {
  def paramFormat: JsonFormat[Parameter[S]] = Parameter[S]
}

sealed class SimpleKeyType[S: JsonFormat: ClassTag] extends KeyType[S] {
  def make(name: String): Key[S] = new Key[S](name, this)
}

sealed class ArrayKeyType[S: JsonFormat: ClassTag]  extends SimpleKeyType[ArrayData[S]]
sealed class MatrixKeyType[S: JsonFormat: ClassTag] extends SimpleKeyType[MatrixData[S]]

object KeyType extends Enum[KeyType[_]] {

  import JsonSupport._

  override def values: immutable.IndexedSeq[KeyType[_]] = findValues

  case object ChoiceKey extends KeyType[Choice] {
    def make(name: String, choices: Choices): GChoiceKey = new GChoiceKey(name, this, choices)
    def make(name: String, choices: Choice*): GChoiceKey = new GChoiceKey(name, this, Choices(choices.toSet))
  }

  case object RaDecKey  extends SimpleKeyType[RaDec]
  case object StringKey extends SimpleKeyType[String]
  case object StructKey extends SimpleKeyType[Struct]

  //scala
  case object BooleanKey extends SimpleKeyType[Boolean]
  case object CharKey    extends SimpleKeyType[Char]
  case object ShortKey   extends SimpleKeyType[Short]
  case object LongKey    extends SimpleKeyType[Long]
  case object IntKey     extends SimpleKeyType[Int]
  case object FloatKey   extends SimpleKeyType[Float]
  case object DoubleKey  extends SimpleKeyType[Double]

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

  //java
  case object JBooleanKey extends SimpleKeyType[java.lang.Boolean]
  case object JCharKey    extends SimpleKeyType[java.lang.Character]
  case object JShortKey   extends SimpleKeyType[java.lang.Short]
  case object JLongKey    extends SimpleKeyType[java.lang.Long]
  case object JIntKey     extends SimpleKeyType[java.lang.Integer]
  case object JFloatKey   extends SimpleKeyType[java.lang.Float]
  case object JDoubleKey  extends SimpleKeyType[java.lang.Double]

  case object JByteArrayKey   extends ArrayKeyType[java.lang.Byte]
  case object JShortArrayKey  extends ArrayKeyType[java.lang.Short]
  case object JLongArrayKey   extends ArrayKeyType[java.lang.Long]
  case object JIntArrayKey    extends ArrayKeyType[java.lang.Integer]
  case object JFloatArrayKey  extends ArrayKeyType[java.lang.Float]
  case object JDoubleArrayKey extends ArrayKeyType[java.lang.Double]

  case object JByteMatrixKey   extends MatrixKeyType[java.lang.Byte]
  case object JShortMatrixKey  extends MatrixKeyType[java.lang.Short]
  case object JLongMatrixKey   extends MatrixKeyType[java.lang.Long]
  case object JIntMatrixKey    extends MatrixKeyType[java.lang.Integer]
  case object JFloatMatrixKey  extends MatrixKeyType[java.lang.Float]
  case object JDoubleMatrixKey extends MatrixKeyType[java.lang.Double]

  implicit def format[T]: JsonFormat[KeyType[T]] = enumFormat(this).asInstanceOf[JsonFormat[KeyType[T]]]

  implicit def format2: JsonFormat[KeyType[_]] = enumFormat(this)
}

object JKeyTypes {
  val ChoiceKey = KeyType.ChoiceKey
  val RaDecKey  = KeyType.RaDecKey
  val StringKey = KeyType.StringKey
  val StructKey = KeyType.StructKey

  val BooleanKey = KeyType.JBooleanKey
  val CharKey    = KeyType.JCharKey
  val ShortKey   = KeyType.JShortKey
  val LongKey    = KeyType.JLongKey
  val IntKey     = KeyType.JIntKey
  val FloatKey   = KeyType.JFloatKey
  val DoubleKey  = KeyType.JDoubleKey

  val ByteArrayKey   = KeyType.JByteArrayKey
  val ShortArrayKey  = KeyType.JShortArrayKey
  val LongArrayKey   = KeyType.JLongArrayKey
  val IntArrayKey    = KeyType.JIntArrayKey
  val FloatArrayKey  = KeyType.JFloatArrayKey
  val DoubleArrayKey = KeyType.JDoubleArrayKey

  val ByteMatrixKey   = KeyType.JByteMatrixKey
  val ShortMatrixKey  = KeyType.JShortMatrixKey
  val LongMatrixKey   = KeyType.JLongMatrixKey
  val IntMatrixKey    = KeyType.JIntMatrixKey
  val FloatMatrixKey  = KeyType.JFloatMatrixKey
  val DoubleMatrixKey = KeyType.JDoubleMatrixKey
}
