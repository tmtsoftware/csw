package csw.services.event.cli

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.apps.clusterseed.client.HTTPLocationService
import csw.commons.redis.EmbeddedRedis
import csw.messages.commons.CoordinatedShutdownReasons
import csw.messages.events._
import csw.messages.params.formats.JsonSupport
import csw.messages.location.scaladsl.LocationService
import csw.services.event.api.scaladsl.EventPublisher
import csw.services.event.cli.args.ArgsParser
import csw.services.event.cli.wiring.Wiring
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.commons.EventServiceConnection
import csw.services.location.commons.ActorSystemFactory
import csw.messages.location.models.TcpRegistration
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.logging.commons.LogAdminActorFactory
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.libs.json.Json
import redis.embedded.{RedisSentinel, RedisServer}

import scala.collection.mutable
import scala.io.Source

trait SeedData extends HTTPLocationService with Matchers with BeforeAndAfterEach with EmbeddedRedis {

  implicit val system: ActorSystem    = ActorSystemFactory.remote()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val argsParser                        = new ArgsParser("csw-event-cli")
  val logBuffer: mutable.Buffer[String] = mutable.Buffer.empty[String]

  val (localHttpClient: LocationService, redisSentinel: RedisSentinel, redisServer: RedisServer) =
    withSentinel(masterId = ConfigFactory.load().getString("csw-event.redis.masterId")) { (sentinelPort, _) ⇒
      val localHttpClient: LocationService = LocationServiceFactory.makeLocalHttpClient
      localHttpClient
        .register(TcpRegistration(EventServiceConnection.value, sentinelPort, LogAdminActorFactory.make(system)))
        .await
      localHttpClient
    }

  private def printLine(msg: Any): Unit = logBuffer += msg.toString

  val cliWiring: Wiring = Wiring.make(system, localHttpClient, printLine)

  val (event1: SystemEvent, event2: ObserveEvent) = seedEvents()

  override protected def afterEach(): Unit = {
    super.afterEach()
    logBuffer.clear()
  }

  override def afterAll(): Unit = {
    stopSentinel(redisSentinel, redisServer)
    cliWiring.actorRuntime.shutdown(CoordinatedShutdownReasons.testFinishedReason).await
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
