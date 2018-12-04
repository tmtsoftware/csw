package csw.time.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import csw.time.api.models.Cancellable;
import csw.time.api.models.CswInstant.TaiInstant;
import csw.time.api.models.CswInstant.UtcInstant;
import csw.time.client.internal.TimeLibraryUtil;
import csw.time.client.internal.TimeServiceImpl;
import csw.time.client.internal.javawrappers.JTimeServiceImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class JTimeServiceTest extends JUnitSuite {

    private static int TaiOffset = 37;
    private static JTimeServiceImpl jTimeService;

    @BeforeClass
    public static void beforeClass() {
        ActorSystem system = ActorSystem.create("time-service");
        TimeServiceImpl timeService = new TimeServiceImpl(system);
        jTimeService = new JTimeServiceImpl(timeService);
        timeService.setTaiOffset(TaiOffset);
    }

    //------------------------------UTC-------------------------------

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Test
    public void shouldGetUTCTime(){
        UtcInstant utcInstant = jTimeService.utcTime();
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
        TaiInstant taiInstant = jTimeService.taiTime();
        Instant expectedTaiInstant = Instant.now().plusSeconds(TaiOffset);

        long expectedMillis = expectedTaiInstant.toEpochMilli();

        assertEquals(expectedMillis, taiInstant.value().toEpochMilli());
    }

    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void shouldGetTAIOffset(){
        int offset = jTimeService.taiOffset();

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

    @Test
    public void shouldScheduleTaskAtStartTime(){
        ActorSystem actorSystem = ActorSystem.create("time-service");
        TestProbe testProbe = new TestProbe(actorSystem);

        TaiInstant idealScheduleTime = new TaiInstant(jTimeService.taiTime().value().plusSeconds(1));

        Cancellable cancellable = jTimeService.scheduleOnce(idealScheduleTime, consumer -> testProbe.ref().tell(jTimeService.taiTime(), ActorRef.noSender()));

        TaiInstant actualScheduleTime = testProbe.expectMsgClass(TaiInstant.class);

        System.out.println("Ideal Schedule Time: "+idealScheduleTime);
        System.out.println("Actual Schedule Time: "+actualScheduleTime);

        int allowedJitterInNanos;
        if (TimeLibraryUtil.osType() == TimeLibraryUtil.Linux$.MODULE$) {
            allowedJitterInNanos = 5 * 1000 * 1000;
        } else {
            allowedJitterInNanos = 7 * 1000 * 1000;
        }
        assertEquals(actualScheduleTime.value().getEpochSecond() - idealScheduleTime.value().getEpochSecond(), 0);
        assertTrue(actualScheduleTime.value().getNano() - idealScheduleTime.value().getNano() < allowedJitterInNanos);
    }
}
