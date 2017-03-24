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

import java.util.ArrayList;
import java.util.Collections;
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
        RegistrationResult registrationResult = locationService.register(new HttpLocation(httpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port, Path)).toCompletableFuture().get();
        Assert.assertEquals(httpServiceComponentId, registrationResult.componentId());
        locationService.unregister(httpServiceConnection).toCompletableFuture().get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().toCompletableFuture().get());
    }

    @Test
    public void testLocationServiceRegisterWithAkkaHttpTcpAsSequence() throws ExecutionException, InterruptedException {

        locationService.register(new AkkaLocation(akkaHcdConnection, actorRef)).toCompletableFuture().get();
        locationService.register(new HttpLocation(httpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port, Path)).toCompletableFuture().get();
        locationService.register(new TcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port)).toCompletableFuture().get();

        Assert.assertEquals(3, locationService.list().toCompletableFuture().get().size());
        Assert.assertEquals(new AkkaLocation(akkaHcdConnection, actorRef), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
        Assert.assertEquals(new HttpLocation(httpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port, Path), locationService.resolve(httpServiceConnection).toCompletableFuture().get().get());
        Assert.assertEquals(new TcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());

    }

    // #resolve_tcp_connection_test
    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException {
        TcpLocation tcpLocation = new TcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port);
        locationService.register(tcpLocation).toCompletableFuture().get();
        Assert.assertEquals(tcpLocation, locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());
    }
    // #resolve_tcp_connection_test

    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException {

        locationService.register(new AkkaLocation(akkaHcdConnection, actorRef)).toCompletableFuture().get();
        Assert.assertEquals(new AkkaLocation(akkaHcdConnection, actorRef), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testListComponents() throws ExecutionException, InterruptedException {
        String Path = "path123";
        HttpLocation httpLocation = new HttpLocation(httpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port, Path);
        locationService.register(httpLocation).toCompletableFuture().get();

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(httpLocation);

        Assert.assertEquals(locations, locationService.list().toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByComponentType() throws ExecutionException, InterruptedException {
        AkkaLocation akkaLocation = new AkkaLocation(akkaHcdConnection, actorRef);
        locationService.register(akkaLocation).toCompletableFuture().get();
        ArrayList<Location> locations = new ArrayList<>();
        locations.add(akkaLocation);
        Assert.assertEquals(locations, locationService.list(JComponentType.HCD).toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByHostname() throws ExecutionException, InterruptedException {
        TcpLocation tcpLocation = new TcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port);
        locationService.register(tcpLocation).toCompletableFuture().get();

        AkkaLocation akkaLocation = new AkkaLocation(akkaHcdConnection, actorRef);
        locationService.register(akkaLocation).toCompletableFuture().get();

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(tcpLocation);
        locations.add(akkaLocation);

        Assert.assertTrue(locations.size() == 2);
        Assert.assertEquals(locations, locationService.list(Networks.getIpv4Address("").getHostAddress()).toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByConnectionType() throws ExecutionException, InterruptedException {
        TcpLocation tcpLocation = new TcpLocation(tcpServiceConnection, actorRuntime.ipaddr().getHostAddress(), Port);
        locationService.register(tcpLocation).toCompletableFuture().get();
        ArrayList<Location> locations = new ArrayList<>();
        locations.add(tcpLocation);
        Assert.assertEquals(locations, locationService.list(JConnectionType.TcpType).toCompletableFuture().get());
    }
    
}
