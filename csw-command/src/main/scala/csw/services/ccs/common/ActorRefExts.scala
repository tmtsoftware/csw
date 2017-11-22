package csw.services.ccs.common

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.Future

object ActorRefExts {
  implicit class RichActor[A](val ref: ActorRef[A]) extends AnyVal {
    def ask[B](command: A)(implicit timeout: Timeout, scheduler: Scheduler): Future[B] = ref ? execute(command)
    private def execute[B](command: A)(replyTo: ActorRef[B]): A                        = command
  }
}
