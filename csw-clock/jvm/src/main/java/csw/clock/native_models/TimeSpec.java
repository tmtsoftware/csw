package csw.clock.native_models;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TimeSpec extends Structure {
    public NativeLong seconds;
    public NativeLong nanoseconds;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("seconds", "nanoseconds");
    }
}
