package csw.services.alarm.client.internal
import java.io.File

import akka.actor.ActorSystem
import csw.commons.redis.EmbeddedRedis
import csw.services.alarm.api.exceptions.InvalidSeverityException
import csw.services.alarm.api.internal._
import csw.services.alarm.api.models.AcknowledgementStatus.Acknowledged
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmHealth.Bad
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.LatchStatus.Latched
import csw.services.alarm.api.models.ShelveStatus.UnShelved
import csw.services.alarm.api.models.{AlarmMetadata, AlarmSeverity, AlarmStatus, AlarmType}
import csw.services.alarm.client.internal.AlarmCodec.{MetadataCodec, SeverityCodec, StatusCodec}
import csw.services.alarm.client.internal.redis.scala_wrapper.{RedisAsyncScalaApi, RedisKeySpaceApi, RedisReactiveScalaApi}
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.Utf8StringCodec
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class AlarmServiceImplTest extends FunSuite with Matchers with EmbeddedRedis with BeforeAndAfterAll {
  private val alarmServer        = "AlarmServer"
  private val (sentinel, server) = startSentinel(26379, 6379, masterId = alarmServer)

  private val redisURI                 = RedisURI.Builder.sentinel("localhost", 26379, alarmServer).build()
  private val redisClient: RedisClient = RedisClient.create(redisURI)

  override protected def afterAll(): Unit = stopSentinel(sentinel, server)

  implicit val system: ActorSystem  = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher

  private val alarmServiceFactory    = new AlarmServiceFactory(redisURI, redisClient)
  val alarmService: AlarmServiceImpl = alarmServiceFactory.make()

  test("init alarms") {

    val path = getClass.getResource("/test-alarms/valid-alarms.conf").getPath

    val alarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    val file     = new File(path)
    Await.result(alarmService.initAlarms(file), 5.seconds)

    Await.result(alarmService.getMetadata(alarmKey), 5.seconds) shouldBe AlarmMetadata(
      subsystem = "nfiraos",
      component = "trombone",
      name = "tromboneAxisHighLimitAlarm",
      description = "Warns when trombone axis has reached the high limit",
      location = "south side",
      AlarmType.Absolute,
      Set(Indeterminate, Okay, Warning, Major),
      probableCause = "the trombone software has failed or the stage was driven into the high limit",
      operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
      isAutoAcknowledgeable = true,
      isLatchable = true,
      activationStatus = Active
    )

    Await.result(alarmService.getStatus(alarmKey), 5.seconds) shouldBe AlarmStatus()

    Await.result(alarmService.getSeverity(alarmKey), 5.seconds) shouldBe Disconnected

    Await.result(alarmService.getAggregatedHealth(alarmKey), 5.seconds) shouldBe Bad

  }

  test("test set severity") {
    val path = getClass.getResource("/test-alarms/valid-alarms.conf").getPath
    val file = new File(path)
    Await.result(alarmService.initAlarms(file, reset = true), 5.seconds)

    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    Await.result(alarmService.setSeverity(tromboneAxisHighLimitAlarm, AlarmSeverity.Major), 5.seconds)

    val status = Await.result(alarmServiceFactory.statusAsyncApi.get(tromboneAxisHighLimitAlarm), 5.seconds)

    status shouldEqual AlarmStatus(Acknowledged, Latched, Major, UnShelved)
  }

  test("should throw InvalidSeverityException when unsupported severity is provided") {
    val path = getClass.getResource("/test-alarms/valid-alarms.conf").getPath
    val file = new File(path)
    Await.result(alarmService.initAlarms(file, reset = true), 5.seconds)

    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    intercept[InvalidSeverityException] {
      Await.result(alarmService.setSeverity(tromboneAxisHighLimitAlarm, AlarmSeverity.Critical), 5.seconds)
    }
  }
}

// Fixme: provide factory in main scope
class AlarmServiceFactory(redisURI: RedisURI, redisClient: RedisClient)(implicit system: ActorSystem, ec: ExecutionContext)
    extends AlarmRW {

  private val metadataAsyncCommands: RedisAsyncCommands[MetadataKey, AlarmMetadata] =
    Await.result(redisClient.connectAsync(MetadataCodec, redisURI).toScala.map(_.async()), 5.seconds)
  private val statusAsyncCommands: RedisAsyncCommands[StatusKey, AlarmStatus] =
    Await.result(redisClient.connectAsync(StatusCodec, redisURI).toScala.map(_.async()), 5.seconds)
  private val severityAsyncCommands: RedisAsyncCommands[SeverityKey, AlarmSeverity] =
    Await.result(redisClient.connectAsync(SeverityCodec, redisURI).toScala.map(_.async()), 5.seconds)
  private val reactiveCommands: RedisPubSubReactiveCommands[String, String] =
    Await.result(redisClient.connectPubSubAsync(new Utf8StringCodec(), redisURI).toScala.map(_.reactive()), 5.seconds)

  val statusAsyncApi = new RedisAsyncScalaApi(statusAsyncCommands)
  val metatdataApi   = new RedisAsyncScalaApi(metadataAsyncCommands)
  val severityApi    = new RedisAsyncScalaApi(severityAsyncCommands)

  def make(): AlarmServiceImpl = new AlarmServiceImpl(
    metatdataApi,
    severityApi,
    statusAsyncApi,
    () ⇒
      new RedisKeySpaceApi[StatusKey, AlarmStatus](
        () => new RedisReactiveScalaApi[String, String](reactiveCommands),
        statusAsyncApi
    ),
    new ShelveTimeoutActorFactory()
  )
}
