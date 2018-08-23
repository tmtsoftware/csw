package csw.services.event.internal.redis

import akka.stream.Materializer
import csw.services.event.api.scaladsl.EventService
import csw.services.event.internal.commons.serviceresolver.EventServiceResolver
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Implementation of [[csw.services.event.api.scaladsl.EventService]] which provides handle to [[csw.services.event.api.scaladsl.EventPublisher]]
 * and [[csw.services.event.api.scaladsl.EventSubscriber]] backed by Redis
 * @param eventServiceResolver to get the connection information of event service
 * @param masterId the Id used by Redis Sentinel to identify the master
 * @param redisClient the client instance of [[io.lettuce.core.RedisClient]]
 * @param executionContext the execution context to be used for performing asynchronous operations
 * @param mat the materializer to be used for materializing underlying streams
 */
class RedisEventService(eventServiceResolver: EventServiceResolver, masterId: String, redisClient: RedisClient)(
    implicit val executionContext: ExecutionContext,
    mat: Materializer
) extends EventService {

  override def makeNewPublisher(): RedisPublisher = new RedisPublisher(redisURI(), redisClient)

  override def makeNewSubscriber(): RedisSubscriber = new RedisSubscriber(redisURI(), redisClient)

  // resolve event service every time before creating a new publisher or subscriber
  private def redisURI(): Future[RedisURI] =
    eventServiceResolver.uri().map(uri ⇒ RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build())

}
