package csw.services.alarm.client.internal;

import csw.services.alarm.api.JAlarmSeverity;
import csw.services.alarm.api.javadsl.IAlarmService;
import csw.services.alarm.api.models.*;
import csw.services.alarm.api.scaladsl.AlarmAdminService;
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static csw.services.alarm.api.models.Key.AlarmKey;
import static org.junit.Assert.assertEquals;

public class JAlarmServiceImplTest {

    private static AlarmServiceTestSetup alarmServiceTestSetup =new AlarmServiceTestSetup(26380, 6380);
    private static IAlarmService jAlarmService = alarmServiceTestSetup.jalarmService();
    private static AlarmAdminService alarmService = alarmServiceTestSetup.alarmService();

    @Before
    public void setup() throws Exception {
        String path = this.getClass().getResource("/test-alarms/valid-alarms.conf").getPath();
        File file = new File(path);
        Await.result(alarmService.initAlarms(file, true), new FiniteDuration(2, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void afterAll() {
        alarmServiceTestSetup.afterAll();
    }

    @Test
    public void shouldSetSeverityInAlarmStoreForGivenKey() throws Exception {
        AlarmKey alarmKey = new AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        AlarmSeverity severityBeforeSetting = Await.result(alarmService.getSeverity(alarmKey), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityBeforeSetting,JAlarmSeverity.Disconnected);

        //set severity to Major
        jAlarmService.setSeverity(alarmKey, JAlarmSeverity.Major).get();
        AlarmStatus status = Await.result(alarmServiceTestSetup.alarmService().getStatus(alarmKey),new FiniteDuration(1,TimeUnit.SECONDS));
        AlarmStatus expectedStatus = new AlarmStatus(AcknowledgementStatus.Acknowledged$.MODULE$, LatchStatus.Latched$.MODULE$, JAlarmSeverity.Major, ShelveStatus.UnShelved$.MODULE$);
        assertEquals(status, expectedStatus);

        //get severity and assert
        AlarmSeverity severityAfterSetting = Await.result(alarmService.getSeverity(alarmKey), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityAfterSetting,JAlarmSeverity.Major);

        //wait for 1 second and assert expiry of severity
        Thread.sleep(1000);
        AlarmSeverity severityAfter1Second = Await.result(alarmService.getSeverity(alarmKey), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityAfter1Second,JAlarmSeverity.Disconnected);
    }
}
