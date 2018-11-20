package csw.time.client.internal.native_models;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TimeSpec extends Structure {
    public Long seconds;
    public Long nanoseconds;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("seconds", "nanoseconds");
    }
}
