package csw.time.client;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NTPTimeVal extends Structure {
    public int tai;

    @Override
    protected List<String> getFieldOrder() {
        return Collections.singletonList("tai");
    }
}
