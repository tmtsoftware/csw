package csw.services.ccs.common

import akka.actor.Scheduler
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.messages.ActorTypes.ComponentRef
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommandResponseManagerMessage
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.params.models.RunId

import scala.concurrent.Future

object ActorRefExts {
  implicit class RichComponentActor(val componentActor: ComponentRef) extends AnyVal {
    def submit(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (Submit(controlCommand, _))

    def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (Oneway(controlCommand, _))

    def getCommandResponse(commandRunId: RunId)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (CommandResponseManagerMessage.Subscribe(commandRunId, _))
  }
}
