package csw.services.alarm.client.internal

import csw.services.alarm.api.internal._
import romaine.codec.RomaineStringCodec
import upickle.default._

object AlarmCodec extends AlarmRW {
  implicit def viaJsonCodec[A: ReadWriter]: RomaineStringCodec[A] = new RomaineStringCodec[A] {
    override def toString(x: A): String        = write(x)
    override def fromString(string: String): A = read[A](string)
  }
}
