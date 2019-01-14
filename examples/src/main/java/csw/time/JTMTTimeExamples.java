package csw.time;

import csw.time.api.models.TAITime;
import csw.time.api.models.UTCTime;
import csw.time.client.TMTTimeHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;

class JTMTTimeExamples {
  //#utc-time
  // get current UTC time
  UTCTime utcTime = UTCTime.now();
  //#utc-time

  //#tai-time
  // get current TAI time
  TAITime taiTime = TAITime.now();
  //#tai-time

    //#utc-to-tai
    TAITime taiTime1 = utcTime.toTAI();
    //#utc-to-tai

    //#tai-to-utc
    UTCTime utcTime1 = taiTime.toUTC();
    //#tai-to-utc

    //#at-local
    // Get UTCTime at local timezone
    ZonedDateTime utcLocalTime = TMTTimeHelper.atLocal(utcTime);

    // Get TAITime at local timezone
    ZonedDateTime taiLocalTime = TMTTimeHelper.atLocal(taiTime);
    //#at-local

    //#at-hawaii
    // Get UTCTime at Hawaii (HST) timezone
    ZonedDateTime utcHawaiiTime = TMTTimeHelper.atHawaii(utcTime);

    // Get TAITime at Hawaii (HST) timezone
    ZonedDateTime taiHawaiiTime = TMTTimeHelper.atHawaii(taiTime);
    //#at-hawaii

    //#at-zone
    // Get UTCTime at specified timezone
    ZonedDateTime utcKolkataTime = TMTTimeHelper.atZone(utcTime, ZoneId.of("Asia/Kolkata"));

    // Get TAITime at specified timezone
    ZonedDateTime taiKolkataTime = TMTTimeHelper.atZone(taiTime, ZoneId.of("Asia/Kolkata"));
    //#at-zone

}
