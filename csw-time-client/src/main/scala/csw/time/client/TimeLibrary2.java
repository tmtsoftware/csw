package csw.time.client;

import com.sun.jna.Native;

public class TimeLibrary2 {
    static {
        Native.register("c");
    }

    public static native int clock_gettime(int clock, TimeSpec timeSpec);
}
