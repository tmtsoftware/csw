package csw.messages.params.pb

import csw.messages.params.formats.{JavaFormats, JsonSupport, WrappedArrayProtocol}
import csw.messages.params.generics.Parameter
import play.api.libs.json.Format

import scala.reflect.ClassTag

object TestJavaFormats extends JsonSupport with JavaFormats with WrappedArrayProtocol {
  def paramFormat[T: Format: ClassTag: ItemsFactory]: Format[Parameter[T]] = implicitly[Format[Parameter[T]]]
}
