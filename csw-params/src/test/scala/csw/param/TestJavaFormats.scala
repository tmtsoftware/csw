package csw.param

import csw.param.parameters.GParam
import spray.json.JsonFormat
import java.lang

import scala.reflect.ClassTag

object TestJavaFormats extends JavaFormats {
  def paramFormat[T: JsonFormat: ClassTag]: JsonFormat[GParam[T]] = implicitly[JsonFormat[GParam[T]]]
  val dd: JsonFormat[GParam[lang.Boolean]]                        = paramFormat[lang.Boolean]
}
