package csw.services.config.server

import java.util.concurrent.{CompletableFuture, CompletionStage}

import akka.Done
import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.stream.{ActorMaterializer, Materializer}

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_actorSystem: ActorSystem, settings: Settings) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  val blockingIoDispatcher: MessageDispatcher = actorSystem.dispatchers.lookup(settings.`blocking-io-dispatcher`)

  def shutdown(): Future[Done] = actorSystem.terminate().map(_ ⇒ Done)

  def jShutdown(): CompletableFuture[Done] = shutdown().toJava.toCompletableFuture
}
