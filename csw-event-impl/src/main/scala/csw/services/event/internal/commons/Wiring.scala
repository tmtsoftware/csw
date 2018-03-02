package csw.services.event.internal.commons

import akka.Done
import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown.Reason
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import csw.services.location.commons.CswCoordinatedShutdown

import scala.concurrent.{ExecutionContext, Future}

abstract class Wiring private[csw] (host: String, port: Int, _actorSystem: ActorSystem) {
  implicit lazy val actorSystem: ActorSystem = _actorSystem
  implicit lazy val ec: ExecutionContext     = actorSystem.dispatcher

  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
  implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)

  def shutdown(reason: Reason): Future[Done] = CswCoordinatedShutdown.run(actorSystem, reason)

  def publisher(): EventPublisher
  def subscriber(): EventSubscriber
}
