package csw.time.client.extensions

import java.time.Instant

object RichInstant {

  implicit class RichInstant(val instant: Instant) extends AnyVal {
    def formatNanos(precision: Int): String = {
      val nanos = instant.getNano
      val d     = Math.pow(10, 9 - precision).toInt
      (nanos / d).toString
    }
  }

}
