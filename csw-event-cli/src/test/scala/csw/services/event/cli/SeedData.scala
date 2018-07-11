package csw.services.event.cli

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.apps.clusterseed.client.HTTPLocationService
import csw.messages.commons.CoordinatedShutdownReasons
import csw.messages.events._
import csw.messages.params.formats.JsonSupport
import csw.services.event.cli.args.ArgsParser
import csw.services.event.cli.wiring.Wiring
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.commons.EventServiceConnection
import csw.services.event.scaladsl.EventPublisher
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.models.TcpRegistration
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import csw.services.logging.commons.LogAdminActorFactory
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.libs.json.Json
import redis.embedded.{RedisSentinel, RedisServer}

import scala.collection.mutable
import scala.io.Source

trait SeedData extends HTTPLocationService with Matchers with BeforeAndAfterEach {

  implicit val system: ActorSystem    = ActorSystemFactory.remote()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val argsParser                        = new ArgsParser("csw-event-cli")
  val logBuffer: mutable.Buffer[String] = mutable.Buffer.empty[String]

  val (localHttpClient: LocationService, redisServer: RedisServer, redisSentinel: RedisSentinel) =
    startAndRegisterRedis(sentinelPort = 26379, serverPort = 6379)

  private def printLine(msg: Any): Unit = {
    logBuffer += msg.toString
  }

  val cliWiring: Wiring = Wiring.make(system, localHttpClient, printLine)

  val (event1: SystemEvent, event2: ObserveEvent) = seedEvents()

  override protected def afterEach(): Unit = {
    super.afterEach()
    logBuffer.clear()
  }

  override def afterAll(): Unit = {
    redisServer.stop()
    redisSentinel.stop()
    cliWiring.actorRuntime.shutdown(CoordinatedShutdownReasons.testFinishedReason).await
    super.afterAll()
  }

  private def startAndRegisterRedis(sentinelPort: Int, serverPort: Int): (LocationService, RedisServer, RedisSentinel) = {
    val localHttpClient: LocationService = LocationServiceFactory.makeLocalHttpClient
    val redisServer: RedisServer         = RedisServer.builder().port(serverPort).build()

    val redisSentinel: RedisSentinel = RedisSentinel
      .builder()
      .port(sentinelPort)
      .masterName(ConfigFactory.load().getString("redis.masterId"))
      .masterPort(serverPort)
      .quorumSize(1)
      .build()

    redisServer.start()
    redisSentinel.start()

    localHttpClient.register(TcpRegistration(EventServiceConnection.value, sentinelPort, LogAdminActorFactory.make(system))).await

    (localHttpClient, redisServer, redisSentinel)
  }

  def seedEvents(): (SystemEvent, ObserveEvent) = {
    val event1Str = Source.fromResource("seedData/event1.json").mkString
    val event2Str = Source.fromResource("seedData/event2.json").mkString

    val e1 = JsonSupport.readEvent[SystemEvent](Json.parse(event1Str))
    val e2 = JsonSupport.readEvent[ObserveEvent](Json.parse(event2Str))

    val publisher: EventPublisher = cliWiring.eventService.defaultPublisher.await
    publisher.publish(e1).await
    publisher.publish(e2).await

    (e1, e2)
  }
}
