package csw.param.parameters

import csw.param.JsonSupport
import csw.param.formats.{EnumJsonSupport, JavaFormats}
import csw.param.models._
import enumeratum.{Enum, EnumEntry}
import spray.json.JsonFormat

import scala.collection.immutable
import scala.reflect.ClassTag

sealed class KeyType[S](implicit @transient jsFormat: JsonFormat[S], @transient clsTag: ClassTag[S])
    extends EnumEntry
    with Serializable {
  def paramFormat: JsonFormat[Parameter[S]] = Parameter[S]
}

sealed class SimpleKeyType[S](implicit @transient jsFormat: JsonFormat[S], @transient clsTag: ClassTag[S])
    extends KeyType[S] {
  def make(name: String): Key[S] = new Key[S](name, this)
}

object KeyType extends JsonSupport with JavaFormats with Enum[KeyType[_]] {

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

  case object ByteArrayKey   extends SimpleKeyType[ArrayData[Byte]]
  case object ShortArrayKey  extends SimpleKeyType[ArrayData[Short]]
  case object LongArrayKey   extends SimpleKeyType[ArrayData[Long]]
  case object IntArrayKey    extends SimpleKeyType[ArrayData[Int]]
  case object FloatArrayKey  extends SimpleKeyType[ArrayData[Float]]
  case object DoubleArrayKey extends SimpleKeyType[ArrayData[Double]]

  case object ByteMatrixKey   extends SimpleKeyType[MatrixData[Byte]]
  case object ShortMatrixKey  extends SimpleKeyType[MatrixData[Short]]
  case object LongMatrixKey   extends SimpleKeyType[MatrixData[Long]]
  case object IntMatrixKey    extends SimpleKeyType[MatrixData[Int]]
  case object FloatMatrixKey  extends SimpleKeyType[MatrixData[Float]]
  case object DoubleMatrixKey extends SimpleKeyType[MatrixData[Double]]

  //java
  case object JBooleanKey extends SimpleKeyType[java.lang.Boolean]
  case object JCharKey    extends SimpleKeyType[java.lang.Character]
  case object JShortKey   extends SimpleKeyType[java.lang.Short]
  case object JLongKey    extends SimpleKeyType[java.lang.Long]
  case object JIntKey     extends SimpleKeyType[java.lang.Integer]
  case object JFloatKey   extends SimpleKeyType[java.lang.Float]
  case object JDoubleKey  extends SimpleKeyType[java.lang.Double]

  case object JByteArrayKey   extends SimpleKeyType[ArrayData[java.lang.Byte]]
  case object JShortArrayKey  extends SimpleKeyType[ArrayData[java.lang.Short]]
  case object JLongArrayKey   extends SimpleKeyType[ArrayData[java.lang.Long]]
  case object JIntArrayKey    extends SimpleKeyType[ArrayData[java.lang.Integer]]
  case object JFloatArrayKey  extends SimpleKeyType[ArrayData[java.lang.Float]]
  case object JDoubleArrayKey extends SimpleKeyType[ArrayData[java.lang.Double]]

  case object JByteMatrixKey   extends SimpleKeyType[ArrayData[Array[java.lang.Byte]]]
  case object JShortMatrixKey  extends SimpleKeyType[MatrixData[java.lang.Short]]
  case object JLongMatrixKey   extends SimpleKeyType[MatrixData[java.lang.Long]]
  case object JIntMatrixKey    extends SimpleKeyType[ArrayData[Array[java.lang.Integer]]]
  case object JFloatMatrixKey  extends SimpleKeyType[MatrixData[java.lang.Float]]
  case object JDoubleMatrixKey extends SimpleKeyType[MatrixData[java.lang.Double]]

  implicit def format[T]: JsonFormat[KeyType[T]] = EnumJsonSupport.format[KeyType, T](this)
}

object JKeyTypes {

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
