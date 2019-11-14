package csw.config.server

import java.util.concurrent.CompletableFuture

import akka.{actor, Done}
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.dispatch.MessageDispatcher
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context and clean up of actor system
 */
private[config] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command], val settings: Settings) {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val untypedSystem: actor.ActorSystem                = _typedSystem.toClassic
  implicit val ec: ExecutionContextExecutor                    = typedSystem.executionContext

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(untypedSystem)

  val blockingIoDispatcher: MessageDispatcher = untypedSystem.dispatchers.lookup(settings.`blocking-io-dispatcher`)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)

  def jShutdown(reason: Reason): CompletableFuture[Done] = shutdown(reason).toJava.toCompletableFuture
}
