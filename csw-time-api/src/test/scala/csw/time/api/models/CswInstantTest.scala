package csw.time.api.models

import java.time.{Instant, ZoneId, ZonedDateTime}

import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}
import org.scalatest.{FunSuite, Matchers}

class CswInstantTest extends FunSuite with Matchers {

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should access parts of UTC time") {
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

  //DEOPSCSW-536: Access parts of TAI date.time in Java and Scala
  test("should access parts of TAI time") {
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

}
