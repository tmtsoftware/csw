package csw.services.alarm.client.internal;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import csw.messages.javadsl.JSubsystem;
import csw.services.alarm.api.exceptions.InvalidSeverityException;
import csw.services.alarm.api.javadsl.IAlarmService;
import csw.services.alarm.api.javadsl.JAlarmSeverity;
import csw.services.alarm.api.models.*;
import csw.services.alarm.api.scaladsl.AlarmAdminService;
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static csw.services.alarm.api.models.Key.AlarmKey;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.*;

// DEOPSCSW-444: Set severity api for component
public class JSeverityServiceModuleTest {

    private static AlarmServiceTestSetup alarmServiceTestSetup = new AlarmServiceTestSetup();
    private static IAlarmService jAlarmService = alarmServiceTestSetup.jAlarmService();
    private static AlarmAdminService alarmService = alarmServiceTestSetup.alarmService();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

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
    public void setSeverity_shouldSetSeverityForAGivenKey() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey(JSubsystem.NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm");

        FullAlarmSeverity initialSeverity = Await.result(alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(JAlarmSeverity.Disconnected, initialSeverity);

        AlarmStatus initialStatus = getStatus(tromboneAxisHighLimitAlarm);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$,initialStatus.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Disconnected,initialStatus.latchedSeverity());

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
    public void setSeverity_shouldThrowInvalidSeverityExceptionWhenUnsupportedSeverityIsProvided() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey(JSubsystem.NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm");

        exception.expectCause(isA(InvalidSeverityException.class));
        setSeverityAndGetStatus(tromboneAxisHighLimitAlarm, JAlarmSeverity.Critical);
    }

    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    // DEOPSCSW-500: Update alarm time on current severity change
    @Test
    public void setSeverity_shouldLatchAlarmWhenItIsHigherThanPreviousLatchedSeverity() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey(JSubsystem.NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm");

        FullAlarmSeverity initialSeverity = Await.result(alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(JAlarmSeverity.Disconnected, initialSeverity);

        AlarmStatus initialStatus = getStatus(tromboneAxisHighLimitAlarm);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$,initialStatus.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Disconnected,initialStatus.latchedSeverity());


        AlarmStatus status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarm, JAlarmSeverity.Major);
        assertEquals(AcknowledgementStatus.Unacknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Major, status.latchedSeverity());
        assertNotNull(status.alarmTime());

        AlarmStatus status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarm, JAlarmSeverity.Warning);
        assertEquals(AcknowledgementStatus.Unacknowledged$.MODULE$, status1.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Major, status1.latchedSeverity());
        // current severity is changed, hence updated alarm time should be > old time
        assertTrue(status1.alarmTime().time().isAfter(status.alarmTime().time()));

        AlarmStatus status2 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarm, JAlarmSeverity.Warning);
        assertEquals(AcknowledgementStatus.Unacknowledged$.MODULE$, status2.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Major, status2.latchedSeverity());
        // current severity is not changed, hence new alarm time == old time
        assertEquals(status2.alarmTime().time(), status1.alarmTime().time());
    }

    @Test
    public void setSeverity_shouldNotAutoAcknowledgeAlarmEvenWhenItIsAutoAcknowlegable() throws Exception {
        AlarmKey tromboneAxisLowLimitAlarm = new AlarmKey(JSubsystem.NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm");

        FullAlarmSeverity initialSeverity = Await.result(alarmService.getCurrentSeverity(tromboneAxisLowLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(JAlarmSeverity.Disconnected, initialSeverity);

        AlarmStatus initialStatus = getStatus(tromboneAxisLowLimitAlarm);
        assertEquals( AcknowledgementStatus.Acknowledged$.MODULE$,initialStatus.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Disconnected,initialStatus.latchedSeverity());

        AlarmStatus status = setSeverityAndGetStatus(tromboneAxisLowLimitAlarm, JAlarmSeverity.Major);
        assertEquals(AcknowledgementStatus.Unacknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(JAlarmSeverity.Major, status.latchedSeverity());
    }

    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    // DEOPSCSW-500: Update alarm time on current severity change
    @Test
    public void setSeverity_shouldNotUpdateAlarmTimeWhenSeverityDoesNotChange() throws Exception {
        // latchable alarm
        AlarmKey highLimitAlarmKey = new AlarmKey(JSubsystem.NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm");
        AlarmTime defaultAlarmTime = getStatus(highLimitAlarmKey).alarmTime();

        // latch it to major
        AlarmStatus status = setSeverityAndGetStatus(highLimitAlarmKey, JAlarmSeverity.Major);
        assertTrue(status.alarmTime().time().isAfter(defaultAlarmTime.time()));

        // set the severity again to mimic alarm refreshing
        AlarmStatus status1 = setSeverityAndGetStatus(highLimitAlarmKey, JAlarmSeverity.Major);
        assertEquals(status.alarmTime().time(), status1.alarmTime().time());
    }
}
