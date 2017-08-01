package csw.param.parameters

import csw.param.{JavaFormats, RaDec}
import enumeratum.{Enum, EnumEntry}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.collection.immutable
import scala.reflect.ClassTag

sealed class KeyType[S: JsonFormat: ClassTag] extends EnumEntry {
  def make(name: String): GKey[S] = new GKey[S](name, entryName)
  Formats.register[S](entryName)
}

object KeyType extends DefaultJsonProtocol with JavaFormats with Enum[KeyType[_]] {

  override def values: immutable.IndexedSeq[KeyType[_]] = findValues

  object RaDecKey  extends KeyType[RaDec]
  object StringKey extends KeyType[String]

  object IntKey     extends KeyType[Int]
  object BooleanKey extends KeyType[Boolean]
  object CharKey    extends KeyType[Char]
  object ShortKey   extends KeyType[Short]
  object DoubleKey  extends KeyType[Double]
  object FloatKey   extends KeyType[Float]
  object LongKey    extends KeyType[Long]

  object IntArrayKey    extends KeyType[GArray[Int]]
  object ByteArrayKey   extends KeyType[GArray[Byte]]
  object DoubleArrayKey extends KeyType[GArray[Double]]
  object FloatArrayKey  extends KeyType[GArray[Float]]
  object LongArrayKey   extends KeyType[GArray[Long]]
  object ShortArrayKey  extends KeyType[GArray[Short]]

  object IntMatrixKey extends KeyType[GArray[Array[Int]]]

  object JIntKey     extends KeyType[java.lang.Integer]
  object JBooleanKey extends KeyType[java.lang.Boolean]
  object JCharKey    extends KeyType[java.lang.Character]
  object JShortKey   extends KeyType[java.lang.Short]
  object JDoubleKey  extends KeyType[java.lang.Double]
  object JFloatKey   extends KeyType[java.lang.Float]
  object JLongKey    extends KeyType[java.lang.Long]

  object JIntArrayKey    extends KeyType[GArray[java.lang.Integer]]
  object JByteArrayKey   extends KeyType[GArray[java.lang.Byte]]
  object JDoubleArrayKey extends KeyType[GArray[java.lang.Double]]
  object JFloatArrayKey  extends KeyType[GArray[java.lang.Float]]
  object JLongArrayKey   extends KeyType[GArray[java.lang.Long]]
  object JShortArrayKey  extends KeyType[GArray[java.lang.Short]]

  object JIntMatrixKey extends KeyType[GArray[Array[java.lang.Integer]]]

  implicit val format: JsonFormat[KeyType[_]] = EnumJsonSupport.format(this)
}

object JKeyTypes {
  val IntKey      = KeyType.JIntKey
  val BooleanKey  = KeyType.JBooleanKey
  val IntArrayKey = KeyType.JIntArrayKey
}
