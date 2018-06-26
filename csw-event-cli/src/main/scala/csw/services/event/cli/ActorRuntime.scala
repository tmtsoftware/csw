package csw.services.event.cli

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.actor.CoordinatedShutdown.Reason
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val system: ActorSystem          = _actorSystem
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  private val coordinatedShutdown = CoordinatedShutdown(system)

  /**
   * Gracefully shutdown [[_actorSystem]]
   *
   * @param reason the reason for shutdown
   * @return a future that completes when shutdown is successful
   */
  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
