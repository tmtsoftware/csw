package csw.common.framework.internal.wiring

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}

import scala.concurrent.Future

class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val actorSystem: ActorSystem = _actorSystem
  val coordinatedShutdown               = CoordinatedShutdown(actorSystem)
  def shutdown(): Future[Done]          = coordinatedShutdown.run()
}
