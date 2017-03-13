package csw.services.location.javadsl;

import akka.actor.ActorPath;
import akka.actor.ActorPaths;
import akka.actor.ActorRef;
import akka.serialization.Serialization;
import akka.testkit.TestProbe;
import csw.services.location.common.ActorRuntime;
import csw.services.location.common.Networks;
import csw.services.location.models.*;
import csw.services.location.models.Connection.*;
import org.junit.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class JLocationServiceImplTest {
    static ILocationService locationService;
    static ActorRuntime actorRuntime;
    private int Port = 1234;

    private ComponentId akkaHcdComponentId = JComponentId.componentId("hcd1", JComponentType.HCD);
    private AkkaConnection akkaHcdConnection = JConnection.akkaConnection(akkaHcdComponentId);

    private ComponentId httpServiceComponentId = JComponentId.componentId("configService", JComponentType.Service);
    private HttpConnection httpServiceConnection = JConnection.httpConnection(httpServiceComponentId);

    private ComponentId tcpServiceComponentId = JComponentId.componentId("redis1", JComponentType.Service);
    private TcpConnection tcpServiceConnection = JConnection.tcpConnection(tcpServiceComponentId);

    private TestProbe actorTestProbe = new TestProbe(actorRuntime.actorSystem(), "test-actor");
    private ActorRef actorRef = actorTestProbe.ref();
    private ActorPath actorPath = ActorPaths.fromString(Serialization.serializedActorPath(actorRef));

    private String prefix = "prefix";
    private String Path = "path123";

    @BeforeClass
    public static void setUp() {
        actorRuntime = JActorRuntime.create("test-java");
        locationService = new JLocationServiceImpl(actorRuntime);
    }

    @After
    public void unregisterAllServices() {
        locationService.unregisterAll();
    }

    @AfterClass
    public static void shutdown() {
        actorRuntime.actorSystem().terminate();
    }

    @Test
    public void testRegistrationOfHttpComponent() throws ExecutionException, InterruptedException {
        RegistrationResult registrationResult = locationService.register(JRegistration.httpRegistration(httpServiceConnection, Port, Path)).get();
        Assert.assertEquals(httpServiceComponentId, registrationResult.componentId());
    }

    @Test
    public void testListComponents() throws ExecutionException, InterruptedException, URISyntaxException {
        String Path = "path123";

        locationService.register(JRegistration.httpRegistration(httpServiceConnection, Port, Path)).get();
        URI uri = new URI("http://" + Networks.getPrimaryIpv4Address().getHostAddress() + ":" + Port + "/" + Path);

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(JLocation.resolvedHttpLocation(httpServiceConnection, uri, Path));

        Assert.assertEquals(locations, locationService.list().get());
    }

    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException, URISyntaxException {

        URI uri = new URI("tcp://" + Networks.getPrimaryIpv4Address().getHostAddress() + ":" + Port);

        RegistrationResult registrationResult = locationService.register(JRegistration.tcpRegistaration(tcpServiceConnection, Port)).get();

        Assert.assertEquals(JLocation.resolvedTcpLocation(tcpServiceConnection, uri), locationService.resolve(tcpServiceConnection).get());
    }

    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException, URISyntaxException {

        URI uri = new URI(actorPath.toString());
        RegistrationResult registrationResult = locationService.register(JRegistration.akkaRegistration(akkaHcdConnection, actorRef, prefix)).get();
        Assert.assertEquals(JLocation.resolvedAkkaLocation(akkaHcdConnection, uri, prefix, Optional.ofNullable(actorRef)), locationService.resolve(akkaHcdConnection).get());
    }

    @Test
    public void testLocationServiceRegisterWithAkkaHttpTcpAsSequence() throws ExecutionException, InterruptedException, URISyntaxException {

        RegistrationResult akkaRegistrationResult = locationService.register(JRegistration.akkaRegistration(akkaHcdConnection, actorRef, prefix)).get();
        RegistrationResult httpRegistrationResult = locationService.register(JRegistration.httpRegistration(httpServiceConnection, Port, Path)).get();
        RegistrationResult tcpRegistrationResult = locationService.register(JRegistration.tcpRegistaration(tcpServiceConnection, Port)).get();

        URI tcpUri = new URI("tcp://" + Networks.getPrimaryIpv4Address().getHostAddress() + ":" + Port);
        URI akkaUri = new URI(actorPath.toString());
        URI httpUri = new URI("http://" + Networks.getPrimaryIpv4Address().getHostAddress() + ":" + Port + "/" + Path);

        Assert.assertEquals(3, locationService.list().get().size());
        Assert.assertEquals(JLocation.resolvedAkkaLocation(akkaHcdConnection, akkaUri, prefix, Optional.ofNullable(actorRef)), locationService.resolve(akkaHcdConnection).get());
        Assert.assertEquals(JLocation.resolvedHttpLocation(httpServiceConnection, httpUri, Path), locationService.resolve(httpServiceConnection).get());
        Assert.assertEquals(JLocation.resolvedTcpLocation(tcpServiceConnection, tcpUri), locationService.resolve(tcpServiceConnection).get());

    }

}
