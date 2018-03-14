package csw.framework.internal.wiring

import akka.Done
import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown.Reason
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.BuildInfo
import csw.services.location.commons.{ClusterAwareSettings, CswCoordinatedShutdown}
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
private[framework] class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  def startLogging(): LoggingSystem =
    LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

  def shutdown(reason: Reason): Future[Done] = CswCoordinatedShutdown.run(actorSystem, reason)
}
