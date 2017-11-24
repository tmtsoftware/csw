package csw.services.ccs.common

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.{CommandResponseManagerMessage, SupervisorExternalMessage}
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.params.models.RunId

import scala.concurrent.Future

object ActorRefExts {
  implicit class RichActor[A](val ref: ActorRef[A]) extends AnyVal {
    def ask[B](f: ActorRef[B] ⇒ A)(implicit timeout: Timeout, scheduler: Scheduler): Future[B] = ref ? f
  }

  implicit class RichComponentActor(val ref: ActorRef[SupervisorExternalMessage]) extends AnyVal {
    def submit(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      ref ? (Submit(controlCommand, _))

    def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      ref ? (Oneway(controlCommand, _))

    def getCommandResponse(commandId: RunId)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      ref ? (CommandResponseManagerMessage.Subscribe(commandId, _))

  }
}
