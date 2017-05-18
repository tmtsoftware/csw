package csw.services.config.server

import java.util.concurrent.CompletableFuture

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.dispatch.MessageDispatcher
import akka.stream.{ActorMaterializer, Materializer}

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
class ActorRuntime(_actorSystem: ActorSystem, settings: Settings) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  val blockingIoDispatcher: MessageDispatcher = actorSystem.dispatchers.lookup(settings.`blocking-io-dispatcher`)

  val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def shutdown(): Future[Done] = coordinatedShutdown.run()

  def jShutdown(): CompletableFuture[Done] = shutdown().toJava.toCompletableFuture
}
