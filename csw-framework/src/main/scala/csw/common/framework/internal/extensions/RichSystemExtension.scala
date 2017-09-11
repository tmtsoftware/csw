package csw.common.framework.internal.extensions

import akka.actor.{ActorSystem, Scheduler}
import akka.typed.internal.ActorSystemImpl
import akka.typed.internal.ActorSystemImpl.CreateSystemActor
import akka.typed.scaladsl.AskPattern.Askable
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.{ActorRef, Behavior, Props}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

object RichSystemExtension {
  implicit val timeout: Timeout = Timeout(2.seconds)

  class RichSystem(val system: ActorSystem) {
    implicit def sched: Scheduler = system.scheduler
    private val rootActor         = system.spawn(ActorSystemImpl.systemGuardianBehavior, "system")
    def spawnTyped[T](behavior: Behavior[T], name: String, props: Props = Props.empty): Future[ActorRef[T]] = {
      rootActor ? CreateSystemActor(behavior, name, props)
    }
  }
}
