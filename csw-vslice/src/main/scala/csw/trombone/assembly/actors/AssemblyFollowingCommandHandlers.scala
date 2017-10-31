package csw.trombone.assembly.actors

import akka.typed.ActorRef
import csw.messages._
import csw.messages.ccs.commands.{CommandExecutionResponse, CommandResponse}

trait AssemblyFollowingCommandHandlers extends AssemblyCommandHandlers {
  def onFollowing(commandMessage: CommandMessage): AssemblyCommandState
  def onFollowingCommandComplete(replyTo: ActorRef[CommandResponse], result: CommandExecutionResponse): Unit
}
