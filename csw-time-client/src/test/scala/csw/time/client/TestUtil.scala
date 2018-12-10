package csw.time.client

import csw.time.api.models.TMTTime

class TestUtil {

  def formatWithPrecision(tmtTime: TMTTime, precision: Int): String = {
    val nanos = tmtTime.value.getNano
    val d     = Math.pow(10, 9 - precision).toInt
    (nanos / d).toString
  }
}
