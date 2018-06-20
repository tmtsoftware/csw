package csw.services.event.internal.redis

import akka.stream.Materializer
import csw.services.event.internal.commons.serviceresolver.EventServiceResolver
import csw.services.event.scaladsl.{EventPublisher, EventService, EventSubscriber}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.{ExecutionContext, Future}

class RedisEventService(eventServiceResolver: EventServiceResolver, masterId: String, redisClient: RedisClient)(
    implicit val executionContext: ExecutionContext,
    mat: Materializer
) extends EventService {

  private val redisURI: Future[RedisURI] =
    eventServiceResolver.uri.map(uri ⇒ RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build())

  lazy val defaultPublisher: Future[EventPublisher] = makeNewPublisher()

  lazy val defaultSubscriber: Future[EventSubscriber] = redisURI.map(subscriber)

  override def makeNewPublisher(): Future[EventPublisher] = redisURI.map(publisher)

  private[csw] def publisher(redisURI: RedisURI): EventPublisher =
    new RedisPublisher(redisURI, redisClient)

  private[csw] def subscriber(redisURI: RedisURI): EventSubscriber =
    new RedisSubscriber(redisURI, redisClient)

}
