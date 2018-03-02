package csw.services.event

import java.net.URI

import akka.actor.ActorSystem
import csw.services.event.internal.commons.EventServiceResolver
import csw.services.event.internal.redis.RedisWiring
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import csw.services.location.scaladsl.LocationService

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

class RedisFactory(locationService: LocationService, actorSystem: ActorSystem) {
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  val eventServiceResolver          = new EventServiceResolver(locationService)

  def publisher(host: String, port: Int): EventPublisher = new RedisWiring(host, port, actorSystem).publisher()

  def publisher(name: String): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    RedisWiring(uri, actorSystem).publisher()
  }

  def subscriber(host: String, port: Int): EventSubscriber = new RedisWiring(host, port, actorSystem).subscriber()

  def subscriber(name: String): Future[EventSubscriber] = async {
    val uri: URI = await(eventServiceResolver.uri)
    RedisWiring(uri, actorSystem).subscriber()
  }

}
