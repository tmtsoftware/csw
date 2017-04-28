package csw.services.location.commons

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, DurationDouble, DurationInt}

private[commons] object BlockingUtils {

  def poll(predicate: ⇒ Boolean, max: Duration = 5.seconds): Boolean = {
    def now  = System.nanoTime.nanos
    val stop = now + max

    @tailrec
    def loop(): Boolean =
      if (predicate || now > stop) {
        predicate
      } else {
        Thread.sleep(100)
        loop()
      }

    loop()
  }

}
