package csw.services.ccs.scaladsl

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.typed.ActorRef
import akka.util.Timeout
import csw.messages.ComponentMessage
import csw.messages.ccs.commands.ActorRefExts.RichComponentActor
import csw.messages.ccs.commands.{CommandResponse, CommandResultType, ControlCommand}
import csw.messages.params.models.RunId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class CommandDistributor(componentToCommands: Map[ActorRef[ComponentMessage], List[ControlCommand]] = Map.empty) {

  def addSubCommand(componentRef: ActorRef[ComponentMessage], controlCommand: ControlCommand): CommandDistributor = {
    componentToCommands.get(componentRef) match {
      case Some(commands) =>
        this.copy(componentToCommands + (componentRef → (controlCommand :: commands)))
      case None => this.copy(componentToCommands + (componentRef → List(controlCommand)))
    }
  }

  def execute()(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): Future[CommandResponse] = {

    val componentToCommand: Map[ActorRef[ComponentMessage], ControlCommand] = componentToCommands.flatMap {
      case (component, commands) ⇒ commands.map(component → _)
    }

    val source: Source[CommandResponse, NotUsed] = Source(componentToCommand)
      .mapAsyncUnordered(10) {
        case (component, command) ⇒ component.submitAndGetCommandResponse(command)
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
