package csw.services.location.javadsl;

import akka.actor.ActorPath;
import akka.actor.ActorPaths;
import akka.actor.ActorRef;
import akka.serialization.Serialization;
import akka.testkit.TestProbe;
import csw.services.location.internal.Networks;
import csw.services.location.models.*;
import csw.services.location.models.Connection.AkkaConnection;
import csw.services.location.models.Connection.HttpConnection;
import csw.services.location.models.Connection.TcpConnection;
import csw.services.location.scaladsl.ActorRuntime;
import org.junit.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
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
    public void testRegistrationAndUnregistrationOfHttpComponent() throws ExecutionException, InterruptedException {
        RegistrationResult registrationResult = locationService.register(new ResolvedHttpLocation(httpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port, Path)).toCompletableFuture().get();
        Assert.assertEquals(httpServiceComponentId, registrationResult.componentId());
        locationService.unregister(httpServiceConnection).toCompletableFuture().get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().toCompletableFuture().get());
    }

    @Test
    public void testLocationServiceRegisterWithAkkaHttpTcpAsSequence() throws ExecutionException, InterruptedException {

        locationService.register(new ResolvedAkkaLocation(akkaHcdConnection, actorRef)).toCompletableFuture().get();
        locationService.register(new ResolvedHttpLocation(httpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port, Path)).toCompletableFuture().get();
        locationService.register(new ResolvedTcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port)).toCompletableFuture().get();

        Assert.assertEquals(3, locationService.list().toCompletableFuture().get().size());
        Assert.assertEquals(new ResolvedAkkaLocation(akkaHcdConnection, actorRef), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
        Assert.assertEquals(new ResolvedHttpLocation(httpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port, Path), locationService.resolve(httpServiceConnection).toCompletableFuture().get().get());
        Assert.assertEquals(new ResolvedTcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());

    }

    // #resolve_tcp_connection_test
    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException {
        ResolvedTcpLocation resolvedTcpLocation = new ResolvedTcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port);
        locationService.register(resolvedTcpLocation).toCompletableFuture().get();
        Assert.assertEquals(resolvedTcpLocation, locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());
    }
    // #resolve_tcp_connection_test

    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException {

        locationService.register(new ResolvedAkkaLocation(akkaHcdConnection, actorRef)).toCompletableFuture().get();
        Assert.assertEquals(new ResolvedAkkaLocation(akkaHcdConnection, actorRef), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testListComponents() throws ExecutionException, InterruptedException {
        String Path = "path123";
        ResolvedHttpLocation resolvedHttpLocation = new ResolvedHttpLocation(httpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port, Path);
        locationService.register(resolvedHttpLocation).toCompletableFuture().get();

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(resolvedHttpLocation);

        Assert.assertEquals(locations, locationService.list().toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByComponentType() throws ExecutionException, InterruptedException {
        ResolvedAkkaLocation resolvedAkkaLocation = new ResolvedAkkaLocation(akkaHcdConnection, actorRef);
        locationService.register(resolvedAkkaLocation).toCompletableFuture().get();
        ArrayList<Location> locations = new ArrayList<>();
        locations.add(resolvedAkkaLocation);
        Assert.assertEquals(locations, locationService.list(JComponentType.HCD).toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByHostname() throws ExecutionException, InterruptedException {
        ResolvedTcpLocation resolvedTcpLocation = new ResolvedTcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port);
        locationService.register(resolvedTcpLocation).toCompletableFuture().get();

        ResolvedAkkaLocation resolvedAkkaLocation = new ResolvedAkkaLocation(akkaHcdConnection, actorRef);
        locationService.register(resolvedAkkaLocation).toCompletableFuture().get();

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(resolvedTcpLocation);
        locations.add(resolvedAkkaLocation);

        Assert.assertTrue(locations.size() == 2);
        Assert.assertEquals(locations, locationService.list(Networks.getIpv4Address("").getHostAddress()).toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByConnectionType() throws ExecutionException, InterruptedException {
        ResolvedTcpLocation resolvedTcpLocation = new ResolvedTcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port);
        locationService.register(resolvedTcpLocation).toCompletableFuture().get();
        ArrayList<Location> locations = new ArrayList<>();
        locations.add(resolvedTcpLocation);
        Assert.assertEquals(locations, locationService.list(JConnectionType.TcpType).toCompletableFuture().get());
    }
    
}
