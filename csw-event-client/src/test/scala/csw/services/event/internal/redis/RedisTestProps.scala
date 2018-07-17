package csw.services.event.internal.redis

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import com.typesafe.config.ConfigFactory
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.commons.javawrappers.JEventService
import csw.services.event.internal.wiring.BaseProperties
import csw.services.event.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.services.event.scaladsl._
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.{ClientOptions, RedisClient, RedisURI}
import redis.embedded.{RedisSentinel, RedisServer}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Future

class RedisTestProps(
    name: String,
    sentinelPort: Int,
    serverPort: Int,
    val redisClient: RedisClient,
    locationService: LocationService,
)(implicit val actorSystem: ActorSystem)
    extends BaseProperties {

  val redis: RedisServer    = RedisServer.builder().port(serverPort).build()
  private lazy val masterId = ConfigFactory.load().getString("redis.masterId")
  private lazy val redisURI = RedisURI.Builder.sentinel("localhost", sentinelPort, masterId).build()
  private lazy val asyncConnection: Future[RedisAsyncCommands[String, String]] =
    redisClient.connectAsync(new StringCodec(), redisURI).toScala.map(_.async())

  private lazy val redisSentinel: RedisSentinel = RedisSentinel
    .builder()
    .port(sentinelPort)
    .masterName(masterId)
    .masterPort(serverPort)
    .quorumSize(1)
    .build()

  override val eventPattern: String = "*"

  private val eventServiceFactory = new RedisEventServiceFactory(redisClient)

  val eventService: EventService       = eventServiceFactory.make(locationService)
  val jEventService: IEventService     = new JEventService(eventService)
  lazy val publisher: EventPublisher   = eventService.defaultPublisher.await
  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber.await

  override def toString: String = name

  override lazy val jPublisher: IEventPublisher = jEventService.defaultPublisher.get()

  override lazy val jSubscriber: IEventSubscriber = jEventService.defaultSubscriber.get()

  override def publishGarbage(channel: String, message: String): Future[Done] =
    asyncConnection.flatMap(c ⇒ c.publish(channel, message).toScala.map(_ ⇒ Done))

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
