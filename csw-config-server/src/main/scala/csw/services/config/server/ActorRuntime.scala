package csw.services.config.server

import java.util.concurrent.CompletableFuture

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.dispatch.MessageDispatcher
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.BuildInfo
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
class ActorRuntime private[config] (_actorSystem: ActorSystem, settings: Settings) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  val blockingIoDispatcher: MessageDispatcher = actorSystem.dispatchers.lookup(settings.`blocking-io-dispatcher`)

  val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(): LoggingSystem =
    LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)

  def jShutdown(reason: Reason): CompletableFuture[Done] = shutdown(reason).toJava.toCompletableFuture
}
