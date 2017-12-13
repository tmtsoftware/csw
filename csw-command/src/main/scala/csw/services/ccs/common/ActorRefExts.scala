package csw.services.ccs.common

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.ccs.commands.CommandResponse.Accepted
import csw.messages.ccs.commands.{CommandResponse, CommandResultType, ControlCommand}
import csw.messages.params.models.RunId
import csw.messages.{CommandResponseManagerMessage, ComponentMessage}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ActorRefExts {
  implicit class RichComponentActor(val componentActor: ActorRef[ComponentMessage]) extends AnyVal {
    def submit(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (Submit(controlCommand, _))

    def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (Oneway(controlCommand, _))

    def getCommandResponse(commandRunId: RunId)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
      componentActor ? (CommandResponseManagerMessage.Subscribe(commandRunId, _))

    def submitAndGetCommandResponse(
        controlCommand: ControlCommand
    )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext): Future[CommandResponse] =
      submit(controlCommand).flatMap {
        case _: Accepted ⇒ getCommandResponse(controlCommand.runId)
        case x           ⇒ Future.successful(x)
      }

    def submitManyAndGetCommandResponse(
        controlCommands: Set[ControlCommand]
    )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext, mat: Materializer): Future[CommandResponse] = {

      val source: Source[CommandResponse, NotUsed] = Source(controlCommands.toList)
        .mapAsyncUnordered(10) { command ⇒
          componentActor.submitAndGetCommandResponse(command)
        }
        .map {
          case x if x.resultType == CommandResultType.Negative ⇒ throw new RuntimeException
        }

      source.runWith(Sink.ignore).transform {
        case Success(_)  ⇒ Success(CommandResponse.Completed(RunId()))
        case Failure(ex) ⇒ Success(CommandResponse.Error(RunId(), s"One of the command failed : ${ex.getMessage}"))
      }
    }
  }
}
