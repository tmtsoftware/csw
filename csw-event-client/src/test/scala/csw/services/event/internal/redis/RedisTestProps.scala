package csw.services.event.internal.redis

import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.wiring.{BaseProperties, EventServiceResolver, Wiring}
import csw.services.event.javadsl.{IEventPublisher, IEventSubscriber, JRedisSentinelFactory}
import csw.services.event.scaladsl._
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.{ClientOptions, RedisClient}
import redis.embedded.{RedisSentinel, RedisServer}

import scala.compat.java8.FutureConverters.CompletionStageOps

class RedisTestProps(
    name: String,
    sentinelPort: Int,
    serverPort: Int,
    val redisFactory: RedisSentinelFactory,
    val jRedisFactory: JRedisSentinelFactory,
    locationService: LocationService,
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

  private val masterId              = "mymaster"
  override val eventPattern: String = "*sys*"

  val publisher: EventPublisher   = redisFactory.publisher(masterId).await
  val subscriber: EventSubscriber = redisFactory.subscriber(masterId).await

  override def toString: String = name

  override def jPublisher[T <: EventPublisher]: IEventPublisher = jRedisFactory.publisher(masterId).toScala.await

  override def jSubscriber[T <: EventSubscriber]: IEventSubscriber = jRedisFactory.subscriber(masterId).toScala.await
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
    redisClient.setOptions(clientOptions)

    val wiring = new Wiring(clusterSettings.system)
    import wiring._

    val eventPublisherUtil  = new EventPublisherUtil()
    val eventSubscriberUtil = new EventSubscriberUtil()

    val redisFactory =
      new RedisSentinelFactory(redisClient, new EventServiceResolver(locationService), eventPublisherUtil, eventSubscriberUtil)
    val jRedisFactory = new JRedisSentinelFactory(redisFactory)

    new RedisTestProps(
      "Redis",
      sentinelPort,
      serverPort,
      redisFactory,
      jRedisFactory,
      locationService,
      wiring,
      redisClient
    )
  }
}
