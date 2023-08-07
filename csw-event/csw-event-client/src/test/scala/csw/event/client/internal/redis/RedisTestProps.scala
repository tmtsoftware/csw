/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.redis

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.commons.redis.EmbeddedRedis
import csw.event.api.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.event.api.scaladsl.*
import csw.event.client.EventServiceFactory
import csw.event.client.helpers.TestFutureExt.given
import scala.language.implicitConversions

import csw.event.client.internal.commons.javawrappers.JEventService
import csw.event.client.internal.wiring.BaseProperties
import csw.event.client.internal.wiring.BaseProperties.createInfra
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.scaladsl.LocationService
import csw.location.server.http.HTTPLocationServiceOnPorts
import csw.network.utils.SocketUtils.getFreePort
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.{ClientOptions, RedisClient, RedisURI}
import redis.embedded.{RedisSentinel, RedisServer}

import scala.jdk.FutureConverters.*
import scala.concurrent.Future

class RedisTestProps(
    name: String,
    sentinelPort: Int,
    serverPort: Int,
    val redisClient: RedisClient,
    locationService: LocationService,
    locationServer: HTTPLocationServiceOnPorts
)(implicit val actorSystem: ActorSystem[SpawnProtocol.Command])
    extends BaseProperties
    with EmbeddedRedis {

  private lazy val masterId = ConfigFactory.load().getString("csw-event.redis.masterId")
  private lazy val redisURI = RedisURI.Builder.sentinel("localhost", sentinelPort, masterId).build()
  private lazy val asyncConnection: Future[RedisAsyncCommands[String, String]] =
    redisClient.connectAsync(new StringCodec(), redisURI).asScala.map(_.async())

  var redisSentinel: RedisSentinel = _
  var redisServer: RedisServer     = _

  override val eventPattern: String = "*"

  private val eventServiceFactory = new EventServiceFactory(RedisStore(redisClient))

  val eventService: EventService   = eventServiceFactory.make(locationService)
  val jEventService: IEventService = new JEventService(eventService)
  val publisher: EventPublisher    = eventService.defaultPublisher
  val subscriber: EventSubscriber  = eventService.defaultSubscriber

  override def toString: String = name

  override val jPublisher: IEventPublisher = jEventService.defaultPublisher

  override val jSubscriber: IEventSubscriber = jEventService.defaultSubscriber

  override def publishGarbage(channel: String, message: String): Future[Done] =
    asyncConnection.flatMap(c => c.publish(channel, message).asScala.map(_ => Done))

  override def start(): Unit = {
    val redis = startSentinel(sentinelPort, serverPort, masterId)
    redisSentinel = redis._1
    redisServer = redis._2
  }

  override def shutdown(): Unit = {
    publisher.shutdown().await
    redisClient.shutdown()
    stopSentinel(redisSentinel, redisServer)
    actorSystem.terminate()
    actorSystem.whenTerminated.await
    locationServer.afterAll()
  }
}

object RedisTestProps extends EmbeddedRedis {
  def createRedisProperties(
      clusterPort: Int = getFreePort,
      httpLocationServicePort: Int = getFreePort,
      sentinelPort: Int = getFreePort,
      serverPort: Int = getFreePort,
      clientOptions: ClientOptions = ClientOptions.create()
  ): RedisTestProps = {

    val locationServer = new HTTPLocationServiceOnPorts(clusterPort, httpLocationServicePort)
    locationServer.beforeAll()
    val (locationService, system) = createInfra(sentinelPort, httpLocationServicePort)
    val redisClient: RedisClient  = RedisClient.create()
    redisClient.setOptions(clientOptions)

    new RedisTestProps("Redis", sentinelPort, serverPort, redisClient, locationService, locationServer)(system)
  }

  def jCreateRedisProperties(clientOptions: ClientOptions): RedisTestProps = createRedisProperties(clientOptions = clientOptions)

  def jCreateRedisProperties(): RedisTestProps = createRedisProperties()
}
