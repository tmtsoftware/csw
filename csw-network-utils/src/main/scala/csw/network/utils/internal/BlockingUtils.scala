package csw.network.utils.internal
import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, DurationDouble, DurationInt}

/**
 * Allows the caller to wait for a predicate to fulfill in a given time duration. Every 100ms the predicate is tested for fulfillment.
 * If predicate evaluates or duration elapses, caller is returned the evaluated result.
 */
private[network] object BlockingUtils {

  def poll(predicate: => Boolean, max: Duration = 5.seconds): Boolean = {
    def now  = System.nanoTime.toDouble.nanos
    val stop = now + max

    @tailrec
    def loop(): Boolean =
      if (predicate || now > stop) {
        predicate
      }
      else {
        Thread.sleep(100)
        loop()
      }

    loop()
  }

}
