package csw.param.parameters

import csw.param.{JavaFormats, RaDec}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.reflect.ClassTag

sealed class KeyFactory[S: JsonFormat: ClassTag](typeName: String) {
  def make(name: String): GKey[S] = new GKey[S](name, typeName)
  Formats.register[S](typeName)
}

object Keys extends DefaultJsonProtocol with JavaFormats {
  val RaDecKey  = new KeyFactory[RaDec]("RaDecKey")
  val StringKey = new KeyFactory[String]("StringKey")

  //Scala types
  val IntKey     = new KeyFactory[Int]("IntKey")
  val BooleanKey = new KeyFactory[Boolean]("JBooleanKey")
  val CharKey    = new KeyFactory[Char]("CharKey")
  val ShortKey   = new KeyFactory[Short]("JShortKey")
  val DoubleKey  = new KeyFactory[Double]("JDoubleKey")
  val FloatKey   = new KeyFactory[Float]("JFloatKey")
  val LongKey    = new KeyFactory[Long]("JLongKey")

  val IntArrayKey    = new KeyFactory[GArray[Int]]("IntArrayKey")
  val ByteArrayKey   = new KeyFactory[GArray[Byte]]("JByteArrayKey")
  val DoubleArrayKey = new KeyFactory[GArray[Double]]("JDoubleArrayKey")
  val FloatArrayKey  = new KeyFactory[GArray[Float]]("JFloatArrayKey")
  val LongArrayKey   = new KeyFactory[GArray[Long]]("JLongArrayKey")
  val ShortArrayKey  = new KeyFactory[GArray[Short]]("JShortArrayKey")

  val IntMatrixKey = new KeyFactory[GArray[Array[Int]]]("IntMatrixKey")

  //Java types
  val JIntKey     = new KeyFactory[java.lang.Integer]("JIntKey")
  val JBooleanKey = new KeyFactory[java.lang.Boolean]("JBooleanKey")
  val JCharKey    = new KeyFactory[java.lang.Character]("JCharacterKey")
  val JShortKey   = new KeyFactory[java.lang.Short]("JShortKey")
  val JDoubleKey  = new KeyFactory[java.lang.Double]("JDoubleKey")
  val JFloatKey   = new KeyFactory[java.lang.Float]("JFloatKey")
  val JLongKey    = new KeyFactory[java.lang.Long]("JLongKey")

  val JIntArrayKey    = new KeyFactory[GArray[java.lang.Integer]]("JIntegerArrayKey")
  val JByteArrayKey   = new KeyFactory[GArray[java.lang.Byte]]("JByteArrayKey")
  val JDoubleArrayKey = new KeyFactory[GArray[java.lang.Double]]("JDoubleArrayKey")
  val JFloatArrayKey  = new KeyFactory[GArray[java.lang.Float]]("JFloatArrayKey")
  val JLongArrayKey   = new KeyFactory[GArray[java.lang.Long]]("JLongArrayKey")
  val JShortArrayKey  = new KeyFactory[GArray[java.lang.Short]]("JShortArrayKey")

  val JIntMatrixKey = new KeyFactory[GArray[Array[java.lang.Integer]]]("JIntegerMatrixKey")
}
