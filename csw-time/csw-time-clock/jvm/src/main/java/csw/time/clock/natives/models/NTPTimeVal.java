package csw.time.clock.natives.models;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class NTPTimeVal extends Structure {
    public TimeVal time;          /* Current time (ro) */
    public NativeLong maxerror;   /* Maximum error (us) (ro) */
    public NativeLong esterror;   /* estimated error (us) (ro) */
    public int tai;               /* TAI offset (ro) */

    // Reserved fields used in glibc
    public NativeLong unused1;
    public NativeLong unused2;
    public NativeLong unused3;
    public NativeLong unused4;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("time", "maxerror", "esterror", "tai" , "unused1", "unused2", "unused3", "unused4");
    }
}
