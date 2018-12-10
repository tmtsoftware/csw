package csw.time.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.testkit.typed.javadsl.ManualTime;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.internal.adapter.ActorSystemAdapter;
import akka.testkit.TestProbe;
import csw.time.api.models.Cancellable;
import csw.time.api.models.TMTTime.TAITime;
import csw.time.api.models.TMTTime.UTCTime;
import csw.time.api.scaladsl.TimeService;
import org.junit.Rule;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TimeServiceTest extends JUnitSuite {
    @Rule
    public TestKitJunitResource testKit = new TestKitJunitResource(ManualTime.config());

    private static int TaiOffset = 37;
    private TestUtil testUtil = new TestUtil();
    private TestProperties testProperties = TestProperties$.MODULE$.instance();
    private ActorSystem untypedSystem = ActorSystemAdapter.toUntyped(testKit.system());
    private ManualTime manualTime = ManualTime.get(testKit.system());

    private TimeService timeService = TimeServiceFactory.make(TaiOffset, untypedSystem);

    //------------------------------UTC-------------------------------

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Test
    public void should_get_utctime() {
        UTCTime utcTime = timeService.utcTimeAtLocal();
        Instant fixedInstant = Instant.now();

        long expectedMillis = fixedInstant.toEpochMilli();

        assertEquals((double) expectedMillis, (double) utcTime.value().toInstant().toEpochMilli(), 5.0);
    }

    //DEOPSCSW-537: Scala and Java API for conversion between TAI and UTC
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_convert_utc_to_tai() {
        UTCTime utcTime = timeService.utcTimeAtLocal();
        TAITime taiTime = timeService.toTAI(utcTime);

        assertEquals(Duration.between(utcTime.value(), taiTime.value()).getSeconds(), TaiOffset);
    }

    //DEOPSCSW-534: PTP accuracy and precision while reading UTC
    @Test
    public void should_get_maximum_precision_supported_by_system_in_utc() {
        assertFalse(new TestUtil().formatWithPrecision(timeService.utcTimeAtLocal(), testProperties.precision()).endsWith("000"));
    }

    //------------------------------TAI-------------------------------

    //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_get_tai_time() {
        TAITime taiTime = timeService.taiTimeAtLocal();
        Instant expectedTaiInstant = Instant.now().plusSeconds(TaiOffset);

        long expectedMillis = expectedTaiInstant.toEpochMilli();

        assertEquals(expectedMillis, taiTime.value().toInstant().toEpochMilli());
    }

    //DEOPSCSW-537: Scala and Java API for conversion between TAI and UTC
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_convert_tai_to_utc() {
        TAITime taiTime = timeService.taiTimeAtLocal();
        UTCTime utcTime = timeService.toUTC(taiTime);

        assertEquals(Duration.between(utcTime.value(), taiTime.value()).getSeconds(), TaiOffset);
    }

    //DEOPSCSW-538: PTP accuracy and precision while reading TAI
    @Test
    public void should_get_maximum_precision_supported_by_system_in_tai() {
        assertFalse(testUtil.formatWithPrecision(timeService.taiTimeAtLocal(), testProperties.precision()).endsWith("000"));
    }

    //------------------------------Scheduling-------------------------------

    //DEOPSCSW-542: Schedule a task to execute in future
    @Test
    public void should_schedule_task_at_start_time() {
        TestProbe testProbe = new TestProbe(untypedSystem);
        String probeMsg = "some message";

        TAITime idealScheduleTime = new TAITime(timeService.taiTimeAtLocal().value().plusSeconds(1));

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

        TAITime startTime = new TAITime(timeService.taiTimeAtLocal().value().plusSeconds(1));

        Cancellable cancellable = timeService.schedulePeriodically(startTime, Duration.ofMillis(100), () -> list.add("x"));

        manualTime.timePasses(Duration.ofSeconds(1));
        assertEquals(list.size(), 1);

        manualTime.timePasses(Duration.ofMillis(500));
        cancellable.cancel();
        assertEquals(list.size(), 6);
    }

    // DEOPSCSW-539: Ability to read local time that is synchronized with PTP time, at remote observing sites
    @Test
    public void should_get_utc_hawaii_time() {
        ZoneId zoneId = ZoneId.of("US/Hawaii");

        ZonedDateTime utcTimeAtHawaii = timeService.utcTimeAtHawaii().value();
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);

        assertEquals(utcTimeAtHawaii.getZone(), zoneId);
        assertEquals(zonedDateTime.toInstant().toEpochMilli(), utcTimeAtHawaii.toInstant().toEpochMilli(), 100);
    }

    // DEOPSCSW-539: Ability to read local time that is synchronized with PTP time, at remote observing sites
    @Test
    public void should_get_utc_local_time() {
        ZoneId zoneId = ZoneId.systemDefault();

        ZonedDateTime utcTimeAtLocal = timeService.utcTimeAtLocal().value();
        ZonedDateTime timeNow = ZonedDateTime.now(zoneId);

        assertEquals(utcTimeAtLocal.getZone(), zoneId);
        assertEquals(timeNow.toInstant().toEpochMilli(), utcTimeAtLocal.toInstant().toEpochMilli(), 100);
    }

    // DEOPSCSW-539: Ability to read local time that is synchronized with PTP time, at remote observing sites
    @Test
    public void should_get_tai_hawaii_time() {
        ZoneId zoneId = ZoneId.of("US/Hawaii");

        ZonedDateTime utcTimeAtHawaii = timeService.taiTimeAtHawaii().value();
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId).plusSeconds(TaiOffset);

        assertEquals(utcTimeAtHawaii.getZone(), zoneId);
        assertEquals(zonedDateTime.toInstant().toEpochMilli(), utcTimeAtHawaii.toInstant().toEpochMilli(), 100);
    }

    // DEOPSCSW-539: Ability to read local time that is synchronized with PTP time, at remote observing sites
    @Test
    public void should_get_tai_local_time() {
        ZoneId zoneId = ZoneId.systemDefault();

        ZonedDateTime utcTimeAtLocal = timeService.taiTimeAtLocal().value();
        ZonedDateTime timeNow = ZonedDateTime.now(zoneId).plusSeconds(TaiOffset);

        assertEquals(utcTimeAtLocal.getZone(), zoneId);
        assertEquals(timeNow.toInstant().toEpochMilli(), utcTimeAtLocal.toInstant().toEpochMilli(), 100);
    }

}
