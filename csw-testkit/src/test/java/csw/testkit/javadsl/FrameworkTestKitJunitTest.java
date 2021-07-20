package csw.testkit.javadsl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import csw.alarm.api.javadsl.IAlarmService;
import csw.alarm.api.javadsl.JAlarmSeverity;
import csw.alarm.client.internal.commons.AlarmServiceConnection;
import csw.alarm.models.AlarmSeverity;
import csw.alarm.models.FullAlarmSeverity;
import csw.alarm.models.Key;
import csw.config.server.commons.ConfigServiceConnection;
import csw.event.client.internal.commons.EventServiceConnection;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.models.HttpLocation;
import csw.location.api.models.TcpLocation;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import csw.testkit.AlarmTestKit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// DEOPSCSW-592: Create csw testkit for component writers
public class FrameworkTestKitJunitTest extends JUnitSuite {

    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.ConfigServer));

    private ILocationService locationService = testKit.jLocationService();
    private AlarmTestKit alarmTestKit = testKit.frameworkTestKit().alarmTestKit();

    @Test
    public void shouldStartAllProvidedCSWServices__DEOPSCSW_592() throws ExecutionException, InterruptedException {
        Optional<TcpLocation> alarmLocation = locationService.find(AlarmServiceConnection.value()).get();
        Assert.assertTrue(alarmLocation.isPresent());
        Assert.assertEquals(alarmLocation.orElseThrow().connection(), AlarmServiceConnection.value());

        Optional<HttpLocation> configLocation = locationService.find(ConfigServiceConnection.value()).get();
        Assert.assertTrue(configLocation.isPresent());
        Assert.assertEquals(configLocation.orElseThrow().connection(), ConfigServiceConnection.value());

        // EventServer is not provided in FrameworkTestKitJunitResource constructor, hence it should not be started
        Optional<TcpLocation> eventLocation = locationService.find(EventServiceConnection.value()).get();
        Assert.assertEquals(eventLocation, Optional.empty());
    }

    @Test
    public void shouldGetSeverityForInitializedAlarmsWithAlarmTestkit__CSW_21() {
        Config validAlarmsConfig = ConfigFactory.parseResources("valid-alarms.conf");
        alarmTestKit.initAlarms(validAlarmsConfig, true);
        Key.AlarmKey alarmKey = new Key.AlarmKey(new Prefix(JSubsystem.NFIRAOS, "trombone"), "tromboneAxisLowLimitAlarm");
        FullAlarmSeverity severity = alarmTestKit.getCurrentSeverity(alarmKey);
        //check initial severity for the initialized alarm key
        Assert.assertEquals(severity.entryName(), JAlarmSeverity.Disconnected.entryName());
    }
}
