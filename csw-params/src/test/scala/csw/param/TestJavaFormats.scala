package csw.param

import csw.param.generics.Parameter
import spray.json.JsonFormat
import java.lang

import csw.param.formats.JsonSupport

import scala.reflect.ClassTag

object TestJavaFormats extends JsonSupport {
  def paramFormat[T: JsonFormat: ClassTag]: JsonFormat[Parameter[T]] = implicitly[JsonFormat[Parameter[T]]]
  val dd: JsonFormat[Parameter[lang.Boolean]]                        = paramFormat[lang.Boolean]
}
