package csw.services.location.commons

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, DurationDouble, DurationInt}

object BlockingClusterUtils {

  def awaitAssert(a: ⇒ Boolean, max: Duration = 5.seconds): Unit = {
    val interval = 100.millis
    def now      = System.nanoTime.nanos
    val stop     = now + max

    @tailrec
    def poll(t: Duration): Unit =
      if (!a) {
        Thread.sleep(t.toMillis)
        poll((stop - now) min interval)
      }

    poll(max min interval)
  }

}
