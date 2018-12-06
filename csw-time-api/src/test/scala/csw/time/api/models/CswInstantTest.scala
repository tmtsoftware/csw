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

  // DEOPSCSW-539: Ability to read local time that is synchronized with PTP time, at remote observing sites
  test("should get hawaii/local date time as well as date time at provided zone id from UtcInstant") {
    val dateTimeStr        = "2018-12-06T14:29:41.204Z"
    val hawaiiDateTimeStr  = "2018-12-06T04:29:41.204-10:00[US/Hawaii]"
    val localDateTimeStr   = "2018-12-06T19:59:41.204+05:30[Asia/Kolkata]"
    val newyorkDateTimeStr = "2018-12-06T09:29:41.204-05:00[America/New_York]"

    // Using TimeService you can get this utcInstant, which is synchronized with PTP
    // and then you can use atHawaii, atLocal and atZone helpers defined on CswInstant
    val utcInstant = UtcInstant(Instant.parse(dateTimeStr))

    utcInstant.atHawaii.toString shouldBe hawaiiDateTimeStr
    utcInstant.atLocal.toString shouldBe localDateTimeStr
    utcInstant.atZone(ZoneId.of("America/New_York")).toString shouldBe newyorkDateTimeStr
  }

  // DEOPSCSW-539: Ability to read local time that is synchronized with PTP time, at remote observing sites
  test("should get hawaii/local date time as well as date time at provided zone id from TaiInstant") {
    val taiDateTimeStr     = "2018-12-06T14:29:41.204Z"
    val hawaiiDateTimeStr  = "2018-12-06T04:29:41.204-10:00[US/Hawaii]"
    val localDateTimeStr   = "2018-12-06T19:59:41.204+05:30[Asia/Kolkata]"
    val newyorkDateTimeStr = "2018-12-06T09:29:41.204-05:00[America/New_York]"

    // Using TimeService you can get this taiInstant, which is synchronized with PTP
    // and then you can use atHawaii, atLocal and atZone helpers defined on CswInstant
    val taiInstant = TaiInstant(Instant.parse(taiDateTimeStr))

    taiInstant.atHawaii.toString shouldBe hawaiiDateTimeStr
    taiInstant.atLocal.toString shouldBe localDateTimeStr
    taiInstant.atZone(ZoneId.of("America/New_York")).toString shouldBe newyorkDateTimeStr
  }
}
