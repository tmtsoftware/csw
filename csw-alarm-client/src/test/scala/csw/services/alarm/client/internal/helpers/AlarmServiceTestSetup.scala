package csw.services.alarm.client.internal.helpers
import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{typed, ActorSystem}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.commons.redis.EmbeddedRedis
import csw.commons.utils.SocketUtils.getFreePort
import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.models.{AlarmMetadata, AlarmStatus, FullAlarmSeverity}
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.commons.serviceresolver.AlarmServiceHostPortResolver
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import csw.services.alarm.client.internal.shelve.{ShelveTimeoutActorFactory, ShelveTimeoutMessage, Unshelvable}
import csw.services.location.commons.ActorSystemFactory
import io.lettuce.core.RedisClient
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}
import romaine.RomaineFactory
import romaine.async.RedisAsyncApi
import romaine.codec.RomaineStringCodec

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

  implicit val actorSystem: ActorSystem               = ActorSystemFactory.remote()
  implicit val ec: ExecutionContext                   = actorSystem.dispatcher
  implicit val typedActorSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val mat: ActorMaterializer                 = ActorMaterializer()

  val alarmServiceFactory             = new AlarmServiceFactory(redisClient)
  val alarmService: AlarmAdminService = alarmServiceFactory.makeAdminApi(hostname, sentinelPort).await
  val jAlarmService: IAlarmService    = alarmServiceFactory.jMakeClientApi(hostname, sentinelPort, actorSystem).get()

  import csw.services.alarm.client.internal.AlarmCodec._

  implicit val metadataRomainCodec: RomaineStringCodec[AlarmMetadata]     = viaJsonCodec
  implicit val severityRomainCodec: RomaineStringCodec[FullAlarmSeverity] = viaJsonCodec
  implicit val statusRomainCodec: RomaineStringCodec[AlarmStatus]         = viaJsonCodec

  val connsFactory: RedisConnectionsFactory                      = new RedisConnectionsFactory(resolver, alarmServer, new RomaineFactory(redisClient))
  val testMetadataApi: RedisAsyncApi[MetadataKey, AlarmMetadata] = connsFactory.asyncApi[MetadataKey, AlarmMetadata].await
  val testSeverityApi: RedisAsyncApi[SeverityKey, FullAlarmSeverity] =
    connsFactory.asyncApi[SeverityKey, FullAlarmSeverity].await
  val testStatusApi: RedisAsyncApi[StatusKey, AlarmStatus] = connsFactory.asyncApi[StatusKey, AlarmStatus].await

  override protected def afterAll(): Unit = {
    redisClient.shutdown()
    stopSentinel(sentinel, server)
    actorSystem.terminate().await
  }

  def settings: Settings = new Settings(ConfigFactory.load())

  val redisConnectionsFactory: RedisConnectionsFactory = connsFactory

  val shelvingTimeoutProbe: TestInbox[ShelveTimeoutMessage] = TestInbox[ShelveTimeoutMessage]()
  val shelveTimeoutActorFactory: ShelveTimeoutActorFactory  = mock[ShelveTimeoutActorFactory]
  when(shelveTimeoutActorFactory.make(any[Unshelvable](), any[Int])(any[ActorSystem])).thenReturn(shelvingTimeoutProbe.ref)

}
