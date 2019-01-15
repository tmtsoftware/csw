package csw.alarm.cli

import akka.actor.CoordinatedShutdown.UnknownReason
import com.typesafe.config.ConfigFactory
import csw.alarm.cli.args.ArgsParser
import csw.alarm.cli.utils.TestFutureExt.RichFuture
import csw.alarm.cli.wiring.Wiring
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.commons.redis.EmbeddedRedis
import csw.location.api.models.TcpRegistration
import csw.location.server.http.HTTPLocationService
import csw.services.BuildInfo
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar.convertFloatToGrainOfTime
import redis.embedded.{RedisSentinel, RedisServer}

import scala.collection.mutable

trait AlarmCliTestSetup extends HTTPLocationService with EmbeddedRedis with Eventually {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  val cliWiring: Wiring = Wiring.make(_printLine = printLine)
  import cliWiring._

  val argsParser                        = new ArgsParser(BuildInfo.name)
  val logBuffer: mutable.Buffer[String] = mutable.Buffer.empty[String]

  var redisSentinel: RedisSentinel = _
  var redisServer: RedisServer     = _

  private def printLine(msg: Any): Unit = logBuffer += msg.toString

  override def beforeAll(): Unit = {
    super.beforeAll()
    val (_, sentinel: RedisSentinel, server: RedisServer) =
      withSentinel(masterId = ConfigFactory.load().getString("csw-alarm.redis.masterId")) { (sentinelPort, _) ⇒
        locationService
          .register(TcpRegistration(AlarmServiceConnection.value, sentinelPort))
          .futureValue
      }
    redisSentinel = sentinel
    redisServer = server
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    logBuffer.clear()
  }

  override def afterAll(): Unit = {
    stopSentinel(redisSentinel, redisServer)
    cliWiring.actorRuntime.shutdown(UnknownReason).await
    super.afterAll()
  }
}
