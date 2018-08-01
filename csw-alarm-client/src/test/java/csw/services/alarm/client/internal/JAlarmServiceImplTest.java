package csw.services.alarm.client.internal;

import csw.services.alarm.api.JAlarmSeverity;
import csw.services.alarm.api.javadsl.IAlarmService;
import csw.services.alarm.api.models.AlarmSeverity;
import csw.services.alarm.api.models.Key;
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup;
import org.junit.Before;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class JAlarmServiceImplTest extends AlarmServiceTestSetup {

    private IAlarmService jAlarmService = jalarmService();

    @Before
    public void setup() throws Exception {
        String path = this.getClass().getResource("/test-alarms/valid-alarms.conf").getPath();
        File file = new File(path);
        Await.result(alarmService().initAlarms(file, true), new FiniteDuration(2, TimeUnit.SECONDS));
    }

    @Test
    public void shouldSetSeverityInAlarmStoreForGivenKey() throws Exception {
        Key.AlarmKey alarmKey = new Key.AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm");

        // checking that severity for given key before setting severity (Disconnected is default severity if any severity is set)
        AlarmSeverity severityBeforeSetting = Await.result(alarmService().getSeverity(alarmKey), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityBeforeSetting,JAlarmSeverity.Disconnected);

        // setting alarm severity to Major in alarm store for the given key
        jAlarmService.setSeverity(alarmKey, JAlarmSeverity.Major).get();

        // getting the severity key from alarm store for the same key
        AlarmSeverity severityAfterSetting = Await.result(alarmService().getSeverity(alarmKey), new FiniteDuration(2, TimeUnit.SECONDS));
        assertEquals(severityAfterSetting,JAlarmSeverity.Major);
    }
}
