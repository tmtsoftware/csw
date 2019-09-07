package csw.event.cli.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import csw.event.cli.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                    = typedSystem.executionContext
  implicit val mat: Materializer                               = Materializer(typedSystem)

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(typedSystem.toClassic)

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
