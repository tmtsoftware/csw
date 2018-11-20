package csw.time.client.internal.native_models;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class NTPTimeVal extends Structure {
    public TimeVal time;        /* Current time */
    public Long maxerror;       /* Maximum error */
    public Long esterror;
    public int tai;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("time", "maxerror", "esterror", "tai");
    }
}
