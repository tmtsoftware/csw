package csw.framework.internal.wiring

import akka.actor.CoordinatedShutdown
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.{ActorRef, Behavior, Props, Terminated}
import csw.messages.models.CoordinatedShutdownReasons.ActorTerminatedReason

/**
 * This actor should be used for spawning actors which needs to be spawned directly from the actor system.
 * This is because the actor system used in csw is an `untyped` actor system which spawns a typed actor with default
 * supervision strategy of `restart` rather than stopping the actor which is the default in the `typed` actor system.
 */
object CswFrameworkGuardian {

  sealed trait GuardianMsg
  case class CreateActor[T](behavior: Behavior[T], name: String, props: Props)(val replyTo: ActorRef[ActorRef[T]])
      extends GuardianMsg

  def behavior: Behavior[GuardianMsg] =
    Actor.immutable[GuardianMsg] {
      case (ctx, msg) ⇒
        msg match {
          case create: CreateActor[t] =>
            val componentRef = ctx.spawn(create.behavior, create.name, create.props)
            ctx.watch(componentRef)
            create.replyTo ! componentRef
        }
        Actor.same
    } onSignal {
      case (ctx, Terminated(_)) ⇒
        CoordinatedShutdown(ctx.system.toUntyped).run(ActorTerminatedReason)
        Actor.stopped
    }
}
