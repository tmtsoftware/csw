package csw.services.alarm.client.internal;

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


// DEOPSCSW-444: Set severity api for component
public class JAlarmServiceImplTest {

    private static AlarmServiceTestSetup alarmServiceTestSetup = new AlarmServiceTestSetup(26380, 6380);
    private static IAlarmService jAlarmService = alarmServiceTestSetup.jAlarmService();
    private static AlarmAdminService alarmService = alarmServiceTestSetup.alarmService();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        String path = this.getClass().getResource("/test-alarms/valid-alarms.conf").getPath();
        File file = new File(path);
        Await.result(alarmService.initAlarms(file, true), new FiniteDuration(5, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void afterAll() {
        alarmServiceTestSetup.afterAll();
    }

    private AlarmStatus setSeverity(AlarmKey alarmKey, AlarmSeverity severity) throws Exception {
        jAlarmService.setSeverity(alarmKey, severity).get();
        return Await.result(alarmServiceTestSetup.alarmService().getStatus(alarmKey), new FiniteDuration(2, TimeUnit.SECONDS));
    }

    //  DEOPSCSW-459: Update severity to Disconnected if not updated within predefined time
    @Test
    public void shouldSetSeverityInAlarmStoreForGivenKey() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        AlarmSeverity severityBeforeSetting = Await.result(alarmService.getSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityBeforeSetting, JAlarmSeverity.Disconnected);

        //set severity to Major
        AlarmStatus status = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Major);
        AlarmStatus expectedStatus = new AlarmStatus(AcknowledgementStatus.Acknowledged$.MODULE$, LatchStatus.Latched$.MODULE$, JAlarmSeverity.Major, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status, expectedStatus);

        //get severity and assert
        AlarmSeverity severityAfterSetting = Await.result(alarmService.getSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityAfterSetting, JAlarmSeverity.Major);

        //wait for 1 second and assert expiry of severity
        Thread.sleep(1000);
        AlarmSeverity severityAfter1Second = Await.result(alarmService.getSeverity(tromboneAxisHighLimitAlarm), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityAfter1Second, JAlarmSeverity.Disconnected);
    }

    @Test
    public void shouldThrowInvalidSeverityExceptionWhenUnsupportedSeverityIsProvided() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        exception.expectCause(isA(InvalidSeverityException.class));
        setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Critical);
    }

    @Test
    public void shouldNotLatchTheAlarmWhenItsLatchableButNotHighRisk() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        //set severity to Okay
        AlarmStatus status = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Okay);
        AlarmStatus expectedStatus = new AlarmStatus(AcknowledgementStatus.UnAcknowledged$.MODULE$, LatchStatus.UnLatched$.MODULE$, JAlarmSeverity.Okay, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status, expectedStatus);

        //set severity to indeterminant
        AlarmStatus status1 = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Indeterminate);
        AlarmStatus expectedStatus1 = new AlarmStatus(AcknowledgementStatus.UnAcknowledged$.MODULE$, LatchStatus.UnLatched$.MODULE$, JAlarmSeverity.Indeterminate, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status1, expectedStatus1);
    }

    @Test
    public  void shouldLatchAlarmOnlyWhenItIsHighRiskAndHigherThanLatchedSeverityInCaseOfLatchableAlarms() throws Exception {
        AlarmKey tromboneAxisHighLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        AlarmStatus status = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Major);
        AlarmStatus expectedStatus = new AlarmStatus(AcknowledgementStatus.Acknowledged$.MODULE$, LatchStatus.Latched$.MODULE$, JAlarmSeverity.Major, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status,expectedStatus);

        AlarmStatus status1 = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Warning);
        AlarmStatus expectedStatus1 = new AlarmStatus(AcknowledgementStatus.Acknowledged$.MODULE$, LatchStatus.Latched$.MODULE$, JAlarmSeverity.Major, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status1,expectedStatus1);

        AlarmStatus status2 = setSeverity(tromboneAxisHighLimitAlarm, JAlarmSeverity.Okay);
        AlarmStatus expectedStatus2 = new AlarmStatus(AcknowledgementStatus.Acknowledged$.MODULE$, LatchStatus.Latched$.MODULE$, JAlarmSeverity.Major, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status2,expectedStatus2);
    }

    @Test
    public void shouldNotLatchAlarmIfItIsNotLatchble() throws Exception {
        AlarmKey cpuExceededAlarm = new AlarmKey("TCS", "tcsPk", "cpuExceededAlarm");

        AlarmStatus status = setSeverity(cpuExceededAlarm, JAlarmSeverity.Critical);
        AlarmStatus expectedStatus = new AlarmStatus(AcknowledgementStatus.Acknowledged$.MODULE$, LatchStatus.UnLatched$.MODULE$, JAlarmSeverity.Critical, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status,expectedStatus);

        AlarmStatus status1 = setSeverity(cpuExceededAlarm, JAlarmSeverity.Indeterminate);
        AlarmStatus expectedStatus1 = new AlarmStatus(AcknowledgementStatus.Acknowledged$.MODULE$, LatchStatus.UnLatched$.MODULE$, JAlarmSeverity.Indeterminate, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status1,expectedStatus1);
    }


    @Test
    public void shouldAutoAcknowledgeAlarmOnlyWhenItIsAutoAcknowlegableWhileSettingSeverity() throws Exception {
        AlarmKey tromboneAxisLowLimitAlarm = new AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm");

        AlarmStatus status = setSeverity(tromboneAxisLowLimitAlarm, JAlarmSeverity.Major);
        AlarmStatus expectedStatus = new AlarmStatus(AcknowledgementStatus.UnAcknowledged$.MODULE$, LatchStatus.Latched$.MODULE$, JAlarmSeverity.Major, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status,expectedStatus);
    }
}
