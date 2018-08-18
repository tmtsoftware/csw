package csw.services.alarm.client.internal.helpers
import akka.actor.ActorSystem
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
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.location.commons.ActorSystemFactory
import io.lettuce.core.RedisClient
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}
import romaine.RomaineFactory
import romaine.async.RedisAsyncApi

import scala.concurrent.ExecutionContext

class AlarmServiceTestSetup
    extends FunSuite
    with Matchers
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

  implicit val actorSystem: ActorSystem = ActorSystemFactory.remote()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher

  val alarmServiceFactory             = new AlarmServiceFactory(redisClient)
  val alarmService: AlarmAdminService = alarmServiceFactory.makeAdminApi(hostname, sentinelPort).await
  val jAlarmService: IAlarmService    = alarmServiceFactory.jMakeClientApi(hostname, sentinelPort, actorSystem).get()

  import csw.services.alarm.client.internal.AlarmCodec._
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

  def shelveTimeoutActorFactory: ShelveTimeoutActorFactory = new ShelveTimeoutActorFactory()
}
