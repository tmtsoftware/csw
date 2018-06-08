package csw.services.event.internal.redis

import java.net.URI

import akka.stream.Materializer
import csw.services.event.internal.commons.EventServiceResolver
import csw.services.event.scaladsl.{EventPublisher, EventService, EventSubscriber}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

class RedisEventService(
    redisClient: RedisClient,
    masterId: String,
    eventServiceResolver: EventServiceResolver
)(implicit ec: ExecutionContext, mat: Materializer)
    extends EventService {

  lazy val defaultPublisher: Future[EventPublisher]   = publisher()
  lazy val defaultSubscriber: Future[EventSubscriber] = subscriber()

  override def makeNewPublisher(): Future[EventPublisher] = publisher()

  private[csw] def publisher(host: String, port: Int): EventPublisher =
    new RedisPublisher(redisURI(host, port), redisClient)

  private def publisher(): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    publisher(uri.getHost, uri.getPort)
  }

  private[csw] def subscriber(host: String, port: Int): EventSubscriber =
    new RedisSubscriber(redisURI(host, port), redisClient)

  private def subscriber(): Future[EventSubscriber] = async {
    val uri: URI = await(eventServiceResolver.uri)
    subscriber(uri.getHost, uri.getPort)
  }

  private def redisURI(host: String, port: Int) =
    RedisURI.Builder.sentinel(host, port, masterId).build()

}
