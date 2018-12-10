package csw.time.api.models

import java.time._

import csw.time.api.models.TMTTime.{TAITime, UTCTime}
import org.scalatest.{FunSuite, Matchers}

class TMTTimeTest extends FunSuite with Matchers {

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should access parts of UTC time") {
    val zonedDateTime = ZonedDateTime.of(2007, 12, 3, 10, 15, 30, 11, ZoneId.of("US/Hawaii"))
    val utcInstant    = UTCTime(zonedDateTime)

    utcInstant.value.getYear shouldBe 2007
    utcInstant.value.getMonth.getValue shouldBe 12
    utcInstant.value.getDayOfMonth shouldBe 3
    utcInstant.value.getHour shouldBe 0 // since HST is -10:00 from UTC
    utcInstant.value.getMinute shouldBe 15
    utcInstant.value.getSecond shouldBe 30
  }

  //DEOPSCSW-536: Access parts of TAI date.time in Java and Scala
  test("should access parts of TAI time") {
    val zonedDateTime = ZonedDateTime.of(2007, 12, 3, 10, 15, 30, 11, ZoneId.of("US/Hawaii"))
    val taiInstant    = TAITime(zonedDateTime)

    taiInstant.value.getYear shouldBe 2007
    taiInstant.value.getMonth.getValue shouldBe 12
    taiInstant.value.getDayOfMonth shouldBe 3
    taiInstant.value.getHour shouldBe 0 // since HST is -10:00 from UTC
    taiInstant.value.getMinute shouldBe 15
    taiInstant.value.getSecond shouldBe 30
  }
}
