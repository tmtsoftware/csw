package csw.time.client;

import akka.actor.ActorSystem;
import csw.time.api.models.CswInstant.TaiInstant;
import csw.time.api.models.CswInstant.UtcInstant;
import csw.time.client.internal.TimeServiceImpl;
import csw.time.client.javadsl.tags.LinuxTag;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;

@LinuxTag
public class JTimeServiceTest {

    private static int TaiOffset = 37;
    private static TimeServiceImpl timeService = null;

    @BeforeClass
    public static void beforeClass() {
        ActorSystem system = ActorSystem.create("time-service");
        timeService = new TimeServiceImpl(system);
        timeService.setTaiOffset(TaiOffset);
    }


    //------------------------------UTC-------------------------------

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Test
    public void shouldGetUTCTime(){
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
        TaiInstant taiInstant = timeService.taiTime();
        Instant TaiInstant = Instant.now().plusSeconds(TaiOffset);

        long expectedMillis = TaiInstant.toEpochMilli();

        assertEquals(expectedMillis, taiInstant.value().toEpochMilli());
    }

    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void shouldGetTAIOffset(){
        int offset = timeService.taiOffset();

        assertEquals(TaiOffset, offset);
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
