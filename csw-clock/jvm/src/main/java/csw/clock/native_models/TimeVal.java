package csw.clock.native_models;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TimeVal extends Structure {
    public NativeLong seconds;
    public NativeLong microseconds;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("seconds", "microseconds");
    }
}
