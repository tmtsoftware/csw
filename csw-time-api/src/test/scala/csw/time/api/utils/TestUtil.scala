package csw.time.api.utils
import java.time.Instant

object TestUtil {

  def formatWithPrecision(tmtTime: Instant, precision: Int): String = {
    val nanos = tmtTime.getNano
    val d     = Math.pow(10, 9 - precision).toInt
    (nanos / d).toString
  }
}
