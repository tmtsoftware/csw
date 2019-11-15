package csw.admin.server.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.{ActorSystem, CoordinatedShutdown, typed}
import csw.admin.server.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

private[admin] class ActorRuntime(_typedSystem: typed.ActorSystem[SpawnProtocol.Command]) {
  implicit val typedSystem: typed.ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                          = _typedSystem.executionContext

  val classicSystem: ActorSystem         = typedSystem.toClassic
  private[admin] val coordinatedShutdown = CoordinatedShutdown(classicSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
