package csw.trombone.assembly.commands
import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.util.Timeout
import csw.messages.ccs.commands.Setup
import csw.messages.params.states.CurrentState
import csw.messages.{CommandExecutionResponse, Completed, Error, PubSub}
import csw.trombone.assembly.FollowActorMessages.{SetZenithAngle, StopFollowing}
import csw.trombone.assembly.actors.TromboneStateActor._
import csw.trombone.assembly.{AssemblyContext, FollowCommandMessages, Matchers, TromboneCommandHandlerMsgs}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class SetAngleCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    followCommandActor: ActorRef[FollowCommandMessages],
    startState: TromboneState,
    stateActor: ActorRef[TromboneStateMsg]
) extends TromboneAssemblyCommand {
  private val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.system.deadLetters
  import ctx.executionContext
  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val timeout: Timeout     = Timeout(5.seconds)
  override def startCommand(): Future[CommandExecutionResponse] = {
    sendState(
      SetState(
        cmdBusy,
        move(startState),
        sodiumLayer(startState),
        nss(startState),
        ctx.spawnAnonymous(Actor.ignore)
      )
    )
    val zenithAngleItem = s(ac.zenithAngleKey)
    followCommandActor ! SetZenithAngle(zenithAngleItem)
    Matchers.matchState(ctx, Matchers.idleMatcher, pubSubRef, 5.seconds).map {
      case Completed =>
        sendState(
          SetState(cmdContinuous,
                   move(startState),
                   sodiumLayer(startState),
                   nss(startState),
                   ctx.spawnAnonymous(Actor.ignore))
        )
        Completed
      case Error(message) =>
        println(s"setElevation command failed with message: $message")
        Error(message)
    }
  }

  override def stopCurrentCommand(): Unit = {
    followCommandActor ! StopFollowing
  }

  private def sendState(setState: SetState): Unit = {
    implicit val timeout: Timeout = Timeout(5.seconds)
    Await.ready(stateActor ? { x: ActorRef[StateWasSet] ⇒
      setState
    }, timeout.duration)
  }
}
