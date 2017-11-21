package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages.ccs.commands.CommandResponse
import csw.messages.models.PubSub
import csw.messages.models.PubSub.Publish
import csw.trombone.assembly.AssemblyCommandHandlerMsgs

import scala.concurrent.Future

abstract class AssemblyCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    startState: AssemblyState,
    stateActor: ActorRef[PubSub[AssemblyState]]
) {

  def startCommand(): Future[CommandResponse]
  def stopCommand(): Unit

  final def publishState(assemblyState: AssemblyState): Unit =
    stateActor ! Publish(assemblyState)
}
