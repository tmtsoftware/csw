package csw.time.api

import java.time._

import org.scalatest.{FunSuite, Matchers}

class TMTTimeTest extends FunSuite with Matchers {

  /* TMTTime contains ZonedDateTime which has all the date time info along with Zone
   * Below tests shows irrespective of local or remote time,
   * how you can access different parts of the ZonedDateTime structure from java
   */

  // DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  // DEOPSCSW-540: Access parts of Remote time/date in Java and Scala
  test("should access parts of UTC time") {
    val instant = ZonedDateTime.of(2007, 12, 3, 10, 15, 30, 11, ZoneOffset.UTC).toInstant

    val zonedDateTime = UTCTime(instant).at(ZoneOffset.UTC)

    zonedDateTime.getYear shouldBe 2007
    zonedDateTime.getMonth.getValue shouldBe 12
    zonedDateTime.getDayOfMonth shouldBe 3
    zonedDateTime.getHour shouldBe 10
    zonedDateTime.getMinute shouldBe 15
    zonedDateTime.getSecond shouldBe 30
  }

  // DEOPSCSW-536: Access parts of TAI date.time in Java and Scala
  // DEOPSCSW-540: Access parts of Remote time/date in Java and Scala
  test("should access parts of TAI time") {
    val instant       = ZonedDateTime.of(2007, 12, 3, 10, 15, 30, 11, ZoneOffset.UTC).toInstant
    val taiTime       = TAITime(instant)
    val zonedDateTime = taiTime.value.atZone(ZoneOffset.UTC)

    zonedDateTime.getYear shouldBe 2007
    zonedDateTime.getMonth.getValue shouldBe 12
    zonedDateTime.getDayOfMonth shouldBe 3
    zonedDateTime.getHour shouldBe 10
    zonedDateTime.getMinute shouldBe 15
    zonedDateTime.getSecond shouldBe 30
  }

}
