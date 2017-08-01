package csw.param.parameters

import csw.param.{JavaFormats, RaDec}
import enumeratum.{Enum, EnumEntry}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.collection.immutable
import scala.reflect.ClassTag

sealed class KeyFactory[S: JsonFormat: ClassTag] extends EnumEntry {
  def make(name: String): GKey[S] = new GKey[S](name, entryName)
  Formats.register[S](entryName)
}

object Keys extends DefaultJsonProtocol with JavaFormats with Enum[KeyFactory[_]] {

  override def values: immutable.IndexedSeq[KeyFactory[_]] = findValues

  object RaDecKey  extends KeyFactory[RaDec]
  object StringKey extends KeyFactory[String]

  object IntKey     extends KeyFactory[Int]
  object BooleanKey extends KeyFactory[Boolean]
  object CharKey    extends KeyFactory[Char]
  object ShortKey   extends KeyFactory[Short]
  object DoubleKey  extends KeyFactory[Double]
  object FloatKey   extends KeyFactory[Float]
  object LongKey    extends KeyFactory[Long]

  object IntArrayKey    extends KeyFactory[GArray[Int]]
  object ByteArrayKey   extends KeyFactory[GArray[Byte]]
  object DoubleArrayKey extends KeyFactory[GArray[Double]]
  object FloatArrayKey  extends KeyFactory[GArray[Float]]
  object LongArrayKey   extends KeyFactory[GArray[Long]]
  object ShortArrayKey  extends KeyFactory[GArray[Short]]

  object IntMatrixKey extends KeyFactory[GArray[Array[Int]]]

  object JIntKey     extends KeyFactory[java.lang.Integer]
  object JBooleanKey extends KeyFactory[java.lang.Boolean]
  object JCharKey    extends KeyFactory[java.lang.Character]
  object JShortKey   extends KeyFactory[java.lang.Short]
  object JDoubleKey  extends KeyFactory[java.lang.Double]
  object JFloatKey   extends KeyFactory[java.lang.Float]
  object JLongKey    extends KeyFactory[java.lang.Long]

  object JIntArrayKey    extends KeyFactory[GArray[java.lang.Integer]]
  object JByteArrayKey   extends KeyFactory[GArray[java.lang.Byte]]
  object JDoubleArrayKey extends KeyFactory[GArray[java.lang.Double]]
  object JFloatArrayKey  extends KeyFactory[GArray[java.lang.Float]]
  object JLongArrayKey   extends KeyFactory[GArray[java.lang.Long]]
  object JShortArrayKey  extends KeyFactory[GArray[java.lang.Short]]

  object JIntMatrixKey extends KeyFactory[GArray[Array[java.lang.Integer]]]
}

object JKeys {
  val IntKey      = Keys.JIntKey
  val BooleanKey  = Keys.JBooleanKey
  val IntArrayKey = Keys.JIntArrayKey
}
