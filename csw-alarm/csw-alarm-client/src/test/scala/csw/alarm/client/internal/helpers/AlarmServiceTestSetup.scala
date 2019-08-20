package csw.alarm.client.internal.helpers

import akka.actor.typed
import akka.actor.typed.SpawnProtocol
import akka.stream.ActorMaterializer
import akka.stream.typed.scaladsl
import com.typesafe.config.ConfigFactory
import csw.alarm.api.internal.{MetadataKey, SeverityKey}
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.{AlarmMetadata, FullAlarmSeverity}
import csw.alarm.api.scaladsl.AlarmAdminService
import csw.alarm.client.AlarmServiceFactory
import csw.alarm.client.internal.commons.Settings
import csw.alarm.client.internal.commons.serviceresolver.AlarmServiceHostPortResolver
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.alarm.client.internal.redis.RedisConnectionsFactory
import csw.commons.redis.EmbeddedRedis
import csw.network.utils.SocketUtils.getFreePort
import io.lettuce.core.RedisClient
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}
import romaine.RomaineFactory
import romaine.async.RedisAsyncApi

import scala.concurrent.ExecutionContext

class AlarmServiceTestSetup
    extends FunSuite
    with Matchers
    with MockitoSugar
    with EmbeddedRedis
    with AlarmTestData
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  val hostname = "localhost"
  val alarmServer = "alarmServer"

  val (sentinelPort, serverPort) = (getFreePort, getFreePort)

  private val (sentinel, server) = startSentinel(sentinelPort, serverPort, masterId = alarmServer)

  private val resolver = new AlarmServiceHostPortResolver(hostname, sentinelPort)

  private val redisClient = RedisClient.create()

  implicit val actorSystem: typed.ActorSystem[SpawnProtocol] = typed.ActorSystem(SpawnProtocol.behavior, "alarm-server")
  implicit val ec: ExecutionContext = actorSystem.executionContext
  implicit val mat: ActorMaterializer = scaladsl.ActorMaterializer()

  val alarmServiceFactory = new AlarmServiceFactory(redisClient)
  val alarmService: AlarmAdminService = alarmServiceFactory.makeAdminApi(hostname, sentinelPort)
  val jAlarmService: IAlarmService = alarmServiceFactory.jMakeClientApi(hostname, sentinelPort, actorSystem)

  import csw.alarm.client.internal.AlarmCodec._

  val connsFactory: RedisConnectionsFactory = new RedisConnectionsFactory(resolver, alarmServer, new RomaineFactory(redisClient))
  val testMetadataApi: RedisAsyncApi[MetadataKey, AlarmMetadata] = connsFactory.asyncApi[MetadataKey, AlarmMetadata]
  val testSeverityApi: RedisAsyncApi[SeverityKey, FullAlarmSeverity] = connsFactory.asyncApi[SeverityKey, FullAlarmSeverity]

  override protected def afterAll(): Unit = {
    redisClient.shutdown()
    stopSentinel(sentinel, server)
    actorSystem.terminate()
    actorSystem.whenTerminated.await
  }

  def settings: Settings = new Settings(ConfigFactory.load())

  val redisConnectionsFactory: RedisConnectionsFactory = connsFactory
}
