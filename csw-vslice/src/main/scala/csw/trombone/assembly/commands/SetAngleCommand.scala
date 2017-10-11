package csw.trombone.assembly.commands
import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.messages.PubSub.Publish
import csw.messages._
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.FollowActorMessages.{SetZenithAngle, StopFollowing}
import csw.trombone.assembly.actors.TromboneStateActor._
import csw.trombone.assembly.{AssemblyContext, FollowCommandMessages, Matchers, TromboneCommandHandlerMsgs}

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class SetAngleCommand(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    followCommandActor: ActorRef[FollowCommandMessages],
    tromboneHCD: ActorRef[SupervisorExternalMessage],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[TromboneState]]
) extends TromboneAssemblyCommand {
  import ctx.executionContext
  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val timeout: Timeout     = Timeout(5.seconds)
  override def startCommand(): Future[CommandExecutionResponse] = {
    sendState(
      TromboneState(
        cmdItem(cmdBusy),
        startState.move,
        startState.sodiumLayer,
        startState.nss
      )
    )
    val zenithAngleItem = s(ac.zenithAngleKey)
    followCommandActor ! SetZenithAngle(zenithAngleItem)
    Matchers.matchState(ctx, Matchers.idleMatcher, tromboneHCD, 5.seconds).map {
      case Completed =>
        sendState(TromboneState(cmdItem(cmdContinuous), startState.move, startState.sodiumLayer, startState.nss))
        Completed
      case Error(message) =>
        println(s"setElevation command failed with message: $message")
        Error(message)
    }
  }

  override def stopCurrentCommand(): Unit = {
    followCommandActor ! StopFollowing
  }

  private def sendState(setState: TromboneState): Unit = {
    stateActor ! Publish(setState)
  }
}
