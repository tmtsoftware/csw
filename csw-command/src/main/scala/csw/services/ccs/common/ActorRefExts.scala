package csw.services.ccs.common

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.params.models.RunId
import csw.messages.{CommandResponseManagerMessage, SupervisorExternalMessage}

import scala.concurrent.Future

object ActorRefExts {
  implicit class RichActor[A](val ref: ActorRef[A]) extends AnyVal {
    def ask[B](f: ActorRef[B] ⇒ A)(implicit timeout: Timeout, scheduler: Scheduler): Future[B] = ref ? f
  }

  implicit class RichComponentActor(val componentActor: ActorRef[SupervisorExternalMessage]) extends AnyVal {
    def submit(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (Submit(controlCommand, _))

    def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (Oneway(controlCommand, _))

    def getCommandResponse(commandRunId: RunId)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (CommandResponseManagerMessage.Subscribe(commandRunId, _))

  }
}
