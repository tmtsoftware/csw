package csw.event.cli.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.{ActorMaterializer, Materializer}
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.services.BuildInfo

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val system: ActorSystem          = _actorSystem
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  val coordinatedShutdown = CoordinatedShutdown(system)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, system)

  /**
   * Gracefully shutdown [[_actorSystem]]
   *
   * @param reason the reason for shutdown
   * @return a future that completes when shutdown is successful
   */
  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
