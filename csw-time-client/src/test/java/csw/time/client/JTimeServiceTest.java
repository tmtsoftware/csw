package csw.time.client;

import csw.time.api.models.CswInstant.TaiInstant;
import csw.time.api.models.CswInstant.UtcInstant;
import csw.time.api.scaladsl.TimeService;
import csw.time.client.internal.TimeServiceImpl;
import csw.time.client.javadsl.tags.LinuxTag;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;

@LinuxTag
public class JTimeServiceTest {

    //------------------------------UTC-------------------------------

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Test
    public void shouldGetUTCTime(){
        TimeService timeService = new TimeServiceImpl();

        UtcInstant utcInstant = timeService.utcTime();
        Instant fixedInstant = Instant.now();

        long expectedMillis = fixedInstant.toEpochMilli();

        assertEquals(expectedMillis, utcInstant.value().toEpochMilli());
    }

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Test
    public void shouldAccessPartsOfUtcInstant(){
        UtcInstant utcInstant = new UtcInstant(Instant.parse("2007-12-03T10:15:30.00Z"));

        ZoneId hstZone = ZoneId.of("-10:00");

        ZonedDateTime hstZDT = utcInstant.value().atZone(hstZone);

        assertEquals(2007, hstZDT.getYear());
        assertEquals(12, hstZDT.getMonth().getValue());
        assertEquals(3, hstZDT.getDayOfMonth());
        assertEquals(0, hstZDT.getHour()); // since HST is -10:00 from UTC
        assertEquals(15, hstZDT.getMinute());
        assertEquals(30, hstZDT.getSecond());
    }

    //------------------------------TAI-------------------------------

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

    //DEOPSCSW-536: Access parts of TAI date.time in Java and Scala
    @Test
    public void shouldAccessPartsOfTaiInstant(){
        TaiInstant taiInstant = new TaiInstant(Instant.parse("2007-12-03T10:15:30.00Z"));

        ZoneId hstZone = ZoneId.of("-10:00");

        ZonedDateTime hstZDT = taiInstant.value().atZone(hstZone);

        assertEquals(2007, hstZDT.getYear());
        assertEquals(12, hstZDT.getMonth().getValue());
        assertEquals(3, hstZDT.getDayOfMonth());
        assertEquals(0, hstZDT.getHour()); // since HST is -10:00 from UTC
        assertEquals(15, hstZDT.getMinute());
        assertEquals(30, hstZDT.getSecond());
    }
}
