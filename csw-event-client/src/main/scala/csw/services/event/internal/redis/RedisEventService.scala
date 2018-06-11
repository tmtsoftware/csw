package csw.services.event.internal.redis

import akka.stream.Materializer
import csw.services.event.scaladsl.{EventPublisher, EventService, EventSubscriber}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.ExecutionContext

class RedisEventService(host: String, port: Int, masterId: String, redisClient: RedisClient)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends EventService {

  private val redisURI                      = RedisURI.Builder.sentinel(host, port, masterId).build()
  lazy val defaultPublisher: EventPublisher = publisher()

  lazy val defaultSubscriber: EventSubscriber = subscriber()

  override def makeNewPublisher(): EventPublisher = publisher()

  private[csw] def publisher(): EventPublisher =
    new RedisPublisher(redisURI, redisClient)

  private[csw] def subscriber(): EventSubscriber =
    new RedisSubscriber(redisURI, redisClient)

}
