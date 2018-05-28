package csw.services.event.internal.redis

import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.wiring.{BaseProperties, EventServiceResolver, Wiring}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, RedisSentinelFactory}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.{ClientOptions, RedisClient}
import redis.embedded.{RedisSentinel, RedisServer}

class RedisTestProps(
    name: String,
    sentinelPort: Int,
    serverPort: Int,
    clusterSettings: ClusterSettings,
    val redisFactory: RedisSentinelFactory,
    val locationService: LocationService,
    val wiring: Wiring,
    val redisClient: RedisClient
) extends BaseProperties {
  val redis: RedisServer = RedisServer.builder().port(serverPort).build()
  val redisSentinel: RedisSentinel = RedisSentinel
    .builder()
    .port(sentinelPort)
    .masterPort(serverPort)
    .quorumSize(1)
    .build()

  val publisher: EventPublisher   = redisFactory.publisher("mymaster").await
  val subscriber: EventSubscriber = redisFactory.subscriber("mymaster").await

  override def toString: String = name
}

object RedisTestProps {
  def createRedisProperties(
      seedPort: Int,
      sentinelPort: Int,
      serverPort: Int,
      clientOptions: ClientOptions = ClientOptions.create()
  ): RedisTestProps = {
    val (clusterSettings, locationService) = BaseProperties.createInfra(seedPort, sentinelPort)
    val redisClient: RedisClient           = RedisClient.create()
    val wiring                             = new Wiring(clusterSettings.system)
    import wiring._
    val eventPublisherUtil  = new EventPublisherUtil()
    val eventSubscriberUtil = new EventSubscriberUtil()

    val redisFactory =
      new RedisSentinelFactory(redisClient, new EventServiceResolver(locationService), eventPublisherUtil, eventSubscriberUtil)
    redisClient.setOptions(clientOptions)
    new RedisTestProps("Redis", sentinelPort, serverPort, clusterSettings, redisFactory, locationService, wiring, redisClient)
  }
}
