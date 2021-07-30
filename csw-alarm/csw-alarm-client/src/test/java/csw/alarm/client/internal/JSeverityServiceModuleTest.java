package csw.alarm.client.internal;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import csw.alarm.api.exceptions.InvalidSeverityException;
import csw.alarm.api.javadsl.IAlarmService;
import csw.alarm.api.javadsl.JAlarmSeverity;
import csw.alarm.api.scaladsl.AlarmAdminService;
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup;
import csw.alarm.models.*;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import csw.time.core.models.UTCTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static csw.alarm.models.Key.AlarmKey;
import static org.junit.Assert.*;

// DEOPSCSW-444: Set severity api for component
// CSW-83: Alarm models should take prefix
public class JSeverityServiceModuleTest extends JUnitSuite {

    private static final AlarmServiceTestSetup alarmServiceTestSetup = new AlarmServiceTestSetup();
    private static final IAlarmService jAlarmService = alarmServiceTestSetup.jAlarmService();
    private static final AlarmAdminService alarmService = alarmServiceTestSetup.alarmService();
    private final Prefix prefix = Prefix.apply(JSubsystem.NFIRAOS, "trombone");

    @Before
    public void setup() throws Exception {
        Config validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf");
        Await.result(alarmService.initAlarms(validAlarmsConfig, true), new FiniteDuration(5, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void afterAll() {
        alarmServiceTestSetup.afterAll();
    }

    private AlarmStatus setSeverityAndGetStatus(AlarmKey alarmKey, AlarmSeverity severity) throws Exception {
        jAlarmService.setSeverity(alarmKey, severity).get();
        return getStatus(alarmKey);
    }

    private AlarmStatus getStatus(AlarmKey alarmKey) throws Exception {
        return Await.result(alarmServiceTestSetup.alarmService().getStatus(alarmKey), new FiniteDuration(2, TimeUnit.SECONDS));
    }

    // DEOPSCSW-459: Update severity to Disconnected if not updated within predefined time
    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    // DEOPSCSW-500: Update alarm time on current severity change
    @Test
    public void setSeverity_shouldSetSeverityForAGivenKey__DEOPSCSW_444_DEOPSCSW_459_DEOPSCSW_462_DEOPSCSW_500() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey(prefix, "tromboneAxisHighLimitAlarm");

        FullAlarmSeverity initialSeverity = Await.result(alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(JAlarmSeverity.Disconnected, initialSeverity);

        AlarmStatus initialStatus = getStatus(tromboneAxisHighLimitAlarm);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$, initialStatus.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Disconnected, initialStatus.latchedSeverity());

        //set severity to Major
        AlarmStatus status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarm, JAlarmSeverity.Major);
        assertEquals(AcknowledgementStatus.Unacknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Major, status.latchedSeverity());
        assertEquals(ShelveStatus.Unshelved$.MODULE$, status.shelveStatus());
        assertNotNull(status.alarmTime());

        //get severity and assert
        FullAlarmSeverity severityAfterSetting = Await.result(alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityAfterSetting, JAlarmSeverity.Major);

        //wait for 1 second and assert expiry of severity
        Thread.sleep(1000);
        FullAlarmSeverity severityAfter1Second = Await.result(alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityAfter1Second, JAlarmSeverity.Disconnected);
        assertEquals(alarmServiceTestSetup.settings().refreshInterval(), new FiniteDuration(1, TimeUnit.SECONDS));
    }

    @Test
    public void setSeverity_shouldThrowInvalidSeverityExceptionWhenUnsupportedSeverityIsProvided__DEOPSCSW_444_CSW_153() {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey(prefix, "tromboneAxisHighLimitAlarm");
        ExecutionException executionException = Assert.assertThrows(ExecutionException.class, () -> setSeverityAndGetStatus(tromboneAxisHighLimitAlarm, JAlarmSeverity.Critical));
        Assert.assertTrue(executionException.getCause() instanceof InvalidSeverityException);
    }

    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    // DEOPSCSW-500: Update alarm time on current severity change
    @Test
    public void setSeverity_shouldLatchAlarmWhenItIsHigherThanPreviousLatchedSeverity__DEOPSCSW_444_DEOPSCSW_462_DEOPSCSW_500() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey(prefix, "tromboneAxisHighLimitAlarm");

        FullAlarmSeverity initialSeverity = Await.result(alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(JAlarmSeverity.Disconnected, initialSeverity);

        AlarmStatus initialStatus = getStatus(tromboneAxisHighLimitAlarm);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$, initialStatus.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Disconnected, initialStatus.latchedSeverity());


        AlarmStatus status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarm, JAlarmSeverity.Major);
        assertEquals(AcknowledgementStatus.Unacknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Major, status.latchedSeverity());
        assertNotNull(status.alarmTime());

        AlarmStatus status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarm, JAlarmSeverity.Warning);
        assertEquals(AcknowledgementStatus.Unacknowledged$.MODULE$, status1.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Major, status1.latchedSeverity());
        // current severity is changed, hence updated alarm time should be > old time
        assertTrue(status1.alarmTime().value().isAfter(status.alarmTime().value()));

        AlarmStatus status2 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarm, JAlarmSeverity.Warning);
        assertEquals(AcknowledgementStatus.Unacknowledged$.MODULE$, status2.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Major, status2.latchedSeverity());
        // current severity is not changed, hence new alarm time == old time
        assertEquals(status2.alarmTime().value(), status1.alarmTime().value());
    }

    @Test
    public void setSeverity_shouldNotAutoAcknowledgeAlarmEvenWhenItIsAutoAcknowlegable__DEOPSCSW_444() throws Exception {
        AlarmKey tromboneAxisLowLimitAlarm = new AlarmKey(prefix, "tromboneAxisHighLimitAlarm");

        FullAlarmSeverity initialSeverity = Await.result(alarmService.getCurrentSeverity(tromboneAxisLowLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(JAlarmSeverity.Disconnected, initialSeverity);

        AlarmStatus initialStatus = getStatus(tromboneAxisLowLimitAlarm);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$, initialStatus.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Disconnected, initialStatus.latchedSeverity());

        AlarmStatus status = setSeverityAndGetStatus(tromboneAxisLowLimitAlarm, JAlarmSeverity.Major);
        assertEquals(AcknowledgementStatus.Unacknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Major, status.latchedSeverity());
    }

    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    // DEOPSCSW-500: Update alarm time on current severity change
    @Test
    public void setSeverity_shouldNotUpdateAlarmTimeWhenSeverityDoesNotChange__DEOPSCSW_444_DEOPSCSW_462_DEOPSCSW_500() throws Exception {
        // latchable alarm
        AlarmKey highLimitAlarmKey = new AlarmKey(prefix, "tromboneAxisHighLimitAlarm");
        UTCTime defaultAlarmTime = getStatus(highLimitAlarmKey).alarmTime();

        // latch it to major
        AlarmStatus status = setSeverityAndGetStatus(highLimitAlarmKey, JAlarmSeverity.Major);
        assertTrue(status.alarmTime().value().isAfter(defaultAlarmTime.value()));

        // set the severity again to mimic alarm refreshing
        AlarmStatus status1 = setSeverityAndGetStatus(highLimitAlarmKey, JAlarmSeverity.Major);
        assertEquals(status.alarmTime().value(), status1.alarmTime().value());
    }
}
