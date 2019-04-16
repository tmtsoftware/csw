package csw.config.server

import java.util.concurrent.CompletableFuture

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.dispatch.MessageDispatcher
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.services.BuildInfo

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
private[config] class ActorRuntime(_actorSystem: ActorSystem, val settings: Settings) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()(actorSystem.toTyped)

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(actorSystem)

  val blockingIoDispatcher: MessageDispatcher = actorSystem.dispatchers.lookup(settings.`blocking-io-dispatcher`)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, actorSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)

  def jShutdown(reason: Reason): CompletableFuture[Done] = shutdown(reason).toJava.toCompletableFuture
}
