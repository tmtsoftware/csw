package csw.time.client.internal.native_models;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TimeVal extends Structure {
    public Long seconds;
    public Long microseconds;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("seconds", "microseconds");
    }
}
