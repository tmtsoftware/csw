package csw.trombone.assembly.actors

import akka.actor.Scheduler
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.framework.internal.pubsub.PubSubBehavior
import csw.messages.CommandMessage.Submit
import csw.messages.PubSub.Publish
import csw.messages._
import csw.messages.ccs.ValidationIssue.{UnsupportedCommandInStateIssue, WrongInternalStateIssue}
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly._
import csw.trombone.assembly.commands._

import scala.concurrent.duration.DurationInt

class TromboneAssemblyCommandBehaviorFactory extends AssmeblyCommandBehaviorFactory {
  override protected def assemblyCommandHandlers(
      ctx: ActorContext[AssemblyCommandHandlerMsgs],
      ac: AssemblyContext,
      tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
      allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ) =
    new TromboneCommandHandler(ctx, ac, tromboneHCD, allEventPublisher)
}

class TromboneCommandHandler(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    ac: AssemblyContext,
    tromboneHCDIn: Option[ActorRef[SupervisorExternalMessage]],
    allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
) extends AssemblyCommandHandlers {

  implicit val scheduler: Scheduler = ctx.system.scheduler
  import TromboneState._
  import ac._
  implicit val system: ActorSystem[Nothing] = ctx.system
  implicit val timeout                      = Timeout(5.seconds)

  private val badHCDReference = ctx.system.deadLetters

  private val tromboneHCD                                         = tromboneHCDIn.getOrElse(badHCDReference)
  private var setElevationItem                                    = naElevation(calculationConfig.defaultInitialElevation)
  private var followCommandActor: ActorRef[FollowCommandMessages] = _

  override var currentState: AssemblyState     = defaultTromboneState
  override var currentCommand: AssemblyCommand = _
  override var tromboneStateActor: ActorRef[PubSub[AssemblyState]] =
    ctx.spawnAnonymous(Actor.mutable[PubSub[AssemblyState]](ctx ⇒ new PubSubBehavior(ctx, "")))

  override def onNotFollowing(commandMessage: CommandMessage): AssemblyCommandState = commandMessage match {
    case Submit(s: Setup, replyTo) =>
      s.prefix match {
        case ac.initCK =>
          replyTo ! Completed
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)

        case ac.datumCK =>
          AssemblyCommandState(
            Some(new DatumCommand(ctx, s, tromboneHCD, currentState.asInstanceOf[TromboneState], tromboneStateActor)),
            CommandExecutionState.Executing
          )
        case ac.moveCK =>
          AssemblyCommandState(
            Some(
              new MoveCommand(ctx, ac, s, tromboneHCD, currentState.asInstanceOf[TromboneState], tromboneStateActor)
            ),
            CommandExecutionState.Executing
          )
        case ac.positionCK =>
          AssemblyCommandState(
            Some(
              new PositionCommand(ctx, ac, s, tromboneHCD, currentState.asInstanceOf[TromboneState], tromboneStateActor)
            ),
            CommandExecutionState.Executing
          )
        case ac.setElevationCK =>
          setElevationItem = s(ac.naElevationKey)
          AssemblyCommandState(
            Some(
              new SetElevationCommand(ctx,
                                      ac,
                                      s,
                                      tromboneHCD,
                                      currentState.asInstanceOf[TromboneState],
                                      tromboneStateActor)
            ),
            CommandExecutionState.Executing
          )

        case ac.followCK =>
          val nssItem = s(ac.nssInUseKey)
          followCommandActor = ctx.spawnAnonymous(
            FollowCommandActor.make(ac, setElevationItem, nssItem, Some(tromboneHCD), allEventPublisher)
          )
          AssemblyCommandState(
            Some(
              new FollowCommand(ctx, ac, s, tromboneHCD, currentState.asInstanceOf[TromboneState], tromboneStateActor)
            ),
            CommandExecutionState.Executing
          )

        case ac.stopCK =>
          replyTo ! NoLongerValid(
            WrongInternalStateIssue("Trombone assembly must be executing a command to use stop")
          )
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)

        case ac.setAngleCK =>
          replyTo ! NoLongerValid(WrongInternalStateIssue("Trombone assembly must be following for setAngle"))
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)

        case otherCommand =>
          replyTo ! Invalid(
            UnsupportedCommandInStateIssue(
              s"""Trombone assembly does not support the command \"${otherCommand.prefix}\" in the current state."""
            )
          )
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)
      }
  }

  override def onFollowing(commandMessage: CommandMessage): AssemblyCommandState = commandMessage match {
    case Submit(s: Setup, replyTo) =>
      s.prefix match {
        case ac.datumCK | ac.moveCK | ac.positionCK | ac.followCK | ac.setElevationCK =>
          replyTo ! Invalid(
            WrongInternalStateIssue(
              "Trombone assembly cannot be following for datum, move, position, setElevation, and follow"
            )
          )
          AssemblyCommandState(None, CommandExecutionState.Following)

        case ac.setAngleCK =>
          AssemblyCommandState(
            Some(
              new SetAngleCommand(ctx,
                                  ac,
                                  s,
                                  followCommandActor,
                                  tromboneHCD,
                                  currentState.asInstanceOf[TromboneState],
                                  tromboneStateActor)
            ),
            CommandExecutionState.Following
          )

        case ac.stopCK =>
          currentCommand.stopCommand()
          tromboneStateActor ! Publish(
            TromboneState(cmdItem(cmdReady),
                          moveItem(moveIndexed),
                          currentState.asInstanceOf[TromboneState].sodiumLayer,
                          currentState.asInstanceOf[TromboneState].nss)
          )
          replyTo ! Completed
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)

        case other =>
          println(s"Unknown config key: $commandMessage")
          AssemblyCommandState(None, CommandExecutionState.Following)
      }
  }

  override def onExecuting(commandMessage: CommandMessage): AssemblyCommandState = commandMessage match {
    case Submit(Setup(ac.commandInfo, ac.stopCK, _), replyTo) =>
      currentCommand.stopCommand()
      replyTo ! Cancelled
      AssemblyCommandState(None, CommandExecutionState.NotFollowing)

    case x =>
      println(s"TromboneCommandHandler:actorExecutingReceive received an unknown message: $x")
      AssemblyCommandState(None, CommandExecutionState.Executing)
  }

  override def onFollowingCommandComplete(replyTo: ActorRef[CommandResponse], result: CommandExecutionResponse): Unit =
    replyTo ! result

  override def onExecutingCommandComplete(replyTo: ActorRef[CommandResponse],
                                          result: CommandExecutionResponse): Unit = {
    replyTo ! result
    currentCommand.stopCommand()
  }

}
