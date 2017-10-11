package csw.trombone.assembly.commands
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.messages._
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.FollowActorMessages.{SetZenithAngle, StopFollowing}
import csw.trombone.assembly.actors.TromboneState._
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
    stateActor: ActorRef[PubSub[AssemblyState]]
) extends AssemblyCommand {

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def startCommand(): Future[CommandExecutionResponse] = {
    publishState(TromboneState(cmdItem(cmdBusy), startState.move, startState.sodiumLayer, startState.nss), stateActor)

    val zenithAngleItem = s(ac.zenithAngleKey)

    followCommandActor ! SetZenithAngle(zenithAngleItem)

    matchCompletion(ctx, Matchers.idleMatcher, tromboneHCD, 5.seconds) {
      case Completed =>
        publishState(TromboneState(cmdItem(cmdContinuous), startState.move, startState.sodiumLayer, startState.nss),
                     stateActor)
        Completed
      case Error(message) =>
        println(s"setElevation command failed with message: $message")
        Error(message)
    }
  }

  override def stopCommand(): Unit = {
    followCommandActor ! StopFollowing
  }

}
