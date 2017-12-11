package csw.trombone.assembly.actors

import akka.actor.Scheduler
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.framework.internal.pubsub.PubSubBehavior
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.CommandIssue.{UnsupportedCommandInStateIssue, WrongInternalStateIssue}
import csw.messages.ccs.commands.CommandResponse.{Cancelled, Completed, Invalid, NoLongerValid}
import csw.messages.ccs.commands.{CommandResponse, Setup}
import csw.messages.location.Connection
import csw.messages.models.PubSub
import csw.messages.models.PubSub.Publish
import csw.services.logging.scaladsl.LoggerFactory
import csw.trombone.assembly._
import csw.trombone.assembly.commands._

import scala.concurrent.duration.DurationInt

class TromboneAssemblyCommandBehaviorFactory extends AssemblyCommandBehaviorFactory {
  override protected def assemblyCommandHandlers(
      ctx: ActorContext[AssemblyCommandHandlerMsgs],
      ac: AssemblyContext,
      tromboneHCDs: Map[Connection, Option[ActorRef[ComponentMessage]]],
      allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ): TromboneCommandHandler =
    new TromboneCommandHandler(ctx, ac, tromboneHCDs, allEventPublisher)
}

class TromboneCommandHandler(ctx: ActorContext[AssemblyCommandHandlerMsgs],
                             ac: AssemblyContext,
                             tromboneHCDs: Map[Connection, Option[ActorRef[ComponentMessage]]],
                             allEventPublisher: Option[ActorRef[TrombonePublisherMsg]])
    extends AssemblyFollowingCommandHandlers {

  implicit val scheduler: Scheduler = ctx.system.scheduler
  import TromboneState._
  import ac._
  implicit val system: ActorSystem[Nothing] = ctx.system
  implicit val timeout: Timeout             = Timeout(5.seconds)

  private var setElevationItem                                    = naElevation(calculationConfig.defaultInitialElevation)
  private var followCommandActor: ActorRef[FollowCommandMessages] = _

  override var hcds: Map[Connection, Option[ActorRef[ComponentMessage]]] = tromboneHCDs
  override var currentState: AssemblyState                               = defaultTromboneState
  override var currentCommand: Option[List[AssemblyCommand]]             = _
  override var tromboneStateActor: ActorRef[PubSub[AssemblyState]] =
    ctx.spawnAnonymous(Actor.mutable[PubSub[AssemblyState]](ctx ⇒ new PubSubBehavior(ctx, new LoggerFactory(""))))

  override def onNotFollowing(commandMessage: CommandMessage): AssemblyCommandState = commandMessage match {
    case Submit(s: Setup, replyTo) =>
      s.target match {
        case ac.initCK =>
          replyTo ! Completed(s.runId)
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)

        case ac.datumCK =>
          AssemblyCommandState(
            Some(
              List(
                new DatumCommand(ctx, ac, s, hcds.head._2, currentState.asInstanceOf[TromboneState], tromboneStateActor)
              )
            ),
            CommandExecutionState.Executing
          )
        case ac.moveCK =>
          AssemblyCommandState(
            Some(
              List(
                new MoveCommand(ctx, ac, s, hcds.head._2, currentState.asInstanceOf[TromboneState], tromboneStateActor)
              )
            ),
            CommandExecutionState.Executing
          )
        case ac.positionCK =>
          AssemblyCommandState(
            Some(
              List(
                new PositionCommand(ctx, ac, s, hcds.head._2, currentState.asInstanceOf[TromboneState], tromboneStateActor)
              )
            ),
            CommandExecutionState.Executing
          )
        case ac.setElevationCK =>
          setElevationItem = s(ac.naElevationKey)
          AssemblyCommandState(
            Some(
              List(
                new SetElevationCommand(ctx, ac, s, hcds.head._2, currentState.asInstanceOf[TromboneState], tromboneStateActor)
              )
            ),
            CommandExecutionState.Executing
          )

        case ac.followCK =>
          val nssItem = s(ac.nssInUseKey)
          followCommandActor = ctx.spawnAnonymous(
            FollowCommandActor.make(ac, setElevationItem, nssItem, hcds.head._2, allEventPublisher)
          )
          AssemblyCommandState(
            Some(
              List(
                new FollowCommand(ctx, ac, s, hcds.head._2, currentState.asInstanceOf[TromboneState], tromboneStateActor)
              )
            ),
            CommandExecutionState.Following
          )

        case ac.stopCK =>
          replyTo ! NoLongerValid(s.runId, WrongInternalStateIssue("Trombone assembly must be executing a command to use stop"))
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)

        case ac.setAngleCK =>
          replyTo ! NoLongerValid(s.runId, WrongInternalStateIssue("Trombone assembly must be following for setAngle"))
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)

        case otherCommand =>
          replyTo ! Invalid(
            s.runId,
            UnsupportedCommandInStateIssue(
              s"""Trombone assembly does not support the command \"${otherCommand.prefix}\" in the current state."""
            )
          )
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)
      }
    case _ ⇒
      println(s"Unexpected command :[$commandMessage] received by component")
      AssemblyCommandState(None, CommandExecutionState.NotFollowing)

  }

  override def onFollowing(commandMessage: CommandMessage): AssemblyCommandState = commandMessage match {
    case Submit(s: Setup, replyTo) =>
      s.target match {
        case ac.datumCK | ac.moveCK | ac.positionCK | ac.followCK | ac.setElevationCK =>
          replyTo ! Invalid(
            s.runId,
            WrongInternalStateIssue(
              "Trombone assembly cannot be following for datum, move, position, setElevation, and follow"
            )
          )
          AssemblyCommandState(None, CommandExecutionState.Following)

        case ac.setAngleCK =>
          AssemblyCommandState(
            Some(
              List(
                new SetAngleCommand(ctx,
                                    ac,
                                    s,
                                    followCommandActor,
                                    hcds.head._2,
                                    currentState.asInstanceOf[TromboneState],
                                    tromboneStateActor)
              )
            ),
            CommandExecutionState.Following
          )

        case ac.stopCK =>
          currentCommand.foreach(x ⇒ x.foreach(_.stopCommand()))
          tromboneStateActor ! Publish(
            TromboneState(cmdItem(cmdReady),
                          moveItem(moveIndexed),
                          currentState.asInstanceOf[TromboneState].sodiumLayer,
                          currentState.asInstanceOf[TromboneState].nss)
          )
          replyTo ! Completed(s.runId)
          ctx.stop(followCommandActor)
          AssemblyCommandState(None, CommandExecutionState.NotFollowing)

        case other =>
          println(s"Unknown config key: $commandMessage")
          AssemblyCommandState(None, CommandExecutionState.Following)
      }
    case _ ⇒
      println(s"Unexpected command :[$commandMessage] received by component")
      AssemblyCommandState(None, CommandExecutionState.NotFollowing)
  }

  override def onExecuting(commandMessage: CommandMessage): AssemblyCommandState = commandMessage match {
    case Submit(Setup(runId, _, ac.stopCK, ac.obsId, _), replyTo) =>
      currentCommand.foreach(x ⇒ x.foreach(_.stopCommand()))
      replyTo ! Cancelled(runId)
      AssemblyCommandState(None, CommandExecutionState.NotFollowing)

    case x =>
      println(s"TromboneCommandHandler:actorExecutingReceive received an unknown message: $x")
      AssemblyCommandState(None, CommandExecutionState.Executing)
  }

  override def onFollowingCommandComplete(replyTo: ActorRef[CommandResponse], result: CommandResponse): Unit =
    replyTo ! result

  override def onExecutingCommandComplete(replyTo: ActorRef[CommandResponse], result: CommandResponse): Unit = {
    replyTo ! result
  }

}
