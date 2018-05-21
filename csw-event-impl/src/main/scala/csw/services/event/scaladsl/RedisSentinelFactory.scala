package csw.services.event.scaladsl

import java.net.URI

import akka.stream.Materializer
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.redis.{RedisPublisher, RedisSubscriber}
import csw.services.event.internal.wiring.EventServiceResolver
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

class RedisSentinelFactory(
    redisClient: RedisClient,
    eventServiceResolver: EventServiceResolver,
    eventPublisherUtil: EventPublisherUtil,
    eventSubscriberUtil: EventSubscriberUtil
)(implicit ec: ExecutionContext, mat: Materializer) {

  def publisher(host: String, port: Int, masterId: String): EventPublisher = {
    val redisURI = RedisURI.Builder.sentinel(host, port, masterId).build()
    new RedisPublisher(redisURI, redisClient, eventPublisherUtil)
  }

  def publisher(masterId: String): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    publisher(uri.getHost, uri.getPort, masterId)
  }

  def subscriber(host: String, port: Int, masterId: String): EventSubscriber = {
    val redisURI = RedisURI.Builder.sentinel(host, port, masterId).build()
    new RedisSubscriber(redisURI, redisClient, eventSubscriberUtil)
  }

  def subscriber(masterId: String): Future[EventSubscriber] = async {
    val uri: URI = await(eventServiceResolver.uri)
    subscriber(uri.getHost, uri.getPort, masterId)
  }
}
