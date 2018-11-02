package csw.testkit.javadsl;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import csw.alarm.client.internal.commons.AlarmServiceConnection;
import csw.config.server.commons.ConfigServiceConnection;
import csw.event.client.internal.commons.EventServiceConnection;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.models.HttpLocation;
import csw.location.api.models.TcpLocation;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class FrameworkTestKitJunitTest {

    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmStore, JCSWService.ConfigServer));

    private ActorSystem system = testKit.frameworkTestKit().actorSystem();
    private Materializer mat = ActorMaterializer.create(system);

    private ILocationService locationService = JHttpLocationServiceFactory.makeLocalClient(system, mat);

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
