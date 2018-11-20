package csw.time.client.internal;

import com.sun.jna.Native;
import csw.time.client.internal.native_models.NTPTimeVal;
import csw.time.client.internal.native_models.TimeSpec;

public class TimeLibrary {

    public TimeLibrary() {
        Native.register("c");
    }

    public native int clock_gettime(int clockId, TimeSpec timeSpec);

    public native int ntp_gettimex(NTPTimeVal ntpTimeVal);
}
