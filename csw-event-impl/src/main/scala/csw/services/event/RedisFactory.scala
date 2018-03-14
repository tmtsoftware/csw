package csw.services.event

import java.net.URI

import csw.services.event.internal.commons.{EventServiceResolver, Wiring}
import csw.services.event.internal.redis.{RedisPublisher, RedisSubscriber}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.concurrent.Future

class RedisFactory(redisClient: RedisClient, locationService: LocationService, wiring: Wiring) {
  import wiring._
  private val eventServiceResolver = new EventServiceResolver(locationService)

  def publisher(host: String, port: Int): EventPublisher = {
    val redisURI = RedisURI.create(host, port)
    new RedisPublisher(redisURI, redisClient)
  }

  def publisher(): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    publisher(uri.getHost, uri.getPort)
  }

  def subscriber(host: String, port: Int): EventSubscriber = {
    val redisURI = RedisURI.create(host, port)
    new RedisSubscriber(redisURI, redisClient)
  }

  def subscriber(): Future[EventSubscriber] = async {
    val uri: URI = await(eventServiceResolver.uri)
    subscriber(uri.getHost, uri.getPort)
  }

}
