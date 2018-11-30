package csw.time.client.internal.native_models;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class NTPTimeVal extends Structure {
    public TimeVal time;        /* Current time */
    public NativeLong maxerror;       /* Maximum error */
    public NativeLong esterror;
    public int tai;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("time", "maxerror", "esterror", "tai");
    }
}
