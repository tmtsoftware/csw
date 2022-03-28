/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.time

import java.time.{Instant, ZoneId, ZonedDateTime}

import csw.time.core.TMTTimeHelper
import csw.time.core.models.{TAITime, UTCTime}

object TMTTimeExamples {
  // #current-time
  // get current UTC time
  val utcTime: UTCTime = UTCTime.now()

  // get current TAI time
  val taiTime: TAITime = TAITime.now()
  // #current-time

  // #creating-time-instances
  // creating a UTCTime of an hour ago
  val utcTimeOfHourAgo: UTCTime = UTCTime(Instant.now().minusSeconds(3600))

  // creating a TAITime of an hour ago
  val taiTimeOfHourAgo: TAITime = TAITime(Instant.now().minusSeconds(3600))
  // #creating-time-instances

  def conversion(): Unit = {
    // #conversion
    // UTC to TAI
    val taiTime: TAITime = utcTime.toTAI

    // TAI to UTC
    val utcTime0: UTCTime = taiTime.toUTC
    // #conversion
  }

  // #at-local
  // Get UTCTime at local timezone
  val utcLocalTime: ZonedDateTime = TMTTimeHelper.atLocal(utcTime)

  // Get TAITime at local timezone
  val taiLocalTime: ZonedDateTime = TMTTimeHelper.atLocal(taiTime)
  // #at-local

  // #at-hawaii
  // Get UTCTime at Hawaii (HST) timezone
  val utcHawaiiTime: ZonedDateTime = TMTTimeHelper.atHawaii(utcTime)

  // Get TAITime at Hawaii (HST) timezone
  val taiHawaiiTime: ZonedDateTime = TMTTimeHelper.atHawaii(taiTime)
  // #at-hawaii

  // #at-zone
  // Get UTCTime at specified timezone
  val utcKolkataTime: ZonedDateTime = TMTTimeHelper.atZone(utcTime, ZoneId.of("Asia/Kolkata"))

  // Get TAITime at specified timezone
  val taiKolkataTime: ZonedDateTime = TMTTimeHelper.atZone(taiTime, ZoneId.of("Asia/Kolkata"))
  // #at-zone

}
