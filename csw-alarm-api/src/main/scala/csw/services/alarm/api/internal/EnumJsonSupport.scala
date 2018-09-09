package csw.services.alarm.api.internal

import csw.messages.extensions.Formats
import csw.messages.extensions.Formats.MappableFormat
import enumeratum.{Enum, EnumEntry}
import play.api.libs.json.Format

private[alarm] object EnumJsonSupport {
  def formatOf[T](implicit x: Format[T]): Format[T] = x

  implicit def format[T <: EnumEntry: Enum]: Format[T] =
    Formats.of[String].bimap[T](_.entryName, implicitly[Enum[T]].withNameInsensitive)
}
