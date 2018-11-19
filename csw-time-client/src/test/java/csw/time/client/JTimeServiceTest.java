package csw.time.client;

import csw.time.api.CswInstant;
import csw.time.api.TimeScales;
import csw.time.api.TimeService;
import csw.time.client.javadsl.tags.LinuxTag;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

@LinuxTag
public class JTimeServiceTest {

    private Instant fixedInstant = Instant.now();
    private ZoneId zoneId        = ZoneId.of("US/Hawaii");
    private Clock clock          = Clock.fixed(fixedInstant, zoneId);

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Test
    public void shouldGetUTCTime(){
        TimeService timeService = new TimeServiceImpl(clock);

        CswInstant cswInstant = timeService.UTCTime();
        Instant fixedInstant = Instant.now();

        long expectedMillis = fixedInstant.toEpochMilli();

        assertEquals(expectedMillis, cswInstant.instant().toEpochMilli());
        assertEquals(TimeScales.jUTCScale(), cswInstant.timeScale());
    }

    //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
    @Test
    public void shouldGetTAITime(){
        TimeService timeService = new TimeServiceImpl(clock);

        int taiOffset = 37;
        CswInstant cswInstant               = timeService.TAITime();
        Instant fixedInstant = Instant.now().plusSeconds(taiOffset);

        long expectedMillis = fixedInstant.toEpochMilli();

        assertEquals(expectedMillis, cswInstant.instant().toEpochMilli());
        assertEquals(TimeScales.jTAIScale(), cswInstant.timeScale());
    }

    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void shouldGetTAIOffset(){
        TimeService timeService = new TimeServiceImpl(clock);

        int expectedOffset = 37;

        int offset = timeService.TAIOffset();

        assertEquals(expectedOffset, offset);
    }
}
