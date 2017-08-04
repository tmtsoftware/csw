package csw.trombone.assembly.commands

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.CommandMsg.Submit
import csw.param.events.EventTime
import csw.param.parameters.Parameter
import csw.trombone.assembly.FollowActorMessages.{StopFollowing, UpdatedEventData}
import csw.trombone.assembly.FollowCommandMessages.{UpdateNssInUse, UpdateTromboneHcd, UpdateZAandFE}
import csw.trombone.assembly._
import csw.trombone.assembly.actors.{FollowActor, TromboneControl}

object FollowCommand {

  def make(assemblyContext: AssemblyContext,
           initialElevation: Parameter[Double],
           nssInUse: Parameter[Boolean],
           tromboneHCD: Option[ActorRef[Submit]],
           eventPublisher: Option[ActorRef[TrombonePublisherMsg]]): Behavior[FollowCommandMessages] =
    Actor.mutable(
      ctx ⇒ new FollowCommand(assemblyContext, initialElevation, nssInUse, tromboneHCD, eventPublisher, ctx)
    )
}

class FollowCommand(ac: AssemblyContext,
                    initialElevation: Parameter[Double],
                    val nssInUseIn: Parameter[Boolean],
                    val tromboneHCDIn: Option[ActorRef[Submit]],
                    eventPublisher: Option[ActorRef[TrombonePublisherMsg]],
                    ctx: ActorContext[FollowCommandMessages])
    extends MutableBehavior[FollowCommandMessages] {

  val tromboneControl: ActorRef[TromboneControlMsg] =
    ctx.spawn(TromboneControl.behavior(ac, tromboneHCDIn), "TromboneControl")

  var followActor: ActorRef[FollowActorMessages] = ctx.spawn(
    FollowActor.make(ac, initialElevation, nssInUseIn, Some(tromboneControl), eventPublisher, eventPublisher),
    "FollowActor"
  )

  val nssInUse: Parameter[Boolean] = nssInUseIn

  var tromboneHCD: Option[ActorRef[Submit]] = tromboneHCDIn

  override def onMessage(msg: FollowCommandMessages): Behavior[FollowCommandMessages] = msg match {
    case UpdateNssInUse(nssInUseUpdate) =>
      if (nssInUseUpdate != nssInUse) {
        ctx.stop(followActor)
        followActor = ctx.spawnAnonymous(
          FollowActor.make(ac, initialElevation, nssInUseIn, Some(tromboneControl), eventPublisher, eventPublisher)
        )
      }
      this

    case UpdateZAandFE(zenithAngleIn, focusErrorIn) =>
      followActor ! UpdatedEventData(zenithAngleIn, focusErrorIn, EventTime())
      this

    case UpdateTromboneHcd(running) =>
      tromboneHCD = running
      tromboneControl ! TromboneControlMsg.UpdateTromboneHcd(running)
      this

    case StopFollowing =>
      ctx.stop(followActor)
      this

    case m: FollowActorMessages =>
      followActor ! m
      this
  }
}
