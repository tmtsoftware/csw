package csw.time.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.testkit.typed.javadsl.ManualTime;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.internal.adapter.ActorSystemAdapter;
import akka.testkit.TestProbe;
import csw.time.api.models.Cancellable;
import csw.time.api.models.CswInstant.TaiInstant;
import csw.time.api.models.CswInstant.UtcInstant;
import csw.time.api.scaladsl.TimeService;
import csw.time.client.extensions.RichInstant;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TimeServiceTest {
    @Rule
    public TestKitJunitResource testKit = new TestKitJunitResource(ManualTime.config());

    private static int TaiOffset = 37;
    private TestProperties testProperties = TestProperties$.MODULE$.instance();

    private ActorSystem untypedSystem = ActorSystemAdapter.toUntyped(testKit.system());
    private TimeService timeService = TimeServiceFactory.make(TaiOffset, untypedSystem);

    private ManualTime manualTime = ManualTime.get(testKit.system());

    //------------------------------UTC-------------------------------

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Test
    public void should_get_utctime(){
        UtcInstant utcInstant = timeService.utcTime();
        Instant fixedInstant = Instant.now();

        long expectedMillis = fixedInstant.toEpochMilli();

        assertEquals((double)expectedMillis, (double)utcInstant.value().toEpochMilli(), 5.0);
    }

    //DEOPSCSW-537: Scala and Java API for conversion between TAI and UTC
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_convert_utc_to_tai(){
        UtcInstant utcInstant = timeService.utcTime();
        TaiInstant taiInstant = timeService.toTai(utcInstant);

        assertEquals(Duration.between(utcInstant.value(),taiInstant.value()).getSeconds(), TaiOffset);
    }

    //DEOPSCSW-534: PTP accuracy and precision while reading UTC
    @Test
    public void should_get_maximum_precision_supported_by_system_in_utc(){
        assertFalse(new RichInstant().formatNanos(testProperties.precision(),  timeService.utcTime().value()).endsWith("000"));
    }

    //------------------------------TAI-------------------------------

    //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_get_tai_time(){
        TaiInstant taiInstant = timeService.taiTime();
        Instant expectedTaiInstant = Instant.now().plusSeconds(TaiOffset);

        long expectedMillis = expectedTaiInstant.toEpochMilli();

        assertEquals(expectedMillis, taiInstant.value().toEpochMilli());
    }

    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_get_tai_offset(){
        assertEquals(TaiOffset, timeService.taiOffset());
    }

    //DEOPSCSW-537: Scala and Java API for conversion between TAI and UTC
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_convert_tai_to_utc(){
        TaiInstant taiInstant = timeService.taiTime();
        UtcInstant utcInstant = timeService.toUtc(taiInstant);

        assertEquals(Duration.between(utcInstant.value(),taiInstant.value()).getSeconds(), TaiOffset);
    }

    //DEOPSCSW-538: PTP accuracy and precision while reading TAI
    @Test
    public void should_get_maximum_precision_supported_by_system_in_tai(){
        assertFalse(new RichInstant().formatNanos(testProperties.precision(), timeService.taiTime().value()).endsWith("000"));
    }

    //------------------------------Scheduling-------------------------------

    //DEOPSCSW-542: Schedule a task to execute in future
    @Test
    public void should_schedule_task_at_start_time(){
        TestProbe testProbe = new TestProbe(untypedSystem);
        String probeMsg = "some message";

        TaiInstant idealScheduleTime = new TaiInstant(timeService.taiTime().value().plusSeconds(1));

        Runnable task = () -> testProbe.ref().tell(probeMsg, ActorRef.noSender());

        timeService.scheduleOnce(idealScheduleTime, task);

        manualTime.timePasses(Duration.ofSeconds(1));
        testProbe.expectMsg(probeMsg);
    }

    //DEOPSCSW-544: Schedule a task to be executed repeatedly
    @Test
    public void should_schedule_a_task_periodically_at_given_interval() {
        List<String> list = new ArrayList<>();

        Cancellable cancellable = timeService.schedulePeriodically(Duration.ofMillis(100), () -> list.add("x"));

        manualTime.timePasses(Duration.ofMillis(500));
        cancellable.cancel();

        assertEquals(list.size(), 6);
    }

    //DEOPSCSW-544: Start a repeating task with initial offset
    @Test
    public void should_schedule_a_task_periodically_at_given_interval_after_start_time() {
        List<String> list = new ArrayList<>();

        TaiInstant startTime = new TaiInstant(timeService.taiTime().value().plusSeconds(1));

        Cancellable cancellable = timeService.schedulePeriodically(startTime, Duration.ofMillis(100), () -> list.add("x"));

        manualTime.timePasses(Duration.ofSeconds(1));
        assertEquals(list.size(), 1);

        manualTime.timePasses(Duration.ofMillis(500));
        cancellable.cancel();
        assertEquals(list.size(), 6);
    }
}
