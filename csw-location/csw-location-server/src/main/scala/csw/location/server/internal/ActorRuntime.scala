package csw.location.server.internal

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{typed, ActorSystem, CoordinatedShutdown, Scheduler}
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.location.server.commons.ClusterAwareSettings
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.services.BuildInfo

import scala.concurrent.{ExecutionContextExecutor, Future}

private[location] class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val actorSystem: ActorSystem               = _actorSystem
  implicit val typedActorSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val ec: ExecutionContextExecutor           = actorSystem.dispatcher
  implicit val mat: ActorMaterializer                 = ActorMaterializer()
  implicit val scheduler: Scheduler                   = typedActorSystem.scheduler

  private[location] val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
