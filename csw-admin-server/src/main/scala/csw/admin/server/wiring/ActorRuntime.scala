package csw.admin.server.wiring

import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.{Scheduler, SpawnProtocol}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.{CoordinatedShutdown, typed}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{Done, actor}
import csw.admin.server.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

private[admin] class ActorRuntime(_typedSystem: typed.ActorSystem[SpawnProtocol]) {
  implicit val typedSystem: typed.ActorSystem[SpawnProtocol] = _typedSystem
  implicit val untypedSystem: actor.ActorSystem              = _typedSystem.toUntyped
  implicit val ec: ExecutionContextExecutor                  = untypedSystem.dispatcher
  implicit val mat: Materializer                             = ActorMaterializer()(typedSystem)
  implicit val scheduler: Scheduler                          = typedSystem.scheduler

  private[admin] val coordinatedShutdown = CoordinatedShutdown(untypedSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
