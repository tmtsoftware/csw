package csw.services.event.internal.wiring

import java.util.concurrent.CompletableFuture

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future}

private[event] class Wiring(_actorSystem: ActorSystem) {
  implicit lazy val actorSystem: ActorSystem = _actorSystem
  implicit lazy val ec: ExecutionContext     = actorSystem.dispatcher

  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)

  implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)

  def shutdown(reason: Reason): Future[Done] = CoordinatedShutdown(actorSystem).run(reason)

  def jShutdown(reason: Reason): CompletableFuture[Done] = CoordinatedShutdown(actorSystem).run(reason).toJava.toCompletableFuture
}
