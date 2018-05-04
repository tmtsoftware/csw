package csw.services.event.scaladsl

import java.net.URI

import csw.services.event.internal.redis.RedisPublisherWithSetActor
import csw.services.event.internal.wiring.{EventServiceResolver, Wiring}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.concurrent.Future

class RedisFactoryForPublisherWithActor(redisClient: RedisClient, locationService: LocationService, wiring: Wiring)
    extends RedisFactory(redisClient, locationService, wiring) {
  import wiring._

  private val eventServiceResolver = new EventServiceResolver(locationService)

  override def publisher(host: String, port: Int): EventPublisher = {
    val redisURI = RedisURI.create(host, port)
    new RedisPublisherWithSetActor(redisURI, redisClient, wiring.actorSystem)
  }

  override def publisher(): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    this.publisher(uri.getHost, uri.getPort)
  }
}
