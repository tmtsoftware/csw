package csw.event.cli.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.services.BuildInfo

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol] = _typedSystem
  implicit val ec: ExecutionContextExecutor            = typedSystem.executionContext
  implicit val mat: Materializer                       = ActorMaterializer()(typedSystem)

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(typedSystem.toUntyped)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  /**
   * Gracefully shutdown [[_typedSystem]]
   *
   * @param reason the reason for shutdown
   * @return a future that completes when shutdown is successful
   */
  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
