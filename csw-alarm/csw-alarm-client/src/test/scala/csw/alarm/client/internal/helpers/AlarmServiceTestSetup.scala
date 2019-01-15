package csw.alarm.client.internal.helpers

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{typed, ActorSystem}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.alarm.api.internal.{MetadataKey, SeverityKey}
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.api.models.{AlarmMetadata, FullAlarmSeverity}
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
  val hostname    = "localhost"
  val alarmServer = "alarmServer"

  val (sentinelPort, serverPort) = (getFreePort, getFreePort)

  private val (sentinel, server) = startSentinel(sentinelPort, serverPort, masterId = alarmServer)

  private val resolver    = new AlarmServiceHostPortResolver(hostname, sentinelPort)
  private val redisClient = RedisClient.create()

  implicit val actorSystem: ActorSystem               = ActorSystem("alarm-server")
  implicit val ec: ExecutionContext                   = actorSystem.dispatcher
  implicit val typedActorSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val mat: ActorMaterializer                 = ActorMaterializer()

  val alarmServiceFactory             = new AlarmServiceFactory(redisClient)
  val alarmService: AlarmAdminService = alarmServiceFactory.makeAdminApi(hostname, sentinelPort)
  val jAlarmService: IAlarmService    = alarmServiceFactory.jMakeClientApi(hostname, sentinelPort, actorSystem)

  import csw.alarm.client.internal.AlarmCodec._

  val connsFactory: RedisConnectionsFactory                          = new RedisConnectionsFactory(resolver, alarmServer, new RomaineFactory(redisClient))
  val testMetadataApi: RedisAsyncApi[MetadataKey, AlarmMetadata]     = connsFactory.asyncApi[MetadataKey, AlarmMetadata]
  val testSeverityApi: RedisAsyncApi[SeverityKey, FullAlarmSeverity] = connsFactory.asyncApi[SeverityKey, FullAlarmSeverity]

  override protected def afterAll(): Unit = {
    redisClient.shutdown()
    stopSentinel(sentinel, server)
    actorSystem.terminate().await
  }

  def settings: Settings = new Settings(ConfigFactory.load())

  val redisConnectionsFactory: RedisConnectionsFactory = connsFactory
}
