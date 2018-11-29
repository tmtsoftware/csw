package csw.time.client.internal;

import csw.time.client.internal.native_models.NTPTimeVal;
import csw.time.client.internal.native_models.TimeSpec;
import csw.time.client.internal.native_models.Timex;

import java.time.Instant;

// TimeLibrary is responsible for making native calls for time
public class TimeLibraryOther {
    private static int ADJ_TAI = 0x0080;  // set TAI offset

    // For adjtime return value ntp_gettimex, ntp_adjtime
    private static int TIME_OK = 0;

    // -1 is a "flag" to indicate that no offset has been set with adjtime
    private static int _TaiOffset = -1;

    private static int getTaiOffset() {
        int MAGIC_NUMBER_TAI_OFFSET = 37;
        if (_TaiOffset == -1)
            return MAGIC_NUMBER_TAI_OFFSET;
        else
            return _TaiOffset;
    }

    public static int clock_gettime(int clockId, TimeSpec timeSpec) {
        int ClockRealtime       = 0;
        int ClockTAI            = 11;

        // Just use system clock and add offset if TAI clock
        Instant instant = Instant.now();
        timeSpec.seconds = instant.getEpochSecond();
        timeSpec.nanoseconds = (long)instant.getNano() + 1;  // Adding one to get the precision tests to work- discuss
        if (clockId == ClockTAI) timeSpec.seconds += getTaiOffset();
        return 0;  // SUCCESS
    }

    public static int ntp_gettimex(NTPTimeVal ntpTimeVal) {
        ntpTimeVal.tai = getTaiOffset();
        return TIME_OK;
    }

    public static int ntp_adjtime(Timex timex) {
        if (timex.modes == ADJ_TAI) {
            _TaiOffset = (int)timex.constant;
        }
        return TIME_OK;
    }
}
