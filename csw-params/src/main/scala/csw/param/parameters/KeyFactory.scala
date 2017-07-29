package csw.param.parameters

import csw.param.{JavaFormats, RaDec}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.reflect.{classTag, ClassTag}

sealed class KeyFactory[S: JsonFormat: ClassTag] {
  def make(name: String): GKey[S] = new GKey[S](name, typeName)

  // typeName should be unique for each GKey
  private val typeName: String = classTag[S].runtimeClass.getSimpleName

  Formats.register[S](typeName)
}

object Keys extends DefaultJsonProtocol {
  val RaDecKey   = new KeyFactory[RaDec]
  val IntegerKey = new KeyFactory[Int]
  val BooleanKey = new KeyFactory[Boolean]
}

object JKeys extends JavaFormats {
  val IntegerKey = new KeyFactory[java.lang.Integer]
  val BooleanKey = new KeyFactory[java.lang.Boolean]
}
