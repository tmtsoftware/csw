package csw.param.parameters

import csw.param.{JavaFormats, RaDec}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.reflect.ClassTag

sealed class KeyFactory[S: JsonFormat: ClassTag](typeName: String) {
  def make(name: String): GKey[S] = new GKey[S](name, typeName)
  Formats.register[S](typeName)
}

object Keys extends DefaultJsonProtocol with JavaFormats {
  object RaDecKey  extends KeyFactory[RaDec]("RaDecKey")
  object StringKey extends KeyFactory[String]("StringKey")

  object IntKey     extends KeyFactory[Int]("IntKey")
  object BooleanKey extends KeyFactory[Boolean]("JBooleanKey")
  object CharKey    extends KeyFactory[Char]("CharKey")
  object ShortKey   extends KeyFactory[Short]("JShortKey")
  object DoubleKey  extends KeyFactory[Double]("JDoubleKey")
  object FloatKey   extends KeyFactory[Float]("JFloatKey")
  object LongKey    extends KeyFactory[Long]("JLongKey")

  object IntArrayKey    extends KeyFactory[GArray[Int]]("IntArrayKey")
  object ByteArrayKey   extends KeyFactory[GArray[Byte]]("JByteArrayKey")
  object DoubleArrayKey extends KeyFactory[GArray[Double]]("JDoubleArrayKey")
  object FloatArrayKey  extends KeyFactory[GArray[Float]]("JFloatArrayKey")
  object LongArrayKey   extends KeyFactory[GArray[Long]]("JLongArrayKey")
  object ShortArrayKey  extends KeyFactory[GArray[Short]]("JShortArrayKey")

  object IntMatrixKey extends KeyFactory[GArray[Array[Int]]]("IntMatrixKey")

  object JIntKey     extends KeyFactory[java.lang.Integer]("JIntKey")
  object JBooleanKey extends KeyFactory[java.lang.Boolean]("JBooleanKey")
  object JCharKey    extends KeyFactory[java.lang.Character]("JCharacterKey")
  object JShortKey   extends KeyFactory[java.lang.Short]("JShortKey")
  object JDoubleKey  extends KeyFactory[java.lang.Double]("JDoubleKey")
  object JFloatKey   extends KeyFactory[java.lang.Float]("JFloatKey")
  object JLongKey    extends KeyFactory[java.lang.Long]("JLongKey")

  object JIntArrayKey    extends KeyFactory[GArray[java.lang.Integer]]("JIntegerArrayKey")
  object JByteArrayKey   extends KeyFactory[GArray[java.lang.Byte]]("JByteArrayKey")
  object JDoubleArrayKey extends KeyFactory[GArray[java.lang.Double]]("JDoubleArrayKey")
  object JFloatArrayKey  extends KeyFactory[GArray[java.lang.Float]]("JFloatArrayKey")
  object JLongArrayKey   extends KeyFactory[GArray[java.lang.Long]]("JLongArrayKey")
  object JShortArrayKey  extends KeyFactory[GArray[java.lang.Short]]("JShortArrayKey")

  object JIntMatrixKey extends KeyFactory[GArray[Array[java.lang.Integer]]]("JIntegerMatrixKey")
}

object JKeys {
  val IntKey      = Keys.JIntKey
  val BooleanKey  = Keys.JBooleanKey
  val IntArrayKey = Keys.JIntArrayKey
}
