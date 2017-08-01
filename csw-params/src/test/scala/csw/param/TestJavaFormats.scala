package csw.param

import csw.param.parameters.Parameter
import spray.json.JsonFormat
import java.lang

import csw.param.formats.JavaFormats

import scala.reflect.ClassTag

object TestJavaFormats extends JavaFormats {
  def paramFormat[T: JsonFormat: ClassTag]: JsonFormat[Parameter[T]] = implicitly[JsonFormat[Parameter[T]]]
  val dd: JsonFormat[Parameter[lang.Boolean]]                        = paramFormat[lang.Boolean]
}
