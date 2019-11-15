package csw.location.agent.wiring

import akka.{Done, actor}
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.location.agent.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

private[agent] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                    = typedSystem.executionContext
  implicit val scheduler: Scheduler                            = typedSystem.scheduler

  val classicSystem: actor.ActorSystem         = typedSystem.toClassic
  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(classicSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
