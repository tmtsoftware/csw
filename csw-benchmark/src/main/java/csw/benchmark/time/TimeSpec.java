package csw.benchmark.time;

//public class TimeSpec {
//    public long seconds;
//    public long nanoseconds;
//
//    }

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TimeSpec extends Structure {
    public long seconds;
    public long nanoseconds;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("seconds", "nanoseconds");
    }
}
