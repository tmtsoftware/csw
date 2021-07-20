package example.testkit;

//#junit-testkit

import com.typesafe.config.ConfigFactory;
import csw.alarm.models.FullAlarmSeverity;
import csw.alarm.models.Key;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import csw.testkit.AlarmTestKit;
import csw.testkit.javadsl.FrameworkTestKitJunitResource;
import csw.testkit.javadsl.JCSWService;
import org.junit.ClassRule;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.Arrays;

public class JUnitIntegrationExampleTest extends JUnitSuite {

    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.EventServer));

    @Test
    public void testSpawningComponentInStandaloneMode() {
        testKit.spawnStandalone(ConfigFactory.load("JSampleHcdStandalone.conf"));

        // ... assertions etc.
    }
}
//#junit-testkit

//#junit-alarm-testkit
class JUnitAlarmTestKitExampleTest extends JUnitSuite {

    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer));
    private AlarmTestKit alarmTestKit = testKit.frameworkTestKit().alarmTestKit();

    @Test
    public void testInitAlarms() {
        alarmTestKit.initAlarms(ConfigFactory.load("valid-alarms.conf"), true);
        // ... assertions etc.
    }
    @Test
    public void useGetCurrentSeverityToFetchSeverityOfInitializedAlarms() {
        Key.AlarmKey key = new Key.AlarmKey(new Prefix(JSubsystem.NFIRAOS, "trombone"),"tromboneAxisLowLimitAlarm");
        FullAlarmSeverity alarmSeverity = alarmTestKit.getCurrentSeverity(key);
        // ... assertions etc.
    }
}
//#junit-alarm-testkit