package csw.services.event.cli

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.apps.clusterseed.client.HTTPLocationService
import csw.messages.commons.CoordinatedShutdownReasons
import csw.messages.events.{EventName, ObserveEvent, SystemEvent}
import csw.messages.params.generics.KeyType
import csw.messages.params.models.{Prefix, Struct}
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.commons.EventServiceConnection
import csw.services.event.scaladsl.EventPublisher
import csw.services.location.models.TcpRegistration
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import csw.services.logging.commons.LogAdminActorFactory
import org.scalatest.{FunSuite, Matchers}
import redis.embedded.{RedisSentinel, RedisServer}

import scala.collection.mutable

class CommandLineRunnerTest extends FunSuite with Matchers with HTTPLocationService {

  implicit val system: ActorSystem    = ActorSystem("test")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  private val argsParser = new ArgsParser("csw-event-cli")
  private val logBuffer  = mutable.Buffer.empty[String]

  val (localHttpClient: LocationService, redisServer: RedisServer, redisSentinel: RedisSentinel) =
    startAndRegisterRedis(sentinelPort = 26379, serverPort = 6379)

  private val cliWiring = Wiring.make(system, localHttpClient, msg ⇒ logBuffer += msg.toString)

  private val (event1: SystemEvent, event2: ObserveEvent, expectedOut1: Set[String], expectedOut2: Set[String]) = seedEvents()

  override def afterAll(): Unit = {
    super.afterAll()
    redisServer.stop()
    redisSentinel.stop()
    cliWiring.actorRuntime.shutdown(CoordinatedShutdownReasons.testFinishedReason).await
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

  private def seedEvents(): (SystemEvent, ObserveEvent, Set[String], Set[String]) = {
    val prefix1: Prefix       = Prefix("wfos.prog.cloudcover")
    val prefix2: Prefix       = Prefix("wfos.prog.filter")
    val eventName1: EventName = EventName("move")
    val eventName2: EventName = EventName("stop")

    val ra           = KeyType.StringKey.make("ra")
    val dec          = KeyType.StringKey.make("dec")
    val epoch        = KeyType.DoubleKey.make("epoch")
    val timestampKey = KeyType.TimestampKey.make("timestamp")
    val structKey1   = KeyType.StructKey.make("struct-1")
    val structKey2   = KeyType.StructKey.make("struct-2")

    val rp  = ra.set("12:13:14.1")
    val dp  = dec.set("32:33:34.4")
    val ep  = epoch.set(1950.0)
    val tp  = timestampKey.set(Instant.now())
    val s1  = Struct(Set(rp, dp))
    val sp1 = structKey1.set(s1)
    val s2  = Struct(Set(sp1))
    val sp2 = structKey2.set(s2, s1)

    val e1 = SystemEvent(prefix1, eventName1).madd(sp1, ep)
    val e2 = ObserveEvent(prefix2, eventName2).madd(sp2, tp)

    val publisher: EventPublisher = cliWiring.eventService.defaultPublisher.await
    publisher.publish(e1).await
    publisher.publish(e2).await

    val expOut1 =
      Set(
        s"${e1.eventKey} ${sp1.keyName} = ${sp1.keyType}[${sp1.units}]",
        s"${e1.eventKey} ${sp1.keyName}/${rp.keyName} = ${rp.keyType}[${rp.units}]",
        s"${e1.eventKey} ${sp1.keyName}/${dp.keyName} = ${dp.keyType}[${dp.units}]",
        s"${e1.eventKey} ${ep.keyName} = ${ep.keyType}[${ep.units}]"
      )

    val expOut2 =
      Set(
        s"${e2.eventKey} ${sp2.keyName} = ${sp2.keyType}[${sp2.units}]",
        s"${e2.eventKey} ${sp2.keyName}/${sp1.keyName} = ${sp1.keyType}[${sp1.units}]",
        s"${e2.eventKey} ${sp2.keyName}/${sp1.keyName}/${rp.keyName} = ${rp.keyType}[${rp.units}]",
        s"${e2.eventKey} ${sp2.keyName}/${sp1.keyName}/${dp.keyName} = ${dp.keyType}[${dp.units}]",
        s"${e2.eventKey} ${sp2.keyName}/${rp.keyName} = ${rp.keyType}[${rp.units}]",
        s"${e2.eventKey} ${sp2.keyName}/${dp.keyName} = ${dp.keyType}[${dp.units}]",
        s"${e2.eventKey} ${tp.keyName} = ${tp.keyType}[${tp.units}]"
      )

    (e1, e2, expOut1, expOut2)
  }

  test("should able to inspect event/events containing multiple parameters including recursive structs") {
    import cliWiring._

    commandLineRunner.inspect(argsParser.parse(Seq("inspect", "-e", s"${event1.eventKey}")).get).await
    logBuffer.filterNot(_.startsWith("==")).toSet shouldEqual expectedOut1

    logBuffer.clear()

    commandLineRunner.inspect(argsParser.parse(Seq("inspect", "-e", s"${event2.eventKey}")).get).await
    logBuffer.filterNot(_.startsWith("==")).toSet shouldEqual expectedOut2

    logBuffer.clear()

    commandLineRunner.inspect(argsParser.parse(Seq("inspect", "-e", s"${event1.eventKey},${event2.eventKey}")).get).await
    logBuffer.filterNot(_.startsWith("==")).toSet shouldEqual (expectedOut1 ++ expectedOut2)

    logBuffer.clear()
  }

}
