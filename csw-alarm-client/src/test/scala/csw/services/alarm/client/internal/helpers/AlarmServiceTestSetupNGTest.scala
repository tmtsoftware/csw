package csw.services.alarm.client.internal.helpers

import akka.actor.ActorSystem
import csw.commons.redis.EmbeddedRedis
import csw.commons.utils.SocketUtils._
import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.models.{AlarmMetadata, AlarmSeverity, AlarmStatus}
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.alarm.client.internal.AlarmCodec.{MetadataCodec, SeverityCodec, StatusCodec}
import csw.services.alarm.client.internal.commons.serviceresolver.AlarmServiceHostPortResolver
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import csw.services.location.commons.ActorSystemFactory
import io.lettuce.core.RedisClient
import org.scalatest.Matchers
import org.testng.annotations.AfterSuite
import romaine.RedisAsyncScalaApi

import scala.concurrent.ExecutionContext

class AlarmServiceTestSetupNGTest extends Matchers with EmbeddedRedis {
  val hostname    = "localhost"
  val alarmServer = "alarmServer"

  val (sentinelPort, serverPort) = (getFreePort, getFreePort)

  private val (sentinel, server) = startSentinel(sentinelPort, serverPort, masterId = alarmServer)

  private val resolver    = new AlarmServiceHostPortResolver(hostname, sentinelPort)
  private val redisClient = RedisClient.create()

  implicit val system: ActorSystem  = ActorSystemFactory.remote()
  implicit val ec: ExecutionContext = system.dispatcher

  val alarmServiceFactory             = new AlarmServiceFactory(redisClient)
  val alarmService: AlarmAdminService = alarmServiceFactory.makeAdminApi(hostname, sentinelPort).await
  val jAlarmService: IAlarmService    = alarmServiceFactory.jMakeClientApi(hostname, sentinelPort, system).get()

  val connsFactory: RedisConnectionsFactory                           = new RedisConnectionsFactory(redisClient, resolver, alarmServer)
  val testMetadataApi: RedisAsyncScalaApi[MetadataKey, AlarmMetadata] = connsFactory.wrappedAsyncConnection(MetadataCodec).await
  val testSeverityApi: RedisAsyncScalaApi[SeverityKey, AlarmSeverity] = connsFactory.wrappedAsyncConnection(SeverityCodec).await
  val testStatusApi: RedisAsyncScalaApi[StatusKey, AlarmStatus]       = connsFactory.wrappedAsyncConnection(StatusCodec).await

  @AfterSuite
  def afterAll(): Unit = {
    redisClient.shutdown()
    stopSentinel(sentinel, server)
    system.terminate().await
  }
}
