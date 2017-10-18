package csw.trombone.assembly.actors

import akka.typed.ActorRef
import csw.messages._

trait AssemblyFollowingCommandHandlers extends AssemblyCommandHandlers {
  def onFollowing(commandMessage: CommandMessage): AssemblyCommandState
  def onFollowingCommandComplete(replyTo: ActorRef[CommandResponse], result: CommandExecutionResponse): Unit
}
