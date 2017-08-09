package csw.param

import csw.param.generics.Parameter
import spray.json.{DefaultJsonProtocol, JsonFormat}
import java.lang

import csw.param.formats.{EnumJsonSupport, JavaFormats, JsonSupport, WrappedArrayProtocol}

import scala.reflect.ClassTag

object TestJavaFormats
    extends JsonSupport
    with DefaultJsonProtocol
    with JavaFormats
    with EnumJsonSupport
    with WrappedArrayProtocol {
  def paramFormat[T: JsonFormat: ClassTag]: JsonFormat[Parameter[T]] = implicitly[JsonFormat[Parameter[T]]]
  val dd: JsonFormat[Parameter[lang.Boolean]]                        = paramFormat[lang.Boolean]
}
