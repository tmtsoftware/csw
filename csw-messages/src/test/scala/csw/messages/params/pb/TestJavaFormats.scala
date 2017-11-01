package csw.messages.params.pb

import csw.messages.params.formats.{EnumJsonSupport, JavaFormats, JsonSupport, WrappedArrayProtocol}
import csw.messages.params.generics.Parameter
import play.api.libs.json.Format

import scala.reflect.ClassTag

object TestJavaFormats extends JsonSupport with JavaFormats with EnumJsonSupport with WrappedArrayProtocol {
  def paramFormat[T: Format: ClassTag: ItemsFactory]: Format[Parameter[T]] = implicitly[Format[Parameter[T]]]
}
