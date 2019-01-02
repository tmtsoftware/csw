package csw.clock.native_models;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class NTPTimeVal extends Structure {
    public TimeVal time;          /* Current time */
    public NativeLong maxerror;   /* Maximum error */
    public NativeLong esterror;
    public int tai;

    public NativeLong unused1;
    public NativeLong unused2;
    public NativeLong unused3;
    public NativeLong unused4;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("time", "maxerror", "esterror", "tai" , "unused1", "unused2", "unused3", "unused4");
    }
}
