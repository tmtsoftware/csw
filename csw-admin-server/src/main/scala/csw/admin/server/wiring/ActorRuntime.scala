package csw.admin.server.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{ActorSystem, CoordinatedShutdown, Scheduler}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.services.BuildInfo

import scala.concurrent.{ExecutionContextExecutor, Future}

private[admin] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol]) {
  implicit val typedSystem                      = _typedSystem
  implicit val untypedSystem: actor.ActorSystem = _typedSystem.toUntyped
  implicit val ec: ExecutionContextExecutor     = untypedSystem.dispatcher
  implicit val mat: Materializer                = ActorMaterializer()(typedSystem)
  implicit val scheduler: Scheduler             = untypedSystem.scheduler

  private[admin] val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, actorSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
