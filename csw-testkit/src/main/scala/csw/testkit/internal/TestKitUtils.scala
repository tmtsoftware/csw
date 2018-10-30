package csw.testkit.internal
import akka.Done
import akka.actor.CoordinatedShutdown.{Reason, UnknownReason}
import akka.util.Timeout

import scala.concurrent.{Await, Future}

private[csw] object TestKitUtils {

  def await[T](f: () ⇒ Future[T], timeout: Timeout): T = Await.result(f.apply(), timeout.duration)

  def coordShutdown(f: Reason ⇒ Future[Done], timeout: Timeout): Done = await(() ⇒ f.apply(UnknownReason), timeout.duration)

}
