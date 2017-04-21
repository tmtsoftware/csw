package csw.services.location.commons

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, DurationDouble, DurationInt}

private[commons] object BlockingUtils {

  def awaitAssert(predicate: ⇒ Boolean, max: Duration = 5.seconds): Unit = {
    def now      = System.nanoTime.nanos
    val stop     = now + max
    val interval = 100.millis

    @tailrec
    def poll(t: Duration): Unit =
      if (!predicate) {
        Thread.sleep(t.toMillis)
        poll((stop - now) min interval)
      }

    poll(max min interval)
  }

}
