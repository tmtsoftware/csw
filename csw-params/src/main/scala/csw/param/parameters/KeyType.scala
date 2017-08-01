package csw.param.parameters

import csw.param.{JavaFormats, RaDec}
import enumeratum.{Enum, EnumEntry}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.collection.immutable
import scala.reflect.ClassTag

sealed class KeyType[S](implicit @transient jsFormat: JsonFormat[S], @transient clsTag: ClassTag[S])
    extends EnumEntry
    with Serializable {

  def make(name: String): GKey[S]        = new GKey[S](name, this)
  def paramFormat: JsonFormat[GParam[S]] = GParam[S]
}

object KeyType extends DefaultJsonProtocol with JavaFormats with Enum[KeyType[_]] {

  override def values: immutable.IndexedSeq[KeyType[_]] = findValues

  case object RaDecKey  extends KeyType[RaDec]
  case object StringKey extends KeyType[String]

  case object IntKey     extends KeyType[Int]
  case object BooleanKey extends KeyType[Boolean]
  case object CharKey    extends KeyType[Char]
  case object ShortKey   extends KeyType[Short]
  case object DoubleKey  extends KeyType[Double]
  case object FloatKey   extends KeyType[Float]
  case object LongKey    extends KeyType[Long]

  case object IntArrayKey    extends KeyType[GArray[Int]]
  case object ByteArrayKey   extends KeyType[GArray[Byte]]
  case object DoubleArrayKey extends KeyType[GArray[Double]]
  case object FloatArrayKey  extends KeyType[GArray[Float]]
  case object LongArrayKey   extends KeyType[GArray[Long]]
  case object ShortArrayKey  extends KeyType[GArray[Short]]

  case object IntMatrixKey    extends KeyType[GMatrix[Int]]
  case object ByteMatrixKey   extends KeyType[GMatrix[Byte]]
  case object DoubleMatrixKey extends KeyType[GMatrix[Double]]
  case object ShortMatrixKey  extends KeyType[GMatrix[Short]]

  case object JIntKey     extends KeyType[java.lang.Integer]
  case object JBooleanKey extends KeyType[java.lang.Boolean]
  case object JCharKey    extends KeyType[java.lang.Character]
  case object JShortKey   extends KeyType[java.lang.Short]
  case object JDoubleKey  extends KeyType[java.lang.Double]
  case object JFloatKey   extends KeyType[java.lang.Float]
  case object JLongKey    extends KeyType[java.lang.Long]

  case object JIntArrayKey    extends KeyType[GArray[java.lang.Integer]]
  case object JByteArrayKey   extends KeyType[GArray[java.lang.Byte]]
  case object JDoubleArrayKey extends KeyType[GArray[java.lang.Double]]
  case object JFloatArrayKey  extends KeyType[GArray[java.lang.Float]]
  case object JLongArrayKey   extends KeyType[GArray[java.lang.Long]]
  case object JShortArrayKey  extends KeyType[GArray[java.lang.Short]]

  case object JIntMatrixKey    extends KeyType[GArray[Array[java.lang.Integer]]]
  case object JByteMatrixKey   extends KeyType[GArray[Array[java.lang.Byte]]]
  case object JDoubleMatrixKey extends KeyType[GMatrix[java.lang.Double]]
  case object JShortMatrixKey  extends KeyType[GMatrix[java.lang.Short]]

  implicit def format[T]: JsonFormat[KeyType[T]] = EnumJsonSupport.format[KeyType, T](this)
}

object JKeyTypes {
  val IntKey      = KeyType.JIntKey
  val BooleanKey  = KeyType.JBooleanKey
  val IntArrayKey = KeyType.JIntArrayKey
}
