package csw.time.client.extensions

import java.time.Instant

class RichInstant {

  def formatNanos(precision: Int, instant: Instant): String = {
    val nanos = instant.getNano
    val d     = Math.pow(10, 9 - precision).toInt
    (nanos / d).toString
  }
}
