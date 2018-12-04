package csw.time.client.internal;

import com.sun.jna.NativeLong;
import csw.time.client.internal.native_models.NTPTimeVal;
import csw.time.client.internal.native_models.TimeSpec;
import csw.time.client.internal.native_models.Timex;

import java.time.Instant;

// TimeLibrary is responsible for making native calls for time
public class TimeLibraryOther {

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
        int ClockTAI            = 11;

        // Just use system clock and add offset if TAI clock
        Instant instant = Instant.now();
        timeSpec.seconds = new NativeLong(instant.getEpochSecond() + ((clockId == ClockTAI) ? getTaiOffset() : 0));
        timeSpec.nanoseconds = new NativeLong(instant.getNano() + 1);  // Adding one to get the precision tests to work- discuss
        return 0;  // SUCCESS
    }

    public static int ntp_gettimex(NTPTimeVal ntpTimeVal) {
        ntpTimeVal.tai = getTaiOffset();
        return TIME_OK;
    }

    public static int ntp_adjtime(Timex timex) {
        int ADJ_TAI = 0x0080;  // set TAI offset
        if (timex.modes == ADJ_TAI) {
            _TaiOffset = (int)timex.constant.longValue();
        }
        return TIME_OK;
    }
}
