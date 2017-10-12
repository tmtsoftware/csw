package csw.trombone.assembly.actors

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.messages.PubSub.Subscribe
import csw.messages._
import csw.messages.ccs.ValidationIssue.RequiredHCDUnavailableIssue
import csw.trombone.assembly.AssemblyCommandHandlerMsgs.{CommandComplete, CommandMessageE}
import csw.trombone.assembly.CommonMsgs.AssemblyStateE
import csw.trombone.assembly._
import csw.trombone.assembly.actors.CommandExecutionState.{Executing, Following, NotFollowing}
import csw.trombone.assembly.commands.{AssemblyCommand, AssemblyState}

import scala.util.{Failure, Success}

class AssemblyCommandBehavior(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    assemblyContext: AssemblyContext,
    hcd: Option[ActorRef[SupervisorExternalMessage]],
    assemblyCommandHandlers: AssemblyCommandHandlers
) extends MutableBehavior[AssemblyCommandHandlerMsgs] {

  import ctx.executionContext
  private var mode: CommandExecutionState                   = NotFollowing
  private val assemblyStateAdapter: ActorRef[AssemblyState] = ctx.spawnAdapter(AssemblyStateE)

  assemblyCommandHandlers.tromboneStateActor ! Subscribe(assemblyStateAdapter)

  override def onMessage(msg: AssemblyCommandHandlerMsgs): Behavior[AssemblyCommandHandlerMsgs] = {
    (mode, msg) match {
      case (_, msg: CommonMsgs)                  ⇒ onCommon(msg)
      case (NotFollowing, msg: NotFollowingMsgs) ⇒ onNotFollowing(msg)
      case (Following, msg: FollowingMsgs)       ⇒ onFollowing(msg)
      case (Executing, msg: ExecutingMsgs)       ⇒ onExecuting(msg)
    }
    this
  }

  def onCommon(msg: CommonMsgs): Unit = msg match {
    case AssemblyStateE(state) => assemblyCommandHandlers.currentState = state
  }

  def onNotFollowing(msg: NotFollowingMsgs): Unit = msg match {
    case CommandMessageE(commandMessage) =>
      val assemblyCommandState = assemblyCommandHandlers.onNotFollowing(commandMessage)
      executeCommand(assemblyCommandState.mayBeAssemblyCommand.get, commandMessage.replyTo)
  }

  def onFollowing(msg: FollowingMsgs): Unit = msg match {
    case CommandMessageE(commandMessage) =>
      val assemblyCommandState = assemblyCommandHandlers.onFollowing(commandMessage)
      executeFollow(assemblyCommandState.mayBeAssemblyCommand.get, commandMessage.replyTo)
    case CommandComplete(replyTo, result) => assemblyCommandHandlers.onFollowingCommandComplete(replyTo, result)
  }

  def onExecuting(msg: ExecutingMsgs): Unit = msg match {
    case CommandMessageE(commandMessage) =>
      val assemblyCommandState = assemblyCommandHandlers.onExecuting(commandMessage)
      executeCommand(assemblyCommandState.mayBeAssemblyCommand.get, commandMessage.replyTo)
    case CommandComplete(replyTo, result) =>
      assemblyCommandHandlers.onExecutingCommandComplete(replyTo, result)
      mode = CommandExecutionState.NotFollowing
  }

  private def executeCommand(assemblyCommand: AssemblyCommand, replyTo: ActorRef[CommandResponse]): Unit = {
    if (hcd.isDefined) {
      mode = CommandExecutionState.Executing
      assemblyCommand.startCommand().onComplete {
        case Success(result) ⇒ ctx.self ! CommandComplete(replyTo, result)
        case Failure(ex)     ⇒ throw ex // replace with sending a failed message to self
      }
    } else hcdNotAvailableResponse(Some(replyTo))
  }

  private def executeFollow(assemblyCommand: AssemblyCommand, replyTo: ActorRef[CommandResponse]): Unit = {
    mode = CommandExecutionState.Following
    assemblyCommand.startCommand().onComplete {
      case Success(result) ⇒ ctx.self ! CommandComplete(replyTo, result)
      case Failure(ex)     ⇒ throw ex // replace with sending a failed message to self
    }
    replyTo ! BehaviorChanged[FollowingMsgs](ctx.self)
  }

  private def hcdNotAvailableResponse(commandOriginator: Option[ActorRef[CommandExecutionResponse]]): Unit = {
    commandOriginator.foreach(
      _ ! NoLongerValid(RequiredHCDUnavailableIssue(s"${assemblyContext.hcdComponentId} is not available"))
    )
  }
}
