package csw.location.server.internal

import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.actor.CoordinatedShutdown
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{Done, actor}
import csw.location.server.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

private[location] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol] = _typedSystem
  implicit val untypedSystem: actor.ActorSystem        = _typedSystem.toUntyped
  implicit val ec: ExecutionContextExecutor            = typedSystem.executionContext
  implicit val mat: Materializer                       = ActorMaterializer()
  implicit val scheduler: Scheduler                    = typedSystem.scheduler

  private[location] val coordinatedShutdown = CoordinatedShutdown(untypedSystem)

  def startLogging(name: String, hostname: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
