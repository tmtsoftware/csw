package csw.services.event.internal

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.event.internal.redis.RedisEventBusDriver
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.ExecutionContext

class Wiring(val redisPort: Int = 4545) {
  lazy val redisURI: RedisURI       = RedisURI.create("localhost", redisPort)
  lazy val redisClient: RedisClient = RedisClient.create(redisURI)

  implicit lazy val actorSystem: ActorSystem = ActorSystem()
  implicit lazy val mat: Materializer        = ActorMaterializer()
  implicit lazy val ec: ExecutionContext     = actorSystem.dispatcher

  lazy val eventBusDriver = new RedisEventBusDriver(redisClient, redisURI)
}
