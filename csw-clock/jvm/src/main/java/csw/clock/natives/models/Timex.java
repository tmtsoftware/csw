package csw.clock.natives.models;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class Timex extends Structure {
    public int  modes;      /* Mode selector */
    public NativeLong offset;     /* Time offset; nanoseconds, if STA_NANO
                                   status flag is set, otherwise
                                   microseconds */
    public NativeLong freq;       /* Frequency offset; see NOTES for units */
    public NativeLong maxerror;   /* Maximum error (microseconds) */
    public NativeLong esterror;   /* Estimated error (microseconds) */
    public int  status;     /* Clock command/status */
    public NativeLong constant;   /* PLL (phase-locked loop) time constant */
    public NativeLong precision;  /* Clock precision
                                   (microseconds, read-only) */
    public NativeLong tolerance;  /* Clock frequency tolerance (read-only);
                                   see NOTES for units */
    TimeVal timeval;
    /* Current time (read-only, except for
       ADJ_SETOFFSET); upon return, time.tv_usec
       contains nanoseconds, if STA_NANO status
       flag is set, otherwise microseconds */

    public NativeLong tick;       /* Microseconds between clock ticks */
    public NativeLong ppsfreq;    /* PPS (pulse per second) frequency
                                   (read-only); see NOTES for units */
    public NativeLong jitter;     /* PPS jitter (read-only); nanoseconds, if
                                   STA_NANO status flag is set, otherwise
                                   microseconds */
    public int  shift;      /* PPS interval duration
                                   (seconds, read-only) */
    public NativeLong stabil;     /* PPS stability (read-only);
                                   see NOTES for units */
    public NativeLong jitcnt;     /* PPS count of jitter limit exceeded
                                   events (read-only) */
    public NativeLong calcnt;     /* PPS count of calibration intervals
                                   (read-only) */
    public NativeLong errcnt;     /* PPS count of calibration errors
                                   (read-only) */
    public NativeLong stbcnt;     /* PPS count of stability limit exceeded
                                   events (read-only) */
    public int tai;         /* TAI offset, as set by previous ADJ_TAI
                                   operation (seconds, read-only,
                                   since Linux 2.6.26) */


    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "modes", "offset", "freq", "maxerror", "esterror", "status", "constant",
                "precision", "tolerance", "tick", "ppsfreq", "jitter",
                "shift", "stabil", "jitcnt", "calcnt", "errcnt", "stbcnt", "tai"
        );
    }
}
