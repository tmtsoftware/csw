package csw.services.location.javadsl;

import akka.actor.ActorPath;
import akka.actor.ActorPaths;
import akka.actor.ActorRef;
import akka.serialization.Serialization;
import akka.testkit.TestProbe;
import csw.services.location.scaladsl.ActorRuntime;
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

    private ComponentId akkaHcdComponentId = new ComponentId("hcd1", JComponentType.HCD);
    private AkkaConnection akkaHcdConnection = new Connection.AkkaConnection(akkaHcdComponentId);

    private ComponentId httpServiceComponentId = new ComponentId("configService", JComponentType.Service);
    private HttpConnection httpServiceConnection = new Connection.HttpConnection(httpServiceComponentId);

    private ComponentId tcpServiceComponentId = new ComponentId("redis1", JComponentType.Service);
    private TcpConnection tcpServiceConnection = new Connection.TcpConnection(tcpServiceComponentId);

    private TestProbe actorTestProbe = new TestProbe(actorRuntime.actorSystem(), "test-actor");
    private ActorRef actorRef = actorTestProbe.ref();
    private ActorPath actorPath = ActorPaths.fromString(Serialization.serializedActorPath(actorRef));

    private String prefix = "prefix";
    private String Path = "path123";

    @BeforeClass
    public static void setUp() {
        actorRuntime = new ActorRuntime("test-java");
        locationService = JLocationServiceFactory.make(actorRuntime);
    }

    @After
    public void unregisterAllServices() throws ExecutionException, InterruptedException {
        locationService.unregisterAll().toCompletableFuture().get();
    }

    @AfterClass
    public static void shutdown() {
        actorRuntime.actorSystem().terminate();
    }

    @Test
    public void testRegistrationOfHttpComponent() throws ExecutionException, InterruptedException, URISyntaxException {
        URI httpURI = new URI("http://" + actorRuntime.ipaddr().getHostAddress() + ":" + Port + "/" + Path);
        RegistrationResult registrationResult = locationService.register(new ResolvedHttpLocation(httpServiceConnection, httpURI, Path)).toCompletableFuture().get();
        Assert.assertEquals(httpServiceComponentId, registrationResult.componentId());
    }

    @Test
    public void testListComponents() throws ExecutionException, InterruptedException, URISyntaxException {
        String Path = "path123";
        URI httpURI = new URI("http://" + actorRuntime.ipaddr().getHostAddress() + ":" + Port + "/" + Path);
        locationService.register(new ResolvedHttpLocation(httpServiceConnection, httpURI, Path)).toCompletableFuture().get();
        URI uri = new URI("http://" + actorRuntime.ipaddr().getHostAddress() + ":" + Port + "/" + Path);

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(new ResolvedHttpLocation(httpServiceConnection, uri, Path));

        Assert.assertEquals(locations, locationService.list().toCompletableFuture().get());
    }
    // #resolve_tcp_connection_test
    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException, URISyntaxException {

        URI uri = new URI("tcp://" + actorRuntime.ipaddr().getHostAddress() + ":" + Port);

        RegistrationResult registrationResult = locationService.register(new ResolvedTcpLocation(tcpServiceConnection, uri)).toCompletableFuture().get();

        Assert.assertEquals(new ResolvedTcpLocation(tcpServiceConnection, uri), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());
    }
    // #resolve_tcp_connection_test
    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException, URISyntaxException {

        URI uri = new URI(actorPath.toString());
        RegistrationResult registrationResult = locationService.register(new ResolvedAkkaLocation(akkaHcdConnection, uri, prefix, Optional.of(actorRef))).toCompletableFuture().get();
        Assert.assertEquals(new ResolvedAkkaLocation(akkaHcdConnection, uri, prefix, Optional.ofNullable(actorRef)), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testLocationServiceRegisterWithAkkaHttpTcpAsSequence() throws ExecutionException, InterruptedException, URISyntaxException {

        URI akkaUri = new URI(actorPath.toString());
        URI tcpUri = new URI("tcp://" + actorRuntime.ipaddr().getHostAddress() + ":" + Port);
        URI httpUri = new URI("http://" + actorRuntime.ipaddr().getHostAddress() + ":" + Port + "/" + Path);

        RegistrationResult akkaRegistrationResult = locationService.register(new ResolvedAkkaLocation(akkaHcdConnection, akkaUri, prefix, Optional.of(actorRef))).toCompletableFuture().get();
        RegistrationResult httpRegistrationResult = locationService.register(new ResolvedHttpLocation(httpServiceConnection, httpUri, Path)).toCompletableFuture().get();
        RegistrationResult tcpRegistrationResult = locationService.register(new ResolvedTcpLocation(tcpServiceConnection, tcpUri)).toCompletableFuture().get();

        Assert.assertEquals(3, locationService.list().toCompletableFuture().get().size());
        Assert.assertEquals(new ResolvedAkkaLocation(akkaHcdConnection, akkaUri, prefix, Optional.ofNullable(actorRef)), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
        Assert.assertEquals(new ResolvedHttpLocation(httpServiceConnection, httpUri, Path), locationService.resolve(httpServiceConnection).toCompletableFuture().get().get());
        Assert.assertEquals(new ResolvedTcpLocation(tcpServiceConnection, tcpUri), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());

    }

}
