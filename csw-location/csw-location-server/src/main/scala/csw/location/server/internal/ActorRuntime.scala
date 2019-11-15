package csw.location.server.internal

import akka.{Done, actor}
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.location.server.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

private[location] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                    = typedSystem.executionContext
  implicit val scheduler: Scheduler                            = typedSystem.scheduler

  val classicSystem: actor.ActorSystem      = typedSystem.toClassic
  private[location] val coordinatedShutdown = CoordinatedShutdown(classicSystem)

  def startLogging(name: String, hostname: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
