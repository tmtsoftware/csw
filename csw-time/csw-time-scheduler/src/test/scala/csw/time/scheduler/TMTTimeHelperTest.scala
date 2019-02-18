package csw.time.scheduler

import java.time._

import csw.time.core.models.{TAITime, UTCTime}
import org.scalatest.{FunSuite, Matchers}

class TMTTimeHelperTest extends FunSuite with Matchers {
  private val instant = ZonedDateTime.of(2007, 12, 3, 10, 15, 30, 11, ZoneOffset.UTC).toInstant

  /* TMTTime contains ZonedDateTime which has all the date time info along with Zone
   * Below tests shows irrespective of local or remote time,
   * how you can access different parts of the ZonedDateTime structure from java
   */

  // DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  // This test is sufficient to show code works in both Scala and Java
  // since UTCTime.toZonedDateTime is used in both languages.

  test("should access parts of UTC time") {
    val utcTime       = UTCTime(instant)
    val zonedDateTime = TMTTimeHelper.toZonedDateTime(utcTime)

    zonedDateTime.getYear shouldBe 2007
    zonedDateTime.getMonth.getValue shouldBe 12
    zonedDateTime.getDayOfMonth shouldBe 3
    zonedDateTime.getHour shouldBe 10
    zonedDateTime.getMinute shouldBe 15
    zonedDateTime.getSecond shouldBe 30
  }

  // DEOPSCSW-536: Access parts of TAI date.time in Java and Scala
  // This test is sufficient to show code works in both Scala and Java
  // since TaiTime.value.atZone is used in both languages.

  test("should access parts of TAI time") {
    val taiTime       = TAITime(instant)
    val zonedDateTime = TMTTimeHelper.atZone(taiTime, ZoneOffset.UTC)

    zonedDateTime.getYear shouldBe 2007
    zonedDateTime.getMonth.getValue shouldBe 12
    zonedDateTime.getDayOfMonth shouldBe 3
    zonedDateTime.getHour shouldBe 10
    zonedDateTime.getMinute shouldBe 15
    zonedDateTime.getSecond shouldBe 30
  }

  // DEOPSCSW-540: Access parts of Remote time/date in Java and Scala
  // This test is sufficient to show code works in both Scala and Java
  // since UTCTime.atZone is used in both languages.
  test("should access parts of a remote time") {
    val utcTime       = UTCTime(instant)
    val zonedDateTime = TMTTimeHelper.atZone(utcTime, ZoneId.of("Asia/Kolkata"))

    zonedDateTime.getYear shouldBe 2007
    zonedDateTime.getMonth.getValue shouldBe 12
    zonedDateTime.getDayOfMonth shouldBe 3
    zonedDateTime.getHour shouldBe 10 + 5    //with zone offset
    zonedDateTime.getMinute shouldBe 15 + 30 //with zone offset
    zonedDateTime.getSecond shouldBe 30
  }
}
