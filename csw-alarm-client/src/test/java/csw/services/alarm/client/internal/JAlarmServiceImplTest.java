package csw.services.alarm.client.internal;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigResolveOptions;
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

import java.io.File;
import java.util.concurrent.TimeUnit;

import static csw.services.alarm.api.models.Key.AlarmKey;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


// DEOPSCSW-444: Set severity api for component
public class JAlarmServiceImplTest {

    private static AlarmServiceTestSetup alarmServiceTestSetup = new AlarmServiceTestSetup();
    private static IAlarmService jAlarmService = alarmServiceTestSetup.jAlarmService();
    private static AlarmAdminService alarmService = alarmServiceTestSetup.alarmService();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        File validAlarmsFile = new File(this.getClass().getResource("/test-alarms/valid-alarms.conf").getPath());
        Config validAlarmsConfig = ConfigFactory.parseFile(validAlarmsFile).resolve(ConfigResolveOptions.noSystem());
        Await.result(alarmService.initAlarms(validAlarmsConfig, true), new FiniteDuration(5, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void afterAll() {
        alarmServiceTestSetup.afterAll();
    }

    private AlarmStatus setSeverity(AlarmKey alarmKey, AlarmSeverity severity) throws Exception {
        jAlarmService.setSeverity(alarmKey, severity).get();
        return Await.result(alarmServiceTestSetup.alarmService().getStatus(alarmKey), new FiniteDuration(2, TimeUnit.SECONDS));
    }

    // DEOPSCSW-459: Update severity to Disconnected if not updated within predefined time
    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    @Test
    public void shouldSetSeverityInAlarmStoreForGivenKey() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        AlarmSeverity severityBeforeSetting = Await.result(alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityBeforeSetting, JAlarmSeverity.Disconnected);

        //set severity to Major
        AlarmStatus status = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Major);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(LatchStatus.Latched$.MODULE$, status.latchStatus());
        assertEquals(JAlarmSeverity.Major, status.latchedSeverity());
        assertEquals(ShelveStatus.UnShelved$.MODULE$, status.shelveStatus());
        assertTrue(status.alarmTime().isDefined());

        //get severity and assert
        AlarmSeverity severityAfterSetting = Await.result(alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityAfterSetting, JAlarmSeverity.Major);

        //wait for 1 second and assert expiry of severity
        Thread.sleep(1000);
        AlarmSeverity severityAfter1Second = Await.result(alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityAfter1Second, JAlarmSeverity.Disconnected);
    }

    @Test
    public void shouldThrowInvalidSeverityExceptionWhenUnsupportedSeverityIsProvided() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        exception.expectCause(isA(InvalidSeverityException.class));
        setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Critical);
    }

    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    @Test
    public void shouldNotLatchTheAlarmWhenItsLatchableButNotHighRisk() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        //set severity to Okay
        AlarmStatus status = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Okay);
        assertEquals(AcknowledgementStatus.UnAcknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(LatchStatus.UnLatched$.MODULE$, status.latchStatus());
        assertEquals(JAlarmSeverity.Okay, status.latchedSeverity());
        assertTrue(status.alarmTime().isDefined());

        //set severity to indeterminant
        AlarmStatus status1 = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Indeterminate);
        assertEquals(AcknowledgementStatus.UnAcknowledged$.MODULE$, status1.acknowledgementStatus());
        assertEquals(LatchStatus.UnLatched$.MODULE$, status1.latchStatus());
        assertEquals(JAlarmSeverity.Indeterminate, status1.latchedSeverity());
        assertTrue(status1.alarmTime().get().time().isAfter(status.alarmTime().get().time()));
    }

    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    @Test
    public void shouldLatchAlarmOnlyWhenItIsHighRiskAndHigherThanLatchedSeverityInCaseOfLatchableAlarms() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        AlarmStatus status = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Major);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(LatchStatus.Latched$.MODULE$, status.latchStatus());
        assertEquals(JAlarmSeverity.Major, status.latchedSeverity());
        assertTrue(status.alarmTime().isDefined());

        AlarmStatus status1 = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Warning);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$, status1.acknowledgementStatus());
        assertEquals(LatchStatus.Latched$.MODULE$, status1.latchStatus());
        assertEquals(JAlarmSeverity.Major, status1.latchedSeverity());
        assertEquals(status1.alarmTime().get().time(), status.alarmTime().get().time());

        AlarmStatus status2 = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Okay);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$, status2.acknowledgementStatus());
        assertEquals(LatchStatus.Latched$.MODULE$, status2.latchStatus());
        assertEquals(JAlarmSeverity.Major, status2.latchedSeverity());
        assertEquals(status2.alarmTime().get().time(), status.alarmTime().get().time());
    }

    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    @Test
    public void shouldNotLatchAlarmIfItIsNotLatchble() throws Exception {
        AlarmKey cpuExceededAlarm = new AlarmKey("TCS", "tcsPk", "cpuExceededAlarm");

        AlarmStatus status = setSeverity(cpuExceededAlarm, JAlarmSeverity.Critical);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(LatchStatus.UnLatched$.MODULE$, status.latchStatus());
        assertEquals(JAlarmSeverity.Critical, status.latchedSeverity());
        assertTrue(status.alarmTime().isDefined());

        AlarmStatus status1 = setSeverity(cpuExceededAlarm, JAlarmSeverity.Indeterminate);
        assertEquals(AcknowledgementStatus.Acknowledged$.MODULE$, status1.acknowledgementStatus());
        assertEquals(LatchStatus.UnLatched$.MODULE$, status1.latchStatus());
        assertEquals(JAlarmSeverity.Indeterminate, status1.latchedSeverity());
        assertTrue(status1.alarmTime().get().time().isAfter(status.alarmTime().get().time()));
    }


    @Test
    public void shouldAutoAcknowledgeAlarmOnlyWhenItIsAutoAcknowlegableWhileSettingSeverity() throws Exception {
        AlarmKey tromboneAxisLowLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm");

        AlarmStatus status = setSeverity(tromboneAxisLowLimitAlarm, JAlarmSeverity.Major);
        assertEquals(AcknowledgementStatus.UnAcknowledged$.MODULE$, status.acknowledgementStatus());
        assertEquals(LatchStatus.Latched$.MODULE$, status.latchStatus());
        assertEquals(JAlarmSeverity.Major, status.latchedSeverity());
        assertTrue(status.alarmTime().isDefined());
    }

    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    @Test
    public void shouldUpdateAlarmTimeOnlyWhenSeverityChangesForLatchableAlarmsWhileSettingIt() throws Exception {
        // latchable alarm
        AlarmKey highLimitAlarmKey = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        // latch it to major
        AlarmStatus status = setSeverity(highLimitAlarmKey, JAlarmSeverity.Major);

        // set the severity again to mimic alarm refreshing
        AlarmStatus status1 = setSeverity(highLimitAlarmKey, JAlarmSeverity.Major);

        assertEquals(status.alarmTime().get().time(), status1.alarmTime().get().time());
    }

    // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
    @Test
    public void shouldUpdateAlarmTimeOnlyWhenSeverityChangesForUnlatchableAlarmsWhileSettingIt() throws Exception {
        // un-latchable alarm
        AlarmKey cpuExceededAlarm = new AlarmKey("TCS", "tcsPk", "cpuExceededAlarm");

        // set severity to major
        AlarmStatus status = setSeverity(cpuExceededAlarm, JAlarmSeverity.Major);

        // set the severity again to mimic alarm refreshing
        AlarmStatus status1 = setSeverity(cpuExceededAlarm, JAlarmSeverity.Major);

        assertEquals(status.alarmTime().get().time(), status1.alarmTime().get().time());
    }
}
