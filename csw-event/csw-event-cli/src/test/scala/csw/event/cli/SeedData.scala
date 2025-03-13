/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.cli

import com.typesafe.config.ConfigFactory
import csw.commons.redis.EmbeddedRedis
import csw.event.api.scaladsl.EventPublisher
import csw.event.cli.args.ArgsParser
import csw.event.cli.wiring.Wiring
import csw.event.client.helpers.TestFutureExt.given
import scala.language.implicitConversions

import csw.event.client.internal.commons.EventServiceConnection
import csw.location.api.models.TcpRegistration
import csw.location.server.http.HTTPLocationService
import csw.params.core.formats.JsonSupport
import csw.params.events.*
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Json
import redis.embedded.{RedisSentinel, RedisServer}

import scala.collection.mutable
import scala.io.Source
import org.scalatest.matchers.should.Matchers

trait SeedData extends HTTPLocationService with Matchers with BeforeAndAfterEach with EmbeddedRedis {

  val cliWiring: Wiring = Wiring.make(_printLine = printLine)
  import cliWiring._

  val argsParser                        = new ArgsParser("csw-event-cli")
  val logBuffer: mutable.Buffer[String] = mutable.Buffer.empty[String]
  var redisSentinel: RedisSentinel      = scala.compiletime.uninitialized
  var redisServer: RedisServer          = scala.compiletime.uninitialized
  var event1: SystemEvent               = scala.compiletime.uninitialized
  var event2: ObserveEvent              = scala.compiletime.uninitialized

  private def printLine(msg: Any): Unit = logBuffer += msg.toString

  override def beforeAll(): Unit = {
    super.beforeAll()
    val (_, sentinel: RedisSentinel, server: RedisServer) =
      withSentinel(masterId = ConfigFactory.load().getString("csw-event.redis.masterId"), keyspace = true) { (sentinelPort, _) =>
        locationService.register(TcpRegistration(EventServiceConnection.value, sentinelPort)).await
      }
    redisSentinel = sentinel
    redisServer = server

    val events = seedEvents()
    event1 = events._1
    event2 = events._2
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    logBuffer.clear()
  }

  override def afterAll(): Unit = {
    stopSentinel(redisSentinel, redisServer)
    cliWiring.actorRuntime.shutdown().await
    super.afterAll()
  }

  def seedEvents(): (SystemEvent, ObserveEvent) = {
    val event1Str = Source.fromResource("seedData/event1.json").mkString
    val event2Str = Source.fromResource("seedData/event2.json").mkString

    val e1 = JsonSupport.readEvent[SystemEvent](Json.parse(event1Str))
    val e2 = JsonSupport.readEvent[ObserveEvent](Json.parse(event2Str))

    val publisher: EventPublisher = cliWiring.eventService.defaultPublisher
    publisher.publish(e1).await
    publisher.publish(e2).await

    (e1, e2)
  }
}
