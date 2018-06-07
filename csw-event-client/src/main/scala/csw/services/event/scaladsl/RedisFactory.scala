package csw.services.event.scaladsl

import java.net.URI

import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.redis.{RedisPublisher, RedisSubscriber}
import csw.services.event.internal.wiring.EventServiceResolver
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

class RedisFactory(
    redisClient: RedisClient,
    eventServiceResolver: EventServiceResolver,
    eventPublisherUtil: EventPublisherUtil,
    eventSubscriberUtil: EventSubscriberUtil
)(implicit ec: ExecutionContext) {

  def publisher(host: String, port: Int): EventPublisher = {
    val redisURI = RedisURI.create(host, port)
    new RedisPublisher(redisURI, redisClient, eventPublisherUtil)
  }

  def publisher(): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    publisher(uri.getHost, uri.getPort)
  }

  def subscriber(host: String, port: Int): EventSubscriber = {
    val redisURI = RedisURI.create(host, port)
    new RedisSubscriber(redisURI, redisClient, eventSubscriberUtil)
  }

  def subscriber(): Future[EventSubscriber] = async {
    val uri: URI = await(eventServiceResolver.uri)
    subscriber(uri.getHost, uri.getPort)
  }
}
