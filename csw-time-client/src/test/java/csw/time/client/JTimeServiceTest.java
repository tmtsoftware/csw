package csw.time.client;

import csw.time.api.models.CswInstant;
import csw.time.api.models.TimeScales;
import csw.time.api.scaladsl.TimeService;
import csw.time.client.internal.TimeServiceImpl;
import csw.time.client.javadsl.tags.LinuxTag;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

@LinuxTag
public class JTimeServiceTest {
    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Test
    public void shouldGetUTCTime(){
        TimeService timeService = new TimeServiceImpl();

        CswInstant cswInstant = timeService.UTCTime();
        Instant fixedInstant = Instant.now();

        long expectedMillis = fixedInstant.toEpochMilli();

        assertEquals(expectedMillis, cswInstant.instant().toEpochMilli());
        assertEquals(TimeScales.jUTCScale(), cswInstant.timeScale());
    }

    //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void shouldGetTAITime(){
        TimeService timeService = new TimeServiceImpl();

        int taiOffset = 37;
        CswInstant cswInstant               = timeService.TAITime();
        Instant TAIInstant = Instant.now().plusSeconds(taiOffset);

        long expectedMillis = TAIInstant.toEpochMilli();

        assertEquals(expectedMillis, cswInstant.instant().toEpochMilli());
        assertEquals(TimeScales.jTAIScale(), cswInstant.timeScale());
    }

    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void shouldGetTAIOffset(){
        TimeService timeService = new TimeServiceImpl();

        int expectedOffset = 37;

        int offset = timeService.TAIOffset();

        assertEquals(expectedOffset, offset);
    }
}
