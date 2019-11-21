package csw.event.cli.wiring

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.{Done, actor}
import csw.event.cli.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                    = typedSystem.executionContext

  val classicSystem: actor.ActorSystem         = typedSystem.toClassic
  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(classicSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  /**
   * Gracefully shutdown [[_typedSystem]]
   *
   * @return a future that completes when shutdown is successful
   */
  def shutdown(): Future[Done] = {
    typedSystem.terminate()
    typedSystem.whenTerminated
  }
}
