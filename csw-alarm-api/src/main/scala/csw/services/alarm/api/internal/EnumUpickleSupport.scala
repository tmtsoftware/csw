package csw.services.alarm.api.internal

import enumeratum.{Enum, EnumEntry}
import upickle.default.{ReadWriter => RW}

private[alarm] object EnumUpickleSupport {
  implicit def enumFormat[T <: EnumEntry: Enum]: RW[T] =
    upickle.default
      .readwriter[String]
      .bimap[T](
        _.entryName,
        implicitly[Enum[T]].withNameInsensitive
      )
}
