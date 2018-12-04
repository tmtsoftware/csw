package csw.time.client

import java.time._

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}
import csw.time.client.extensions.RichInstant.RichInstant
import csw.time.client.internal.{TimeLibraryUtil, TimeServiceImpl}
import csw.time.client.tags.Linux
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await

class TimeServiceTest extends FunSuite with Matchers with BeforeAndAfterAll with Eventually {
  private val TaiOffset = 37 // At the time of writing this, TAI is ahead of UTC by 37 seconds.

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(50.millis, 10.millis)
  implicit val system: ActorSystem                     = ActorSystem("time-service")
  var timeService: TimeServiceImpl                     = _

  override protected def beforeAll(): Unit = {
    timeService = new TimeServiceImpl()
    timeService.setTaiOffset(TaiOffset)
  }

  override protected def afterAll(): Unit = Await.result(system.terminate(), 5.seconds)

  //------------------------------UTC-------------------------------

  //DEOPSCSW-532: Synchronize activities with other comp. using UTC
  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should get UTC time", Linux) {
    val utcInstant            = timeService.utcTime()
    val fixedInstant: Instant = Instant.now()

    val expectedMillis = fixedInstant.toEpochMilli +- 5

    utcInstant.value.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-534: PTP accuracy and precision while reading UTC
  test("should get precision up to nanoseconds in UTC time", Linux) {
    // This test is written in eventually block because coincidentally digits at nano place can be "000".
    // To cover that edge-case eventually is used.
    // formatNanos formats instant in 9 digits precision and if clock does not support nano precision (supports us precision)
    // then last 3 digits will be printed as 000 (This test will fail with jdk's Instant.now call which does not provide nano precision)

    eventually(timeService.utcTime().value.formatNanos should not endWith "000")
  }

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should access parts of UTC time", Linux) {
    val utcInstant = UtcInstant(Instant.parse("2007-12-03T10:15:30.00Z"))

    val hstZone = ZoneId.of("-10:00")

    val hstZDT: ZonedDateTime = utcInstant.value.atZone(hstZone)

    hstZDT.getYear shouldBe 2007
    hstZDT.getMonth.getValue shouldBe 12
    hstZDT.getDayOfMonth shouldBe 3
    hstZDT.getHour shouldBe 0 // since HST is -10:00 from UTC
    hstZDT.getMinute shouldBe 15
    hstZDT.getSecond shouldBe 30
  }

  //------------------------------TAI-------------------------------

  //DEOPSCSW-535: Synchronize activities with other comp, using TAI
  //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI time", Linux) {
    val taiOffset = 37

    val taiInstant          = timeService.taiTime()
    val TAIInstant: Instant = Instant.now().plusSeconds(taiOffset)

    val expectedMillis = TAIInstant.toEpochMilli +- 5

    taiInstant.value.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-538: PTP accuracy and precision while reading TAI
  test("should get precision up to nanoseconds in TAI time", Linux) {
    // This test is written in eventually block because coincidentally digits at nano place can be "000".
    // To cover that edge-case eventually is used.

    eventually(timeService.taiTime().value.formatNanos should not endWith "000")
  }

  //DEOPSCSW-536: Access parts of TAI date.time in Java and Scala
  test("should access parts of TAI time", Linux) {
    val taiInstant = TaiInstant(Instant.parse("2007-12-03T10:15:30.00Z"))

    val hstZone = ZoneId.of("-10:00")

    val hstZDT = taiInstant.value.atZone(hstZone)

    hstZDT.getYear shouldBe 2007
    hstZDT.getMonth.getValue shouldBe 12
    hstZDT.getDayOfMonth shouldBe 3
    hstZDT.getHour shouldBe 0 // since HST is -10:00 from UTC
    hstZDT.getMinute shouldBe 15
    hstZDT.getSecond shouldBe 30
  }

  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI offset", Linux) {
    val offset = timeService.taiOffset()
    offset shouldEqual TaiOffset
  }

  //------------------------------Scheduling-------------------------------

  //DEOPSCSW-542: Schedule a task to execute in future
  test("should schedule a task once at given start time with allowed jitter of 5ms", Linux) {
    val testProbe = TestProbe()

    val idealScheduleTime: TaiInstant = TaiInstant(timeService.taiTime().value.plusSeconds(1))

    timeService.scheduleOnce(idealScheduleTime) {
      testProbe.ref ! timeService.taiTime()
    }

    val actualScheduleTime: TaiInstant = testProbe.expectMsgType[TaiInstant]

    println(s"Ideal Schedule Time: $idealScheduleTime")
    println(s"Actual Schedule Time: $actualScheduleTime")

    val allowedJitterInNanos = TimeLibraryUtil.osType match {
      case TimeLibraryUtil.Linux => 5 * 1000 * 1000 // should be ideally 400µs as per 3σ statistics from manual tests
      case _                     => 7 * 1000 * 1000
    }

    actualScheduleTime.value.getEpochSecond - idealScheduleTime.value.getEpochSecond shouldBe 0
    actualScheduleTime.value.getNano - idealScheduleTime.value.getNano should be < allowedJitterInNanos
  }
}
