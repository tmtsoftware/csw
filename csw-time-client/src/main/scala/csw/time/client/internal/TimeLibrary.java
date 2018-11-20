package csw.time.client.internal;

import com.sun.jna.Native;
import csw.time.client.internal.native_models.NTPTimeVal;
import csw.time.client.internal.native_models.TimeSpec;

// TimeLibrary is responsible for making native calls for time
public class TimeLibrary {

    static {
        Native.register("c");
    }

    public static native int clock_gettime(int clockId, TimeSpec timeSpec);

    public static native int ntp_gettimex(NTPTimeVal ntpTimeVal);
}
