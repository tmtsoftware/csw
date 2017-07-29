package csw.param.parameters
import csw.param.RaDec
import spray.json.JsonFormat

import scala.reflect.{classTag, ClassTag}
import csw.param.JsonSupport._

sealed class KeyFactory[S: JsonFormat: ClassTag] {
  def make(name: String): GKey[S] = new GKey[S](name, typeName)

  // typeName should be unique for each GKey
  private val typeName: String = classTag[S].runtimeClass.getSimpleName

  Formats.register[S](typeName)
}

object Keys {
  val RaDecKey   = new KeyFactory[RaDec]
  val IntegerKey = new KeyFactory[Int]
  val BooleanKey = new KeyFactory[Boolean]
}

object JKeys {
  val IntegerKey = new KeyFactory[java.lang.Integer]
  val BooleanKey = new KeyFactory[java.lang.Boolean]
}
