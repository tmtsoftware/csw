package csw.apps.clusterseed.admin.internal

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.BuildInfo
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.scaladsl.LoggingSystem

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(): LoggingSystem =
    new LoggingSystem(BuildInfo.name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

  def shutdown(): Future[Done] = coordinatedShutdown.run()

}
