package csw.testkit;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import com.typesafe.config.ConfigFactory;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.models.AkkaLocation;
import csw.location.api.models.ComponentId;
import csw.location.api.models.Connection.AkkaConnection;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.testkit.javadsl.JCSWService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static csw.location.api.javadsl.JComponentType.Assembly;

public class JTestKitsExampleTest extends JUnitSuite {

    //#framework-testkit
    private static FrameworkTestKit frameworkTestKit = FrameworkTestKit.create();

    @BeforeClass
    public static void beforeAll() {
        frameworkTestKit.start(JCSWService.ConfigServer, JCSWService.EventServer);
    }

    @AfterClass
    public static void afterAll() {
        frameworkTestKit.shutdown();
    }
    //#framework-testkit

    private ActorSystem system = frameworkTestKit.actorSystem();
    private ILocationService locationService =
            JHttpLocationServiceFactory.makeLocalClient(system, ActorMaterializer.create(system));

    @Test
    public void shouldAbleToSpawnContainerUsingTestKit() throws ExecutionException, InterruptedException {
        //#spawn-using-testkit

        // starting container from container config using testkit
        frameworkTestKit.spawnContainer(ConfigFactory.load("JSampleContainer.conf"));

        // starting standalone component from config using testkit
        // ActorRef<ComponentMessage> componentRef =
        //      frameworkTestKit.spawnStandaloneComponent(ConfigFactory.load("SampleHcdStandalone.conf"));

        //#spawn-using-testkit

        AkkaConnection connection       = new AkkaConnection(new ComponentId("JSampleAssembly", Assembly));
        Optional<AkkaLocation> akkaLocation = locationService.resolve(connection, Duration.ofSeconds(5)).get();

        Assert.assertTrue(akkaLocation.isPresent());
        Assert.assertEquals(connection, akkaLocation.get().connection());
    }

}
