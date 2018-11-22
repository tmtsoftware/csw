package csw.time.client;

import csw.time.api.models.CswInstant.TaiInstant;
import csw.time.api.models.CswInstant.UtcInstant;
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

        UtcInstant utcInstant = timeService.utcTime();
        Instant fixedInstant = Instant.now();

        long expectedMillis = fixedInstant.toEpochMilli();

        assertEquals(expectedMillis, utcInstant.value().toEpochMilli());
    }

    //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void shouldGetTAITime(){
        TimeService timeService = new TimeServiceImpl();

        int taiOffset = 37;
        TaiInstant taiInstant = timeService.taiTime();
        Instant TaiInstant = Instant.now().plusSeconds(taiOffset);

        long expectedMillis = TaiInstant.toEpochMilli();

        assertEquals(expectedMillis, taiInstant.value().toEpochMilli());
    }

    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void shouldGetTAIOffset(){
        TimeService timeService = new TimeServiceImpl();

        int expectedOffset = 37;

        int offset = timeService.taiOffset();

        assertEquals(expectedOffset, offset);
    }
}
