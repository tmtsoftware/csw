package csw.services.event.internal.redis

import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.wiring.{BaseProperties, EventServiceResolver, Wiring}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, RedisFactory}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.{ClientOptions, RedisClient}
import redis.embedded.RedisServer

class RedisTestProps(
    name: String,
    redisPort: Int,
    clusterSettings: ClusterSettings,
    val redisFactory: RedisFactory,
    val locationService: LocationService,
    val wiring: Wiring,
    val redisClient: RedisClient
) extends BaseProperties {
  val redis: RedisServer = RedisServer.builder().setting(s"bind ${clusterSettings.hostname}").port(redisPort).build()

  val publisher: EventPublisher   = redisFactory.publisher().await
  val subscriber: EventSubscriber = redisFactory.subscriber().await

  override def toString: String = name
}

object RedisTestProps {
  def createRedisProperties(
      seedPort: Int,
      serverPort: Int,
      clientOptions: ClientOptions = ClientOptions.create()
  ): RedisTestProps = {
    val (clusterSettings, locationService) = BaseProperties.createInfra(seedPort, serverPort)
    val redisClient: RedisClient           = RedisClient.create()
    val wiring                             = new Wiring(clusterSettings.system)
    import wiring._
    val eventPublisherUtil  = new EventPublisherUtil()
    val eventSubscriberUtil = new EventSubscriberUtil()

    val redisFactory =
      new RedisFactory(redisClient, new EventServiceResolver(locationService), eventPublisherUtil, eventSubscriberUtil)
    redisClient.setOptions(clientOptions)
    new RedisTestProps("Redis", serverPort, clusterSettings, redisFactory, locationService, wiring, redisClient)
  }
}
