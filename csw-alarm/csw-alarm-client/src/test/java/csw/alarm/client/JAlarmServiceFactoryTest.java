/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client;

import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.SpawnProtocol;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import csw.alarm.api.javadsl.IAlarmService;
import csw.alarm.api.javadsl.JAlarmSeverity;
import csw.alarm.api.scaladsl.AlarmAdminService;
import csw.alarm.client.internal.commons.AlarmServiceConnection;
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup;
import csw.alarm.models.FullAlarmSeverity;
import csw.location.api.javadsl.ILocationService;
import csw.location.client.ActorSystemFactory;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.location.api.models.TcpRegistration;
import csw.location.server.http.JHTTPLocationService;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// DEOPSCSW-481: Component Developer API available to all CSW components
public class JAlarmServiceFactoryTest {
    // start location http server
    private static final JHTTPLocationService jHttpLocationService = new JHTTPLocationService();

    private static final AlarmServiceTestSetup testSetup = new AlarmServiceTestSetup();
    private final AlarmServiceFactory alarmServiceFactory = testSetup.alarmServiceFactory();
    private static final ActorSystem<SpawnProtocol.Command> seedSystem = ActorSystemFactory.remote(SpawnProtocol.create(), "test");
    private static final ILocationService locationService = JHttpLocationServiceFactory.makeLocalClient(seedSystem);
    private final AlarmAdminService alarmService = testSetup.alarmService();

    @BeforeClass
    public static void setup() throws ExecutionException, InterruptedException {
        jHttpLocationService.beforeAll();
        locationService.register(new TcpRegistration(AlarmServiceConnection.value(), testSetup.sentinelPort())).get();
    }

    @Before
    public void beforeTest() throws Exception {
        Config validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf");
        Await.result(alarmService.initAlarms(validAlarmsConfig, true), new FiniteDuration(5, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void teardown() throws Exception {
        seedSystem.terminate();
        Await.result(seedSystem.whenTerminated(), FiniteDuration.create(5, TimeUnit.SECONDS));
        jHttpLocationService.afterAll();
        testSetup.afterAll();
    }

    @Test
    public void shouldCreateClientAlarmServiceUsingLocationService__DEOPSCSW_481() throws Exception {
        IAlarmService alarmServiceUsingLS = alarmServiceFactory.jMakeClientApi(locationService, seedSystem);
        alarmServiceUsingLS.setSeverity(testSetup.tromboneAxisHighLimitAlarmKey(), JAlarmSeverity.Indeterminate).get();

        FullAlarmSeverity alarmSeverity = Await.result(alarmService.getCurrentSeverity(testSetup.tromboneAxisHighLimitAlarmKey()), Duration.create(5, TimeUnit.SECONDS));
        Assert.assertEquals(alarmSeverity, JAlarmSeverity.Indeterminate);
    }

    @Test
    public void shouldCreateClientAlarmServiceUsingHostAndPort__DEOPSCSW_481() throws Exception {
        IAlarmService alarmServiceUsingHostPort = alarmServiceFactory.jMakeClientApi(testSetup.hostname(), testSetup.sentinelPort(), seedSystem);
        alarmServiceUsingHostPort.setSeverity(testSetup.tromboneAxisHighLimitAlarmKey(), JAlarmSeverity.Indeterminate).get();

        FullAlarmSeverity alarmSeverity = Await.result(alarmService.getCurrentSeverity(testSetup.tromboneAxisHighLimitAlarmKey()), Duration.create(5, TimeUnit.SECONDS));
        Assert.assertEquals(alarmSeverity, JAlarmSeverity.Indeterminate);
    }
}
