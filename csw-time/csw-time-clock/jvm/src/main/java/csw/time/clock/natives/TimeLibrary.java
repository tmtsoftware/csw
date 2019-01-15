package csw.time.clock.natives;

import com.sun.jna.Native;
import csw.time.clock.natives.models.NTPTimeVal;
import csw.time.clock.natives.models.TimeSpec;
import csw.time.clock.natives.models.Timex;

// TimeLibrary is responsible for making native calls for time
public class TimeLibrary {

    // Load C library
    static {
        Native.register("c");
    }

    /**
     * Retrieves the time of the specified clock clk_id.
     * @param clockId the identifier of the particular clock on which to act
     * @param timeSpec a [[TimeSpec]] structure in which seconds and nanoseconds fields are filled in.
     * @return 0 for success, or -1 for failure (in which case errno is set appropriately)
     */
    public static native int clock_gettime(int clockId, TimeSpec timeSpec);

    /**
     *
     * @param ntpTimeVal an [[NTPTimeVal]] structure in which the time, max‐error, and esterror fields are filled in
     * along with information in the tai field.
     * @return the current state of the clock on success, or the errors
     */
    public static native int ntp_gettimex(NTPTimeVal ntpTimeVal);


    /**
     * Adjusts the system clock to an externally derived time
     * @param timex [[Timex]] structure to be filled in with the current values of the corresponding variables
     * @return the clock state. On failure, return -1 and sets errno
     */
    public static native int ntp_adjtime(Timex timex);
}
