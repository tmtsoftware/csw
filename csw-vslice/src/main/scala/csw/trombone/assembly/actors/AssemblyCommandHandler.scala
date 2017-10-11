package csw.trombone.assembly.actors

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor.MutableBehavior
import csw.messages.SupervisorExternalMessage
import csw.trombone.assembly.actors.CommandExecutionState.{Executing, Following, NotFollowing}
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, AssemblyContext}

class AssemblyCommandHandler(
    assemblyContext: AssemblyContext,
    tromboneHCDIn: Option[ActorRef[SupervisorExternalMessage]]
) extends MutableBehavior[AssemblyCommandHandlerMsgs] {

  val mode: CommandExecutionState = NotFollowing

  def onNotFollowing() = ???

  def onFollowing() = ???

  def onExecuting() = ???

  override def onMessage(msg: AssemblyCommandHandlerMsgs): Behavior[AssemblyCommandHandlerMsgs] = (mode, msg) match {
    case (NotFollowing, _) ⇒ onNotFollowing()
    case (Following, _)    ⇒ onFollowing()
    case (Executing, _)    ⇒ onExecuting()
  }
}
