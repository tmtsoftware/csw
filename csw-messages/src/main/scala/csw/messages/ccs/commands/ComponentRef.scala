package csw.messages.ccs.commands

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.ccs.commands.CommandResponse.Accepted
import csw.messages.params.models.RunId
import csw.messages.{CommandResponseManagerMessage, ComponentMessage}

import scala.concurrent.{ExecutionContext, Future}

case class ComponentRef(value: ActorRef[ComponentMessage]) {
  def submit(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
    value ? (Submit(controlCommand, _))

  def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
    value ? (Oneway(controlCommand, _))

  def getCommandResponse(commandRunId: RunId)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
    value ? (CommandResponseManagerMessage.Subscribe(commandRunId, _))

  def submitAndGetCommandResponse(
      controlCommand: ControlCommand
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext): Future[CommandResponse] =
    submit(controlCommand).flatMap {
      case _: Accepted ⇒ getCommandResponse(controlCommand.runId)
      case x           ⇒ Future.successful(x)
    }

  /*def submitManyAndGetCommandResponse(
        controlCommands: Set[ControlCommand]
    )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext, mat: Materializer): Future[CommandResponse] = {
      CommandDistributor(Map(componentActor → controlCommands.toList)).execute()
    }*/
}
