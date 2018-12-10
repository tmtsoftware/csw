package csw.time.client

import csw.time.api.models.TMTTime

object TestUtil {

  def formatWithPrecision(tmtTime: TMTTime[_], precision: Int): String = {
    val nanos = tmtTime.value.getNano
    val d     = Math.pow(10, 9 - precision).toInt
    (nanos / d).toString
  }
}
