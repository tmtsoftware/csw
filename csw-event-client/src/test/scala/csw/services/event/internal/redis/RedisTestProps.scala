package csw.services.event.internal.redis

import akka.actor.{ActorSystem, CoordinatedShutdown}
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.wiring.BaseProperties
import csw.services.event.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.services.event.scaladsl._
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.{ClientOptions, RedisClient}
import redis.embedded.{RedisSentinel, RedisServer}

import scala.compat.java8.FutureConverters.CompletionStageOps

class RedisTestProps(
    name: String,
    sentinelPort: Int,
    serverPort: Int,
    val redisClient: RedisClient,
    locationService: LocationService,
)(implicit val actorSystem: ActorSystem)
    extends BaseProperties {

  private val redis: RedisServer = RedisServer.builder().port(serverPort).build()
  private val redisSentinel: RedisSentinel = RedisSentinel
    .builder()
    .port(sentinelPort)
    .masterName("eventServer")
    .masterPort(serverPort)
    .quorumSize(1)
    .build()

  override val eventPattern: String = "*sys*"

  val eventService: EventService   = RedisEventServiceFactory.make(locationService, redisClient)
  val jEventService: IEventService = RedisEventServiceFactory.jMake(locationService.asJava, redisClient, typedActorSystem)

  val publisher: EventPublisher   = eventService.defaultPublisher.await
  val subscriber: EventSubscriber = eventService.defaultSubscriber.await

  override def toString: String = name

  override def jPublisher[T <: EventPublisher]: IEventPublisher = jEventService.defaultPublisher.toScala.await

  override def jSubscriber[T <: EventSubscriber]: IEventSubscriber = jEventService.defaultSubscriber.toScala.await

  override def start(): Unit = {
    redisSentinel.start()
    redis.start()
  }

  override def shutdown(): Unit = {
    publisher.shutdown().await
    redisClient.shutdown()
    redisSentinel.stop()
    redis.stop()
    CoordinatedShutdown(actorSystem).run(TestFinishedReason).await
  }
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

    new RedisTestProps("Redis", sentinelPort, serverPort, redisClient, locationService)(clusterSettings.system)
  }
}
