package csw.trombone.assembly.actors

import akka.typed.Behavior
import akka.typed.scaladsl.ActorContext
import csw.trombone.assembly.AssemblyCommandHandlerMsgs.{CommandComplete, CommandMessageE}
import csw.trombone.assembly._
import csw.trombone.assembly.actors.CommandExecutionState.{Executing, Following, NotFollowing}

class AssemblyFollowingCommandBehavior(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    assemblyContext: AssemblyContext,
    assemblyCommandHandlers: AssemblyFollowingCommandHandlers
) extends AssemblyCommandBehavior(ctx, assemblyContext, assemblyCommandHandlers) {

  override def onMessage(msg: AssemblyCommandHandlerMsgs): Behavior[AssemblyCommandHandlerMsgs] = {
    (commandExecutionState, msg) match {
      case (_, msg: CommonMsgs)                  ⇒ onCommon(msg)
      case (NotFollowing, msg: NotFollowingMsgs) ⇒ onNotFollowing(msg)
      case (Following, msg: FollowingMsgs)       ⇒ onFollowing(msg)
      case (Executing, msg: ExecutingMsgs)       ⇒ onExecuting(msg)
      case _                                     ⇒ println(s"Unexpected message :[$msg] received by component in lifecycle state :[$commandExecutionState]")
    }
    this
  }

  def onFollowing(msg: FollowingMsgs): Unit = msg match {
    case CommandMessageE(commandMessage) =>
      val assemblyCommandState = assemblyCommandHandlers.onFollowing(commandMessage)
      commandExecutionState = assemblyCommandState.commandExecutionState
      assemblyCommandHandlers.currentCommand = assemblyCommandState.mayBeAssemblyCommand
      assemblyCommandState.mayBeAssemblyCommand.foreach(x ⇒ x.foreach(executeCommand(_, commandMessage.replyTo)))
    case CommandComplete(replyTo, result) =>
      assemblyCommandHandlers.onFollowingCommandComplete(replyTo, result)
  }
}
