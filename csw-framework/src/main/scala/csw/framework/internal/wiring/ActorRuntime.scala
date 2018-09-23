package csw.framework.internal.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.BuildInfo
import csw.location.api.commons.ClusterAwareSettings
import csw.logging.internal.LoggingSystem
import csw.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

// $COVERAGE-OFF$
/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
private[framework] class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val system: ActorSystem          = _actorSystem
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  private[framework] val coordinatedShutdown = CoordinatedShutdown(system)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, ClusterAwareSettings.hostname, system)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
// $COVERAGE-ON$
