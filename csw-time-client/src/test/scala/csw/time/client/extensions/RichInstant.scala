package csw.time.client.extensions

import java.time.Instant

object RichInstant {
  implicit class RichInstant(val instant: Instant) extends AnyVal {
    def formatNanos: String = {
      val precision = 9

      val number: Int = instant.getNano
      s"%${precision}d".format(number)
    }
  }
}
