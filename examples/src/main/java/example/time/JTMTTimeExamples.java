package example.time;

import csw.time.core.TMTTimeHelper;
import csw.time.core.models.TAITime;
import csw.time.core.models.UTCTime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

class JTMTTimeExamples {
    //#current-time
    // get current UTC time
    private UTCTime utcTime = UTCTime.now();

    // get current TAI time
    private TAITime taiTime = TAITime.now();
    //#current-time

    // #creating-time-instances
    //creating a UTCTime of an hour ago
    private UTCTime utcTimeOfHourAgo = new UTCTime(Instant.now().minusSeconds(3600));

    //creating a TAITime of an hour ago
    private TAITime taiTimeOfHourAgo = new TAITime(Instant.now().minusSeconds(3600));
    // #creating-time-instances

    void usage() {
        System.out.println(utcTimeOfHourAgo);
        System.out.println(taiTimeOfHourAgo);
    }

    void conversion() {
        //#conversion
        // UTC to TAI
        TAITime taiTime = utcTime.toTAI();

        // TAI to UTC
        UTCTime utcTime = taiTime.toUTC();
        //#conversion
    }

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
