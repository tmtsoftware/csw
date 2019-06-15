package csw.location.agent.wiring

import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.{TypedActorSystemOps, UntypedActorSystemOps}
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.actor.CoordinatedShutdown
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{Done, actor}
import csw.location.agent.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

private[agent] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol] = _typedSystem
  implicit val untypedSystem: actor.ActorSystem        = _typedSystem.toUntyped
  implicit val ec: ExecutionContextExecutor            = typedSystem.executionContext
  implicit val mat: Materializer                       = ActorMaterializer()(untypedSystem.toTyped)
  implicit val scheduler: Scheduler                    = typedSystem.scheduler

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(untypedSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
