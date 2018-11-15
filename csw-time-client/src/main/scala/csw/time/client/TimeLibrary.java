package csw.time.client;

import com.sun.jna.Native;

public class TimeLibrary {

    public TimeLibrary() {
        Native.register("c");
    }

    public native int clock_gettime(int clock, TimeSpec timeSpec);

    public native int ntp_gettimex(NTPTimeVal ntpTimeVal);
}
