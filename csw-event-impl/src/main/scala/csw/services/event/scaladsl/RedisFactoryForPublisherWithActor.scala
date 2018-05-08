package csw.services.event.scaladsl

import java.net.URI

import akka.actor.ActorSystem
import akka.stream.Materializer
import csw.services.event.internal.redis.RedisPublisherWithSetActor
import csw.services.event.internal.wiring.EventServiceResolver
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

class RedisFactoryForPublisherWithActor(redisClient: RedisClient, eventServiceResolver: EventServiceResolver)(
    implicit ec: ExecutionContext,
    mat: Materializer,
    actorSystem: ActorSystem
) extends RedisFactory(redisClient, eventServiceResolver) {

  override def publisher(host: String, port: Int): EventPublisher = {
    val redisURI = RedisURI.create(host, port)
    new RedisPublisherWithSetActor(redisURI, redisClient, actorSystem)
  }

  override def publisher(): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    this.publisher(uri.getHost, uri.getPort)
  }
}
