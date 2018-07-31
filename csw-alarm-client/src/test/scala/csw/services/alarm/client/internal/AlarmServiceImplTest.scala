package csw.services.alarm.client.internal
import java.io.File

import akka.actor.ActorSystem
import csw.commons.redis.EmbeddedRedis
import csw.services.alarm.api.exceptions.{InvalidSeverityException, KeyNotFoundException}
import csw.services.alarm.api.models.AcknowledgementStatus.Acknowledged
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmHealth.Bad
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.UnShelved
import csw.services.alarm.api.models.{AlarmMetadata, AlarmSeverity, AlarmStatus, AlarmType}
import io.lettuce.core.{RedisClient, RedisURI}
import org.scalatest._

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class AlarmServiceImplTest extends FunSuite with Matchers with EmbeddedRedis with BeforeAndAfterAll with BeforeAndAfterEach {
  private val alarmServer        = "AlarmServer"
  private val (sentinel, server) = startSentinel(26379, 6379, masterId = alarmServer)

  private val redisURI                 = RedisURI.Builder.sentinel("localhost", 26379, alarmServer).build()
  private val redisClient: RedisClient = RedisClient.create(redisURI)

  override protected def afterAll(): Unit = stopSentinel(sentinel, server)

  implicit val system: ActorSystem  = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher

  private val alarmServiceFactory    = new AlarmServiceTestFactory(redisURI, redisClient)
  val alarmService: AlarmServiceImpl = alarmServiceFactory.make()

  override protected def beforeEach(): Unit = {
    val path = getClass.getResource("/test-alarms/valid-alarms.conf").getPath
    val file = new File(path)
    await(alarmService.initAlarms(file))
  }

  // DEOPSCSW-486: Provide API to load alarm metadata in Alarm store from file
  test("should load alarms from provided config file") {
    val alarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    await(alarmService.getMetadata(alarmKey)) shouldBe AlarmMetadata(
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

    await(alarmService.getStatus(alarmKey)) shouldBe AlarmStatus()
    await(alarmService.getSeverity(alarmKey)) shouldBe Disconnected
    await(alarmService.getAggregatedHealth(alarmKey)) shouldBe Bad
  }

  // DEOPSCSW-486: Provide API to load alarm metadata in Alarm store from file
  test("should reset the previous Alarm data in redis and set newly provided") {

    val threeAlarmConfigPath = getClass.getResource("/test-alarms/valid-alarms.conf").getPath
    val twoAlarmConfigPath   = getClass.getResource("/test-alarms/two-valid-alarms.conf").getPath

    val nfiraosAlarmKey = AlarmKey("nfiraos", "cc.trombone", "tromboneAxisHighLimitAlarm")
    val tcpAlarmKey     = AlarmKey("tcp", "tcsPk", "cpuExceededAlarm")
    val firstFile       = new File(threeAlarmConfigPath)
    val secondFile      = new File(twoAlarmConfigPath)
    Await.result(alarmService.initAlarms(firstFile), 5.seconds)

    Await.result(alarmService.initAlarms(secondFile, reset = true), 5.seconds)

    intercept[KeyNotFoundException] {
      Await.result(alarmService.getMetadata(tcpAlarmKey), 3.seconds)
    }

    Await.result(alarmService.getMetadata(nfiraosAlarmKey), 5.seconds) shouldBe AlarmMetadata(
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
  }

  test("test set severity") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    //set severity to Major
    val status = setSeverity(tromboneAxisHighLimitAlarm, Major)
    status shouldEqual AlarmStatus(Acknowledged, Latched, Major, UnShelved)

    //get severity and assert
    val alarmSeverity = await(alarmServiceFactory.severityApi.get(tromboneAxisHighLimitAlarm)).get
    alarmSeverity shouldEqual Major

    //wait for 1 second and assert expiry of severity
    Thread.sleep(1000)
    val severityAfter1Second = await(alarmService.getSeverity(tromboneAxisHighLimitAlarm))
    severityAfter1Second shouldEqual Disconnected
  }

  test("should throw InvalidSeverityException when unsupported severity is provided") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    intercept[InvalidSeverityException] {
      Await.result(alarmService.setSeverity(tromboneAxisHighLimitAlarm, AlarmSeverity.Critical), 5.seconds)
    }
  }

  test("should not latch the alarm when it's latchable but not high risk") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    //set severity to Okay
    val status = setSeverity(tromboneAxisHighLimitAlarm, Okay)
    status shouldEqual AlarmStatus(latchStatus = UnLatched, latchedSeverity = Okay)

    //set severity to indeterminant
    val status1 = setSeverity(tromboneAxisHighLimitAlarm, Indeterminate)
    status1 shouldEqual AlarmStatus(latchStatus = UnLatched, latchedSeverity = Indeterminate)
  }

  test("should latch alarm only when it is high risk and higher than latched severity in case of latchable alarms") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    val status                     = setSeverity(tromboneAxisHighLimitAlarm, Major)
    status shouldEqual AlarmStatus(latchStatus = Latched, latchedSeverity = Major)

    val status1 = setSeverity(tromboneAxisHighLimitAlarm, Warning)
    status1 shouldEqual AlarmStatus(latchStatus = Latched, latchedSeverity = Major)

    val status2 = setSeverity(tromboneAxisHighLimitAlarm, Okay)
    status2 shouldEqual AlarmStatus(latchStatus = Latched, latchedSeverity = Major)
  }

  test("should not latch alarm if it is not latchable") {
    val cpuExceededAlarm = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")
    val status           = setSeverity(cpuExceededAlarm, Critical)
    status shouldEqual AlarmStatus(latchStatus = UnLatched, latchedSeverity = Critical)

    val status1 = setSeverity(cpuExceededAlarm, Indeterminate)
    status1 shouldEqual AlarmStatus(latchStatus = UnLatched, latchedSeverity = Indeterminate)
  }

  private def setSeverity(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    await(alarmService.setSeverity(alarmKey, alarmSeverity))
    await(alarmServiceFactory.statusApi.get(alarmKey)).get
  }

  private def await[T](awaitable: Awaitable[T], atMost: FiniteDuration = 5.seconds): T = {
    Await.result(awaitable, atMost)
  }
}
