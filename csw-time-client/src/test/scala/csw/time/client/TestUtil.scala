package csw.time.client

import java.time.Instant

class TestUtil {

  def formatWithPrecision(instant: Instant, precision: Int): String = {
    val nanos = instant.getNano
    val d     = Math.pow(10, 9 - precision).toInt
    (nanos / d).toString
  }
}
