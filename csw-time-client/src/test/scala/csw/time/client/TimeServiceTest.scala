package csw.time.client

import java.time._

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import csw.time.api.models.CswInstant.TaiInstant
import csw.time.api.scaladsl.TimeService
import csw.time.client.extensions.RichInstant.RichInstant
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await

class TimeServiceTest extends FunSuite with Matchers with BeforeAndAfterAll with Eventually {
  private val TaiOffset = 37 // At the time of writing this, TAI is ahead of UTC by 37 seconds.

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(50.millis, 10.millis)
  private implicit val system: ActorSystem             = ActorSystem("time-service")
  private val timeService: TimeService                 = TimeServiceFactory.make(TaiOffset)

  private val testProperties: TestProperties = TestProperties.instance
  import testProperties._

  override protected def afterAll(): Unit = Await.result(system.terminate(), 5.seconds)

  //------------------------------UTC-------------------------------

  //DEOPSCSW-532: Synchronize activities with other comp. using UTC
  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should get UTC time") {
    val utcInstant            = timeService.utcTime()
    val fixedInstant: Instant = Instant.now()

    val expectedMillis = fixedInstant.toEpochMilli +- 5

    utcInstant.value.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-534: PTP accuracy and precision while reading UTC
  test("should get precision up to nanoseconds in UTC time") {
    // This test is written in eventually block because coincidentally digits at nano place can be "000".
    // To cover that edge-case eventually is used.
    // formatNanos formats instant in 9 digits precision and if clock does not support nano precision (supports us precision)
    // then last 3 digits will be printed as 000 (This test will fail with jdk's Instant.now call which does not provide nano precision)

    eventually(timeService.utcTime().value.formatNanos(precision) should not endWith "000")
  }

  //DEOPSCSW-537: Optimum way for conversion from UTC to TAI
  test("should convert UTC time to TAI time") {
    val utcInstant = timeService.utcTime()

    val taiInstant = timeService.toTai(utcInstant)

    Duration.between(utcInstant.value, taiInstant.value).getSeconds shouldBe TaiOffset
  }

  //------------------------------TAI-------------------------------

  //DEOPSCSW-535: Synchronize activities with other comp, using TAI
  //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI time") {
    val taiInstant          = timeService.taiTime()
    val TAIInstant: Instant = Instant.now().plusSeconds(TaiOffset)

    val expectedMillis = TAIInstant.toEpochMilli +- 5

    taiInstant.value.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-538: PTP accuracy and precision while reading TAI
  test("should get precision up to nanoseconds in TAI time") {
    // This test is written in eventually block because coincidentally digits at nano place can be "000".
    // To cover that edge-case eventually is used.

    eventually(timeService.taiTime().value.formatNanos(precision) should not endWith "000")
  }

  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI offset") {
    val offset = timeService.taiOffset()
    offset shouldEqual TaiOffset
  }

  //DEOPSCSW-537: Optimum way for conversion from UTC to TAI
  test("should convert TAI time to UTC time") {
    val taiInstant = timeService.taiTime()

    val utcInstant = timeService.toUtc(taiInstant)

    Duration.between(utcInstant.value, taiInstant.value).getSeconds shouldBe TaiOffset
  }

  //------------------------------Scheduling-------------------------------

  //DEOPSCSW-542: Schedule a task to execute in future
  test("should schedule a task once at given start time with allowed jitter") {
    val testProbe = TestProbe() //todo: Replace with promise

    val idealScheduleTime: TaiInstant = TaiInstant(timeService.taiTime().value.plusSeconds(1))

    timeService.scheduleOnce(idealScheduleTime) {
      testProbe.ref ! timeService.taiTime()
    }

    val actualScheduleTime: TaiInstant = testProbe.expectMsgType[TaiInstant]

    actualScheduleTime.value.getEpochSecond - idealScheduleTime.value.getEpochSecond shouldBe 0
    actualScheduleTime.value.getNano - idealScheduleTime.value.getNano should be < allowedJitterInNanos
  }
}
