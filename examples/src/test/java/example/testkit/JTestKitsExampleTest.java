package example.testkit;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import com.typesafe.config.ConfigFactory;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.JComponentType;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.location.api.models.AkkaLocation;
import csw.location.api.models.ComponentId;
import csw.location.api.models.Connection.AkkaConnection;
import csw.prefix.models.Prefix;
import csw.prefix.javadsl.JSubsystem;
import csw.testkit.FrameworkTestKit;
import csw.testkit.javadsl.JCSWService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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

    private ActorSystem<SpawnProtocol.Command> system = frameworkTestKit.actorSystem();
    private ILocationService locationService =
            JHttpLocationServiceFactory.makeLocalClient(system);

    @Test
    public void shouldAbleToSpawnContainerUsingTestKit() throws ExecutionException, InterruptedException {
        //#spawn-using-testkit

        // starting container from container config using testkit
        frameworkTestKit.spawnContainer(ConfigFactory.load("JSampleContainer.conf"));

        // starting standalone component from config using testkit
        // ActorRef<ComponentMessage> componentRef =
        //      frameworkTestKit.spawnStandaloneComponent(ConfigFactory.load("SampleHcdStandalone.conf"));

        //#spawn-using-testkit

        AkkaConnection connection = new AkkaConnection(new ComponentId(new Prefix(JSubsystem.NFIRAOS, "JSampleAssembly"), JComponentType.Assembly()));
        Optional<AkkaLocation> akkaLocation = locationService.resolve(connection, Duration.ofSeconds(5)).get();

        Assert.assertTrue(akkaLocation.isPresent());
        Assert.assertEquals(connection, akkaLocation.orElseThrow().connection());
    }

}
