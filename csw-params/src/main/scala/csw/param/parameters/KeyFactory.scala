package csw.param.parameters

import csw.param.{JavaFormats, RaDec}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.reflect.ClassTag

sealed class KeyFactory[S: JsonFormat: ClassTag](typeName: String) {
  def make(name: String): GKey[S] = new GKey[S](name, typeName)
  Formats.register[S](typeName)
}

object Keys extends DefaultJsonProtocol {
  val RaDecKey   = new KeyFactory[RaDec]("RaDecKey")
  val IntKey     = new KeyFactory[Int]("IntKey")
  val BooleanKey = new KeyFactory[Boolean]("BooleanKey")
  val CharKey    = new KeyFactory[Char]("CharKey")
  val ShortKey   = new KeyFactory[Short]("ShortKey")
  val DoubleKey  = new KeyFactory[Double]("DoubleKey")
  val StringKey  = new KeyFactory[String]("StringKey")
  val FloatKey   = new KeyFactory[Float]("FloatKey")
  val LongKey    = new KeyFactory[Long]("LongKey")

  val IntArrayKey  = new KeyFactory[GArray[Int]]("IntArrayKey")
  val ByteArrayKey = new KeyFactory[GArray[Byte]]("ByteArrayKey")

  val IntMatrixKey = new KeyFactory[GArray[Array[Int]]]("IntMatrixKey")
}

object JKeys extends JavaFormats {
  val RaDecKey     = new KeyFactory[RaDec]("JRaDecKey")
  val IntegerKey   = new KeyFactory[java.lang.Integer]("JIntegerKey")
  val BooleanKey   = new KeyFactory[java.lang.Boolean]("JBooleanKey")
  val CharacterKey = new KeyFactory[java.lang.Character]("JCharacterKey")
  val ShortKey     = new KeyFactory[java.lang.Short]("JShortKey")
  val DoubleKey    = new KeyFactory[java.lang.Double]("JDoubleKey")
  val StringKey    = new KeyFactory[java.lang.String]("JStringKey")
  val FloatKey     = new KeyFactory[java.lang.Float]("JFloatKey")
  val LongKey      = new KeyFactory[java.lang.Long]("JLongKey")

  val IntegerArrayKey = new KeyFactory[GArray[java.lang.Integer]]("JIntegerArrayKey")
  val ByteArrayKey    = new KeyFactory[GArray[java.lang.Byte]]("JByteArrayKey")

  val IntegerMatrixKey = new KeyFactory[GArray[Array[java.lang.Integer]]]("JIntegerMatrixKey")
}
