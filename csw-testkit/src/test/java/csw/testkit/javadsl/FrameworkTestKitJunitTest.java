package csw.testkit.javadsl;

import csw.alarm.client.internal.commons.AlarmServiceConnection;
import csw.config.server.commons.ConfigServiceConnection;
import csw.event.client.internal.commons.EventServiceConnection;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.models.HttpLocation;
import csw.location.api.models.TcpLocation;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

// DEOPSCSW-592: Create csw testkit for component writers
public class FrameworkTestKitJunitTest extends JUnitSuite {

    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.ConfigServer));

    private ILocationService locationService = testKit.jLocationService();

    @Test
    public void shouldStartAllProvidedCSWServices() throws ExecutionException, InterruptedException {
        Optional<TcpLocation> alarmLocation = locationService.find(AlarmServiceConnection.value()).get();
        Assert.assertTrue(alarmLocation.isPresent());
        Assert.assertEquals(alarmLocation.get().connection(),AlarmServiceConnection.value());

        Optional<HttpLocation> configLocation = locationService.find(ConfigServiceConnection.value()).get();
        Assert.assertTrue(configLocation.isPresent());
        Assert.assertEquals(configLocation.get().connection(), ConfigServiceConnection.value());

        // EventServer is not provided in FrameworkTestKitJunitResource constructor, hence it should not be started
        Optional<TcpLocation> eventLocation = locationService.find(EventServiceConnection.value()).get();
        Assert.assertEquals(eventLocation, Optional.empty());
    }

}
