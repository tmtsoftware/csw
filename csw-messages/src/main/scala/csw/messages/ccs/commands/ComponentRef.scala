package csw.messages.ccs.commands

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.scaladsl.Source
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

  def submitAll(
      controlCommands: Set[ControlCommand]
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext): Source[CommandResponse, NotUsed] = {
    Source(controlCommands).mapAsyncUnordered(10)(this.submit)
  }

  def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
    value ? (Oneway(controlCommand, _))

  def subscribe(commandRunId: RunId)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
    value ? (CommandResponseManagerMessage.Subscribe(commandRunId, _))

  def submitAndSubscribe(
      controlCommand: ControlCommand
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext): Future[CommandResponse] =
    submit(controlCommand).flatMap {
      case _: Accepted ⇒ subscribe(controlCommand.runId)
      case x           ⇒ Future.successful(x)
    }

  def submitAllAndSubscribe(
      controlCommands: Set[ControlCommand]
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext): Source[CommandResponse, NotUsed] = {
    Source(controlCommands).mapAsyncUnordered(10)(this.submitAndSubscribe)
  }

  def submitAllAndSubscribe(
      controlCommands: Set[ControlCommand]
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext, mat: Materializer): Future[CommandResponse] = {
    val value = Source(controlCommands).mapAsyncUnordered(10)(this.submitAndSubscribe)
    CommandResponse.aggregateResponse(value)
  }
}
