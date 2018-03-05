package csw.services.event

import java.net.URI

import akka.actor.ActorSystem
import csw.services.event.internal.commons.EventServiceResolver
import csw.services.event.internal.kafka.KafkaWiring
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import csw.services.location.scaladsl.LocationService

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class KafkaFactory(locationService: LocationService, actorSystem: ActorSystem) {

  private implicit val ec: ExecutionContext = actorSystem.dispatcher
  private val eventServiceResolver          = new EventServiceResolver(locationService)

  def publisher(host: String, port: Int): EventPublisher = new KafkaWiring(host, port, actorSystem).publisher()

  def publisher(): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    KafkaWiring(uri, actorSystem).publisher()
  }

  def subscriber(host: String, port: Int): EventSubscriber = new KafkaWiring(host, port, actorSystem).subscriber()

  def subscriber(): Future[EventSubscriber] = async {
    val uri: URI = await(eventServiceResolver.uri)
    KafkaWiring(uri, actorSystem).subscriber()
  }
}
