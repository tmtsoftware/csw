package csw.trombone.assembly.actors

import akka.typed.ActorRef
import csw.messages._
import csw.messages.ccs.commands.{CommandResponse, WrappedComponent}
import csw.messages.location.Connection
import csw.messages.models.PubSub
import csw.trombone.assembly.commands.{AssemblyCommand, AssemblyState}

trait AssemblyCommandHandlers {
  var hcds: Map[Connection, Option[WrappedComponent]]
  var currentState: AssemblyState
  var currentCommand: Option[List[AssemblyCommand]]
  var tromboneStateActor: ActorRef[PubSub[AssemblyState]]

  def onNotFollowing(commandMessage: CommandMessage): AssemblyCommandState

  def onExecuting(commandMessage: CommandMessage): AssemblyCommandState
  def onExecutingCommandComplete(replyTo: ActorRef[CommandResponse], result: CommandResponse): Unit

}
