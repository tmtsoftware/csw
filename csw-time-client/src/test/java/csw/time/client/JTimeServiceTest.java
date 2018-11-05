package csw.time.client;

import csw.time.api.TimeService;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

public class JTimeServiceTest {

    private Instant fixedInstant = Instant.now();
    private ZoneId zoneId        = ZoneId.of("US/Hawaii");
    private Clock clock          = Clock.fixed(fixedInstant, zoneId);

    private TimeService timeService = new TimeServiceImpl(clock);

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Ignore
    public void shouldGetUTCTime(){
        Instant instant               = timeService.UTCTime();
        Instant fixedInstant = Instant.now();

        long expectedMillis = fixedInstant.toEpochMilli() +- 10;

        assertEquals(expectedMillis, instant.toEpochMilli());
    }

    //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
    @Ignore
    public void shouldGetTAITime(){
        int taiOffset = 37;
        Instant instant               = timeService.TAITime();
        Instant fixedInstant = Instant.now().plusSeconds(taiOffset);

        long expectedMillis = fixedInstant.toEpochMilli() +- 10;

        assertEquals(expectedMillis, instant.toEpochMilli());
    }
}
