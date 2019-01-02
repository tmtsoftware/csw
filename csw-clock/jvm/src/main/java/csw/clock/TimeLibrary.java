package csw.clock;

import com.sun.jna.Native;
import csw.clock.native_models.NTPTimeVal;
import csw.clock.native_models.TimeSpec;
import csw.clock.native_models.Timex;

// TimeLibrary is responsible for making native calls for time
public class TimeLibrary {

    static {
        Native.register("c");
    }

    public static native int clock_gettime(int clockId, TimeSpec timeSpec);

    public static native int ntp_gettimex(NTPTimeVal ntpTimeVal);

    public static native int ntp_adjtime(Timex timex);
}
