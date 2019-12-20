package csw.location.impl.internal

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.{Done, actor}
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory

import scala.concurrent.{ExecutionContext, Future}

private[location] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContext                            = typedSystem.executionContext
  implicit val scheduler: Scheduler                            = typedSystem.scheduler

  val classicSystem: actor.ActorSystem      = typedSystem.toClassic
  private[location] val coordinatedShutdown = CoordinatedShutdown(classicSystem)

  def startLogging(name: String, hostname: String, version: String): LoggingSystem =
    LoggingSystemFactory.start(name, version, hostname, typedSystem)

  def shutdown(): Future[Done] = {
    typedSystem.terminate()
    typedSystem.whenTerminated
  }
}
