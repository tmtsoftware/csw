package csw.trombone.assembly.commands
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.util.Timeout
import csw.messages._
import csw.messages.ccs.commands.CommandResponse.{Completed, Error}
import csw.messages.ccs.commands.{CommandResponse, Setup}
import csw.messages.models.PubSub
import csw.services.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}
import csw.services.ccs.internal.matchers.Matcher
import csw.trombone.assembly.FollowActorMessages.{SetZenithAngle, StopFollowing}
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneState._

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class SetAngleCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    followCommandActor: ActorRef[FollowCommandMessages],
    tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[AssemblyState]]
) extends AssemblyCommand(ctx, startState, stateActor) {

  implicit val timeout: Timeout         = Timeout(5.seconds)
  implicit val ec: ExecutionContext     = ctx.executionContext
  implicit val actorSystem: ActorSystem = ctx.system.toUntyped
  implicit val mat: ActorMaterializer   = ActorMaterializer()

  override def startCommand(): Future[CommandResponse] = {
    publishState(TromboneState(cmdItem(cmdBusy), startState.move, startState.sodiumLayer, startState.nss))

    val zenithAngleItem = s(ac.zenithAngleKey)

    followCommandActor ! SetZenithAngle(zenithAngleItem)

    new Matcher(tromboneHCD.get, AssemblyMatchers.idleMatcher).response.map {
      case MatchCompleted =>
        publishState(TromboneState(cmdItem(cmdContinuous), startState.move, startState.sodiumLayer, startState.nss))
        Completed(s.runId)
      case MatchFailed(ex) =>
        println(s"setElevation command failed with message: ${ex.getMessage}")
        Error(s.runId, ex.getMessage)
    }
  }

  override def stopCommand(): Unit = {
    followCommandActor ! StopFollowing
  }

}
