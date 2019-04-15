package csw.admin.server.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{typed, ActorSystem, CoordinatedShutdown, Scheduler}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.services.BuildInfo

import scala.concurrent.{ExecutionContextExecutor, Future}

private[admin] class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val actorSystem: ActorSystem          = _actorSystem
  implicit val typedSystem: typed.ActorSystem[_] = _actorSystem.toTyped
  implicit val ec: ExecutionContextExecutor      = actorSystem.dispatcher
  implicit val mat: Materializer                 = ActorMaterializer()
  implicit val scheduler: Scheduler              = actorSystem.scheduler

  private[admin] val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
