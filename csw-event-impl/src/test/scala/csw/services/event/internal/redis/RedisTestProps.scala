package csw.services.event.internal.redis

import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.commons.{BaseProperties, Wiring}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, RedisFactory}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import redis.embedded.RedisServer

class RedisTestProps(redisPort: Int, clusterSettings: ClusterSettings, locationService: LocationService) extends BaseProperties {
  val wiring                      = new Wiring(clusterSettings.system)
  val redis: RedisServer          = RedisServer.builder().setting(s"bind ${clusterSettings.hostname}").port(redisPort).build()
  val redisClient: RedisClient    = RedisClient.create()
  val redisFactory                = new RedisFactory(redisClient, locationService, wiring)
  val publisher: EventPublisher   = redisFactory.publisher().await
  val subscriber: EventSubscriber = redisFactory.subscriber().await

  override def toString: String = "Redis"
}

object RedisTestProps {
  def createRedisProperties(seedPort: Int, serverPort: Int): RedisTestProps = {
    val (clusterSettings: ClusterSettings, locationService: LocationService) = BaseProperties.createInfra(seedPort, serverPort)
    new RedisTestProps(serverPort, clusterSettings, locationService)
  }
}
