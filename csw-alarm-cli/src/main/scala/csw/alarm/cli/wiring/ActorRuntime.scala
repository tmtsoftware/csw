package csw.alarm.cli.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_actorSystem: ActorSystem) {
  implicit lazy val system: ActorSystem          = _actorSystem
  implicit lazy val ec: ExecutionContextExecutor = system.dispatcher
  implicit lazy val mat: Materializer            = ActorMaterializer()

  lazy val coordinatedShutdown = CoordinatedShutdown(system)

  /**
   * Gracefully shutdown [[_actorSystem]]
   *
   * @param reason the reason for shutdown
   * @return a future that completes when shutdown is successful
   */
  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
