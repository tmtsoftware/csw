package csw.param.parameters
import csw.param.RaDec
import spray.json.JsonFormat

import scala.reflect.ClassTag
import csw.param.ParameterSetJson._

sealed class KeyFactory[S: JsonFormat: ClassTag] {
  def make(name: String): GKey[S] = GKey[S](name, typeName)

  // typeName should be unique for each GKey
  private val typeName: String = implicitly[ClassTag[S]].runtimeClass.getSimpleName

  Formats.register[S](typeName)
}

object Keys {
  val RaDecKey   = new KeyFactory[RaDec]
  val IntegerKey = new KeyFactory[Int]
  val BooleanKey = new KeyFactory[Boolean]
}

object Jkeys {
  val IntegerKey = new KeyFactory[java.lang.Integer]
  val BooleanKey = new KeyFactory[java.lang.Boolean]
}
