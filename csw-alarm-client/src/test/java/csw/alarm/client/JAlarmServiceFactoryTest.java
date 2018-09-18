package csw.alarm.client;

import akka.actor.ActorSystem;
import akka.actor.CoordinatedShutdown;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import csw.alarm.api.javadsl.IAlarmService;
import csw.alarm.api.models.FullAlarmSeverity;
import csw.alarm.api.scaladsl.AlarmAdminService;
import csw.alarm.client.internal.commons.AlarmServiceConnection;
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.models.TcpRegistration;
import csw.location.api.commons.ClusterAwareSettings;
import csw.location.javadsl.JLocationServiceFactory;
import csw.logging.commons.LogAdminActorFactory;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static csw.alarm.api.javadsl.JAlarmSeverity.Indeterminate;

// DEOPSCSW-481: Component Developer API available to all CSW components
public class JAlarmServiceFactoryTest {

    private static AlarmServiceTestSetup testSetup = new AlarmServiceTestSetup();
    private AlarmServiceFactory alarmServiceFactory = testSetup.alarmServiceFactory();
    private static ActorSystem seedSystem = ClusterAwareSettings.onPort(3558).system();
    private static ILocationService locationService = JLocationServiceFactory.withSystem(seedSystem);
    private AlarmAdminService alarmService = testSetup.alarmService();

    @BeforeClass
    public static void setup() throws ExecutionException, InterruptedException {
        locationService.register(new TcpRegistration(AlarmServiceConnection.value(), testSetup.sentinelPort(), LogAdminActorFactory.make(seedSystem))).get();
    }

    @Before
    public void beforeTest() throws Exception {
        Config validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf");
        Await.result(alarmService.initAlarms(validAlarmsConfig, true), new FiniteDuration(5, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void teardown() throws ExecutionException, InterruptedException {
        locationService.shutdown(CoordinatedShutdown.unknownReason()).get();
        testSetup.afterAll();
    }

    @Test
    public void shouldCreateClientAlarmServiceUsingLocationService() throws Exception {
        IAlarmService alarmServiceUsingLS = alarmServiceFactory.jMakeClientApi(locationService, seedSystem);
        alarmServiceUsingLS.setSeverity(testSetup.tromboneAxisHighLimitAlarmKey(), Indeterminate).get();

        FullAlarmSeverity alarmSeverity = Await.result(alarmService.getCurrentSeverity(testSetup.tromboneAxisHighLimitAlarmKey()), Duration.create(5, TimeUnit.SECONDS));
        Assert.assertEquals(alarmSeverity, Indeterminate);
    }

    @Test
    public void shouldCreateClientAlarmServiceUsingHostAndPort() throws Exception {
        IAlarmService alarmServiceUsingHostPort = alarmServiceFactory.jMakeClientApi(testSetup.hostname(), testSetup.sentinelPort(), seedSystem);
        alarmServiceUsingHostPort.setSeverity(testSetup.tromboneAxisHighLimitAlarmKey(), Indeterminate).get();

        FullAlarmSeverity alarmSeverity = Await.result(alarmService.getCurrentSeverity(testSetup.tromboneAxisHighLimitAlarmKey()), Duration.create(5, TimeUnit.SECONDS));
        Assert.assertEquals(alarmSeverity, Indeterminate);
    }
}
