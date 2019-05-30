package csw.alarm.cli.wiring

import akka.{actor, Done}
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.services.BuildInfo

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol]) {
  implicit lazy val typedSystem: ActorSystem[SpawnProtocol] = _typedSystem
  implicit lazy val untypedSystem: actor.ActorSystem        = _typedSystem.toUntyped
  implicit lazy val ec: ExecutionContextExecutor            = untypedSystem.dispatcher
  implicit lazy val mat: Materializer                       = ActorMaterializer()(typedSystem)

  lazy val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(untypedSystem)

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
