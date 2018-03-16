package csw.services.event.internal.commons

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.actor.CoordinatedShutdown.Reason
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}

import scala.concurrent.{ExecutionContext, Future}

private[event] class Wiring(_actorSystem: ActorSystem) {
  implicit lazy val actorSystem: ActorSystem = _actorSystem
  implicit lazy val ec: ExecutionContext     = actorSystem.dispatcher

  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)

  implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)

  def shutdown(reason: Reason): Future[Done] = CoordinatedShutdown(actorSystem).run(reason)
}
