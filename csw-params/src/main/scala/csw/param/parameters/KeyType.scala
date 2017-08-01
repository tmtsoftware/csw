package csw.param.parameters

import csw.param.{JavaFormats, JsonSupport, RaDec}
import enumeratum.{Enum, EnumEntry}
import spray.json.JsonFormat

import scala.collection.immutable
import scala.reflect.ClassTag

sealed class KeyType[S](implicit @transient jsFormat: JsonFormat[S], @transient clsTag: ClassTag[S])
    extends EnumEntry
    with Serializable {

  def make(name: String): GKey[S]        = new GKey[S](name, this)
  def paramFormat: JsonFormat[GParam[S]] = GParam[S]
}

object KeyType extends JsonSupport with JavaFormats with Enum[KeyType[_]] {

  override def values: immutable.IndexedSeq[KeyType[_]] = findValues

  case object RaDecKey  extends KeyType[RaDec]
  case object StringKey extends KeyType[String]
  case object StructKey extends KeyType[Struct]

  //scala
  case object BooleanKey extends KeyType[Boolean]
  case object CharKey    extends KeyType[Char]
  case object ShortKey   extends KeyType[Short]
  case object LongKey    extends KeyType[Long]
  case object IntKey     extends KeyType[Int]
  case object FloatKey   extends KeyType[Float]
  case object DoubleKey  extends KeyType[Double]

  case object ByteArrayKey   extends KeyType[GArray[Byte]]
  case object ShortArrayKey  extends KeyType[GArray[Short]]
  case object LongArrayKey   extends KeyType[GArray[Long]]
  case object IntArrayKey    extends KeyType[GArray[Int]]
  case object FloatArrayKey  extends KeyType[GArray[Float]]
  case object DoubleArrayKey extends KeyType[GArray[Double]]

  case object ByteMatrixKey   extends KeyType[GMatrix[Byte]]
  case object ShortMatrixKey  extends KeyType[GMatrix[Short]]
  case object LongMatrixKey   extends KeyType[GMatrix[Long]]
  case object IntMatrixKey    extends KeyType[GMatrix[Int]]
  case object FloatMatrixKey  extends KeyType[GMatrix[Float]]
  case object DoubleMatrixKey extends KeyType[GMatrix[Double]]

  //java
  case object JBooleanKey extends KeyType[java.lang.Boolean]
  case object JCharKey    extends KeyType[java.lang.Character]
  case object JShortKey   extends KeyType[java.lang.Short]
  case object JLongKey    extends KeyType[java.lang.Long]
  case object JIntKey     extends KeyType[java.lang.Integer]
  case object JFloatKey   extends KeyType[java.lang.Float]
  case object JDoubleKey  extends KeyType[java.lang.Double]

  case object JByteArrayKey   extends KeyType[GArray[java.lang.Byte]]
  case object JShortArrayKey  extends KeyType[GArray[java.lang.Short]]
  case object JLongArrayKey   extends KeyType[GArray[java.lang.Long]]
  case object JIntArrayKey    extends KeyType[GArray[java.lang.Integer]]
  case object JFloatArrayKey  extends KeyType[GArray[java.lang.Float]]
  case object JDoubleArrayKey extends KeyType[GArray[java.lang.Double]]

  case object JByteMatrixKey   extends KeyType[GArray[Array[java.lang.Byte]]]
  case object JShortMatrixKey  extends KeyType[GMatrix[java.lang.Short]]
  case object JLongMatrixKey   extends KeyType[GMatrix[java.lang.Long]]
  case object JIntMatrixKey    extends KeyType[GArray[Array[java.lang.Integer]]]
  case object JFloatMatrixKey  extends KeyType[GMatrix[java.lang.Float]]
  case object JDoubleMatrixKey extends KeyType[GMatrix[java.lang.Double]]

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
