package csw.common.framework.internal.extensions

import akka.actor.{ActorSystem, Scheduler}
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.AskPattern.Askable
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.{ActorRef, Behavior, Props}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

object RichSystemExtension {
  implicit val timeout: Timeout = Timeout(2.seconds)

  sealed trait GuardianBehaviorMsg
  case class CreateActor[T](behavior: Behavior[T], name: String, props: Props)(val replyTo: ActorRef[ActorRef[T]])
      extends GuardianBehaviorMsg

  def behavior: Behavior[GuardianBehaviorMsg] = Actor.immutable {
    case (ctx, create: CreateActor[t]) ⇒
      create.replyTo ! ctx.spawn(create.behavior, create.name, create.props)
      Actor.same
  }

  class RichSystem(val system: ActorSystem) {
    implicit def sched: Scheduler = system.scheduler
    private val rootActor         = system.spawn(behavior, "system")
    def spawnTyped[T](behavior: Behavior[T], name: String, props: Props = Props.empty): Future[ActorRef[T]] = {
      rootActor ? CreateActor(behavior, name, props)
    }
  }
}
