package csw.framework.internal.wiring

import akka.actor.{ActorSystem, Scheduler}
import akka.typed.scaladsl.AskPattern.Askable
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.{ActorRef, Behavior, Props}
import akka.util.Timeout
import csw.framework.internal.wiring.CswFrameworkGuardian.CreateActor

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class CswFrameworkSystem(val system: ActorSystem) {
  implicit val sched: Scheduler         = system.scheduler
  implicit val timeout: Timeout         = Timeout(2.seconds)
  private val cswFrameworkGuardianActor = system.spawn(CswFrameworkGuardian.behavior, "system")
  def spawnTyped[T](behavior: Behavior[T], name: String, props: Props = Props.empty): Future[ActorRef[T]] = {
    cswFrameworkGuardianActor ? CreateActor(behavior, name, props)
  }
}
