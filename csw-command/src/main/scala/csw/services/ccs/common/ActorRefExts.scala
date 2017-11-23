package csw.services.ccs.common

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.Future

object ActorRefExts {
  implicit class RichActor[A](val ref: ActorRef[A]) extends AnyVal {
    def ask[B](f: ActorRef[B] ⇒ A)(implicit timeout: Timeout, scheduler: Scheduler): Future[B] = ref ? f
  }
}
