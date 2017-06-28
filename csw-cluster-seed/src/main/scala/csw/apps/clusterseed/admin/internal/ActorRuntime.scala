package csw.apps.clusterseed.admin.internal

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.appenders.{FileAppender, StdOutAppender}
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(): LoggingSystem =
    LoggingSystemFactory.start("cluster-seed-app", ClusterAwareSettings.hostname, actorSystem,
      Seq(StdOutAppender, FileAppender))

  def shutdown(): Future[Done] = coordinatedShutdown.run()

}
