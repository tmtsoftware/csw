package csw.framework.internal.wiring

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.{Done, actor}
import csw.framework.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

// $COVERAGE-OFF$
/**
 * A convenient class wrapping actor system and providing handles for execution context and clean up of actor system
 */
class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                    = typedSystem.executionContext
  val classicSystem: actor.ActorSystem                         = typedSystem.toClassic
  lazy val coordinatedShutdown: CoordinatedShutdown            = CoordinatedShutdown(classicSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
// $COVERAGE-ON$
