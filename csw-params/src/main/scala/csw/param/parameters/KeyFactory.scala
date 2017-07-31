package csw.param.parameters

import csw.param.{JavaFormats, RaDec}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.reflect.ClassTag

sealed class KeyFactory[S: JsonFormat: ClassTag](typeName: String) {
  def make(name: String): GKey[S] = new GKey[S](name, typeName)
  Formats.register[S](typeName)
}

object Keys extends DefaultJsonProtocol {
  val RaDecKey     = new KeyFactory[RaDec]("RaDecKey")
  val IntKey       = new KeyFactory[Int]("IntKey")
  val IntArrayKey  = new KeyFactory[GArray[Int]]("IntArrayKey")
  val IntMatrixKey = new KeyFactory[GArray[Array[Int]]]("IntMatrixKey")
  val BooleanKey   = new KeyFactory[Boolean]("BooleanKey")
}

object JKeys extends JavaFormats {
  val RaDecKey         = new KeyFactory[RaDec]("JRaDecKey")
  val IntegerKey       = new KeyFactory[java.lang.Integer]("JIntegerKey")
  val IntegerArrayKey  = new KeyFactory[GArray[java.lang.Integer]]("JIntegerArrayKey")
  val IntegerMatrixKey = new KeyFactory[GArray[Array[Int]]]("JIntegerMatrixKey")
  val BooleanKey       = new KeyFactory[java.lang.Boolean]("JBooleanKey")
}
