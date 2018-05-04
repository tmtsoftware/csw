package csw.services.event.javadsl

import java.net.URI
import java.util.concurrent.CompletableFuture

import csw.services.event.internal.pubsub.{JBaseEventPublisher, JBaseEventSubscriber}
import csw.services.event.internal.redis._
import csw.services.event.internal.wiring.{EventServiceResolver, Wiring}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.FutureOps

class JRedisFactory(redisClient: RedisClient, locationService: LocationService, wiring: Wiring) {
  import wiring._

  private val eventServiceResolver = new EventServiceResolver(locationService)

  def publisher(host: String, port: Int): IEventPublisher = {
    val redisURI = RedisURI.create(host, port)
    new JBaseEventPublisher(new RedisPublisher(redisURI, redisClient))
  }

  def publisher(): CompletableFuture[IEventPublisher] =
    async {
      val uri: URI = await(eventServiceResolver.uri)
      publisher(uri.getHost, uri.getPort)
    }.toJava.toCompletableFuture

  def subscriber(host: String, port: Int): IEventSubscriber = {
    val redisURI        = RedisURI.create(host, port)
    val redisSubscriber = new RedisSubscriber(redisURI, redisClient)
    new JBaseEventSubscriber(redisSubscriber)
  }

  def subscriber(): CompletableFuture[IEventSubscriber] =
    async {
      val uri: URI = await(eventServiceResolver.uri)
      subscriber(uri.getHost, uri.getPort)
    }.toJava.toCompletableFuture

}
