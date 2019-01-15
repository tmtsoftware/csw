package csw.time.clock.natives.models;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TimeVal extends Structure {
    public NativeLong seconds; /* seconds since the Epoch */
    public NativeLong microseconds; /* microseconds */

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("seconds", "microseconds");
    }
}
