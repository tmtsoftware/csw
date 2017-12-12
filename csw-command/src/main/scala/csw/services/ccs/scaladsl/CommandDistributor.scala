package csw.services.ccs.scaladsl

import akka.actor.Scheduler
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import akka.util.Timeout
import csw.messages.CommandResponseManagerMessage.Subscribe
import csw.messages.ComponentMessage
import csw.messages.ccs.commands.CommandResponse.Accepted
import csw.messages.ccs.commands.{CommandResponse, CommandResultType, ControlCommand}
import csw.services.ccs.common.ActorRefExts.RichComponentActor

import scala.concurrent.{ExecutionContext, Future}

class CommandDistributor(controlCommand: ControlCommand) {

  var commandToComponent: Map[ActorRef[ComponentMessage], ControlCommand] = _

  def addSubCommand(componentRef: ActorRef[ComponentMessage], controlCommand: ControlCommand): CommandDistributor = {
    commandToComponent = commandToComponent + (componentRef → controlCommand)
    this
  }

  def addSubCommand(subCommands: Map[ActorRef[ComponentMessage], ControlCommand]): CommandDistributor = {
    commandToComponent = commandToComponent ++ subCommands
    this
  }

  def execute()(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext): Future[CommandResponse] = {
    var finalResponse: CommandResponse = CommandResponse.Completed(controlCommand.runId)

    Source
      .actorRef[CommandResponse](256, OverflowStrategy.fail)
      .mapMaterializedValue { ref ⇒
        commandToComponent.foreach { kv ⇒
          kv._1.submit(kv._2).map {
            case _: Accepted ⇒ kv._1 ! Subscribe(kv._2.runId, ref)
            case response    ⇒ response
          }
        }
      }
      .to(Sink.foreach { x ⇒
        finalResponse = x
        if (x.resultType == CommandResultType.Negative)
          throw new RuntimeException("")
      })

    Future.successful(finalResponse)
  }
}
