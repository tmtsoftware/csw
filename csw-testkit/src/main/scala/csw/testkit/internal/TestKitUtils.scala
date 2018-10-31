package csw.testkit.internal
import akka.Done
import akka.actor.CoordinatedShutdown.{Reason, UnknownReason}
import akka.util.Timeout

import scala.concurrent.{Await, Future}

private[testkit] object TestKitUtils {

  def await[T](f: Future[T], timeout: Timeout): T       = Await.result(f, timeout.duration)
  def await1[T](f: () ⇒ Future[T], timeout: Timeout): T = Await.result(f.apply(), timeout.duration)

  def coordShutdown(f: Reason ⇒ Future[Done], timeout: Timeout): Done = await(f.apply(UnknownReason), timeout.duration)

}
