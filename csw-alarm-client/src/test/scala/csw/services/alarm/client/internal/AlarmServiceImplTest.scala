package csw.services.alarm.client.internal
import java.io.File

import akka.actor.ActorSystem
import csw.commons.redis.EmbeddedRedis
import csw.services.alarm.api.internal._
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmHealth.Bad
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AlarmMetadata, AlarmSeverity, AlarmStatus, AlarmType}
import csw.services.alarm.client.internal.AlarmCodec.{MetadataCodec, SeverityCodec, StatusCodec}
import csw.services.alarm.client.internal.redis.scala_wrapper.{RedisAsyncScalaApi, RedisReactiveScalaApi}
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.async.Async.{async, await}
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

class AlarmServiceImplTest extends FunSuite with Matchers with EmbeddedRedis with BeforeAndAfterAll {
  private val alarmServer        = "AlarmServer"
  private val (sentinel, server) = startSentinel(26379, 6379, masterId = alarmServer)

  private val redisURI                 = RedisURI.Builder.sentinel("localhost", 26379, alarmServer).build()
  private val redisClient: RedisClient = RedisClient.create(redisURI)

  override protected def afterAll(): Unit = stopSentinel(sentinel, server)

  implicit val system: ActorSystem          = ActorSystem()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val alarmService: AlarmServiceImpl = Await.result(new AlarmServiceFactory(redisURI, redisClient).make(), 5.seconds)

  test("init alarms") {

    val path = getClass.getResource("/test-alarms/valid-alarms.conf").getPath

    val alarmKey = AlarmKey("nfiraos", "cc.trombone", "tromboneAxisHighLimitAlarm")
    val file     = new File(path)
    Await.result(alarmService.initAlarms(file), 5.seconds)

    Await.result(alarmService.getMetadata(alarmKey), 5.seconds) shouldBe AlarmMetadata(
      subsystem = "nfiraos",
      component = "cc.trombone",
      name = "tromboneAxisHighLimitAlarm",
      description = "Warns when trombone axis has reached the high limit",
      location = "south side",
      AlarmType.Absolute,
      Set(Indeterminate, Okay, Warning, Major, Critical),
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

}

// Fixme: provide factory in main scope
class AlarmServiceFactory(redisURI: RedisURI, redisClient: RedisClient)(implicit system: ActorSystem, ec: ExecutionContext)
    extends AlarmRW {

  // Fixme
  object AggregateCodec0 extends AlarmCodec[AggregateKey, String]

  val metadataAsyncCommandsF: Future[RedisAsyncCommands[MetadataKey, AlarmMetadata]] =
    redisClient.connectAsync(MetadataCodec, redisURI).toScala.map(_.async())
  val statusAsyncCommandsF: Future[RedisAsyncCommands[StatusKey, AlarmStatus]] =
    redisClient.connectAsync(StatusCodec, redisURI).toScala.map(_.async())
  val severityAsyncCommandsF: Future[RedisAsyncCommands[SeverityKey, AlarmSeverity]] =
    redisClient.connectAsync(SeverityCodec, redisURI).toScala.map(_.async())
  val aggregateReactiveCommandsF: Future[RedisPubSubReactiveCommands[AggregateKey, String]] =
    redisClient.connectPubSubAsync(AggregateCodec0, redisURI).toScala.map(_.reactive())

  def make(): Future[AlarmServiceImpl] = async {

    val value = await(aggregateReactiveCommandsF)

    new AlarmServiceImpl(
      new RedisAsyncScalaApi(await(metadataAsyncCommandsF)),
      new RedisAsyncScalaApi(await(severityAsyncCommandsF)),
      new RedisAsyncScalaApi(await(statusAsyncCommandsF)),
      () ⇒ new RedisReactiveScalaApi(value),
      new ShelveTimeoutActorFactory()
    )

  }

}
