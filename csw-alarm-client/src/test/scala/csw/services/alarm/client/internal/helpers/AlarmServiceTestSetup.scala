package csw.services.alarm.client.internal.helpers
import akka.actor.ActorSystem
import csw.commons.redis.EmbeddedRedis
import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.models.{AlarmMetadata, AlarmSeverity, AlarmStatus}
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.alarm.client.internal.AlarmCodec.{MetadataCodec, SeverityCodec, StatusCodec}
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import io.lettuce.core.{RedisClient, RedisURI}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}
import romaine.RedisAsyncScalaApi

import scala.concurrent.ExecutionContext

class AlarmServiceTestSetup(sentinelPort: Int, serverPort: Int)
    extends FunSuite
    with Matchers
    with EmbeddedRedis
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  private val hostname           = "localhost"
  private val alarmServer        = "alarmServer"
  private val (sentinel, server) = startSentinel(sentinelPort, serverPort, masterId = alarmServer)

  private val redisURI                 = RedisURI.Builder.sentinel(hostname, sentinelPort, alarmServer).build()
  private val redisClient: RedisClient = RedisClient.create()

  implicit val system: ActorSystem  = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher

  val alarmServiceFactory             = new AlarmServiceFactory(redisClient)
  val alarmService: AlarmAdminService = alarmServiceFactory.adminApi(hostname, sentinelPort).await
  val jAlarmService: IAlarmService    = alarmServiceFactory.jClientApi(hostname, sentinelPort, system).await

  val connsFactory: RedisConnectionsFactory                           = new RedisConnectionsFactory(redisClient, redisURI)
  val testMetadataApi: RedisAsyncScalaApi[MetadataKey, AlarmMetadata] = connsFactory.wrappedAsyncConnection(MetadataCodec).await
  val testSeverityApi: RedisAsyncScalaApi[SeverityKey, AlarmSeverity] = connsFactory.wrappedAsyncConnection(SeverityCodec).await
  val testStatusApi: RedisAsyncScalaApi[StatusKey, AlarmStatus]       = connsFactory.wrappedAsyncConnection(StatusCodec).await

  override protected def afterAll(): Unit = {
    redisClient.shutdown()
    stopSentinel(sentinel, server)
    system.terminate().await
  }
}
