package csw.services.alarm.cli

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.apps.clusterseed.client.HTTPLocationService
import csw.commons.redis.EmbeddedRedis
import csw.messages.commons.CoordinatedShutdownReasons
import csw.services.BuildInfo
import csw.services.alarm.cli.args.ArgsParser
import csw.services.alarm.cli.wiring.Wiring
import csw.services.alarm.client.internal.commons.AlarmServiceConnection
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.models.TcpRegistration
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import csw.services.logging.commons.LogAdminActorFactory
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.SpanSugar.convertFloatToGrainOfTime
import org.scalatest.{BeforeAndAfterEach, Matchers}
import redis.embedded.{RedisSentinel, RedisServer}

import scala.collection.mutable

trait AlarmCliTestSetup
    extends HTTPLocationService
    with Matchers
    with BeforeAndAfterEach
    with EmbeddedRedis
    with ScalaFutures
    with Eventually {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  implicit val system: ActorSystem    = ActorSystemFactory.remote()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val argsParser                        = new ArgsParser(BuildInfo.name)
  val logBuffer: mutable.Buffer[String] = mutable.Buffer.empty[String]

  val (localHttpClient: LocationService, redisSentinel: RedisSentinel, redisServer: RedisServer) =
    withSentinel(masterId = ConfigFactory.load().getString("csw-alarm.redis.masterId")) { (sentinelPort, _) ⇒
      val localHttpClient: LocationService = LocationServiceFactory.makeLocalHttpClient
      localHttpClient
        .register(TcpRegistration(AlarmServiceConnection.value, sentinelPort, LogAdminActorFactory.make(system)))
        .await
      localHttpClient
    }

  private def printLine(msg: Any): Unit = logBuffer += msg.toString

  val cliWiring: Wiring = Wiring.make(system, localHttpClient, printLine)

  override protected def afterEach(): Unit = {
    super.afterEach()
    logBuffer.clear()
  }

  override def afterAll(): Unit = {
    stopSentinel(redisSentinel, redisServer)
    cliWiring.actorRuntime.shutdown(CoordinatedShutdownReasons.testFinishedReason).await
    super.afterAll()
  }
}
