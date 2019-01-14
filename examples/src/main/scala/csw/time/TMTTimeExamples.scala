package csw.time

import java.time.{ZoneId, ZonedDateTime}

import csw.time.api.models.{TAITime, UTCTime}
import csw.time.client.TMTTimeHelper

object TMTTimeExamples {
  //#utc-time
  // get current UTC time
  val utcTime: UTCTime = UTCTime.now()
  //#utc-time

  //#tai-time
  // get current TAI time
  val taiTime: TAITime = TAITime.now()
  //#tai-time

  //#utc-to-tai
  val taiTime1: TAITime = utcTime.toTAI
  //#utc-to-tai

  //#tai-to-utc
  val utcTime1: UTCTime = taiTime.toUTC
  //#tai-to-utc

  //#at-local
  // Get UTCTime at local timezone
  val utcLocalTime: ZonedDateTime = TMTTimeHelper.atLocal(utcTime)

  // Get TAITime at local timezone
  val taiLocalTime: ZonedDateTime = TMTTimeHelper.atLocal(taiTime)
  //#at-local

  //#at-hawaii
  // Get UTCTime at Hawaii (HST) timezone
  val utcHawaiiTime: ZonedDateTime = TMTTimeHelper.atHawaii(utcTime)

  // Get TAITime at Hawaii (HST) timezone
  val taiHawaiiTime: ZonedDateTime = TMTTimeHelper.atHawaii(taiTime)
  //#at-hawaii

  //#at-zone
  // Get UTCTime at specified timezone
  val utcKolkataTime: ZonedDateTime = TMTTimeHelper.atZone(utcTime, ZoneId.of("Asia/Kolkata"))

  // Get TAITime at specified timezone
  val taiKolkataTime: ZonedDateTime = TMTTimeHelper.atZone(taiTime, ZoneId.of("Asia/Kolkata"))
  //#at-zone

}
