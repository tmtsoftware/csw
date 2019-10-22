package csw.framework.internal.wiring

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props, Scheduler, SpawnProtocol}
import akka.util.Timeout
import csw.framework.internal.wiring.CswFrameworkGuardian.CreateActor
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

/**
 * A convenient class for creating a `typed` actor from the provided `untyped` actor system.
 * It creates an actor `CswFrameworkGuardian` from the provided actor system which spawns any other required actor
 * as its child. This is required because the default supervision strategy for an actor created from untyped actor
 * system is to `restart` the underlying actor but we want the default supervision strategy of `stopping` the actor
 * as provided in the `typed` actor world.
 */
private[framework] class CswFrameworkSystem(val system: ActorSystem[SpawnProtocol.Command]) {
  implicit val scheduler: Scheduler     = system.scheduler
  implicit val timeout: Timeout         = Timeout(2.seconds)
  private val cswFrameworkGuardianActor = system.spawn(CswFrameworkGuardian.behavior, "system")
  def spawnTyped[T](behavior: Behavior[T], name: String, props: Props = Props.empty): Future[ActorRef[T]] =
    cswFrameworkGuardianActor ? CreateActor(behavior, name, props)
}
