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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class JLocationServiceImplTest {
    // #declarations
    static ActorRuntime actorRuntime = new ActorRuntime();
    static ILocationService locationService = JLocationServiceFactory.make(actorRuntime);

    // #declarations

    private ComponentId akkaHcdComponentId = new ComponentId("hcd1", JComponentType.HCD);
    private AkkaConnection akkaHcdConnection = new Connection.AkkaConnection(akkaHcdComponentId);

    //#declare_tcp_component
    ComponentId tcpServiceComponentId = new ComponentId("exampleTcpService", JComponentType.Service);
    TcpConnection tcpServiceConnection = new Connection.TcpConnection(tcpServiceComponentId);
    //#declare_tcp_component

    private TestProbe actorTestProbe = new TestProbe(actorRuntime.actorSystem(), "test-actor");
    private ActorRef actorRef = actorTestProbe.ref();
    private ActorPath actorPath = ActorPaths.fromString(Serialization.serializedActorPath(actorRef));

    private String prefix = "prefix";

    //#declare_http_component
    ComponentId httpServiceComponentId = new ComponentId("exampleHTTPService", JComponentType.Service);
    HttpConnection httpServiceConnection = new Connection.HttpConnection(httpServiceComponentId);
    String Path = "/path/to/resource";
    //#declare_http_component


    @After
    public void unregisterAllServices() throws ExecutionException, InterruptedException {
        locationService.unregisterAll().toCompletableFuture().get();
    }

    @AfterClass
    public static void shutdown() {
        actorRuntime.terminate();
    }

    @Test
    public void testRegistrationAndUnregistrationOfHttpComponent() throws ExecutionException, InterruptedException {
        //#register_http_connection
        int port = 8080;

        //To register a http endpoint on host http://10.1.2.22:8080/path/to/resource
        HttpLocation httpLocation = new HttpLocation(httpServiceConnection, actorRuntime.hostname(), port, Path);

        CompletionStage<RegistrationResult> completionStage = locationService.register(httpLocation);
        CompletableFuture<RegistrationResult> completableFuture = completionStage.toCompletableFuture();

        RegistrationResult registrationResult = completableFuture.get();
        //#register_http_connection
        Assert.assertEquals(httpServiceComponentId, registrationResult.componentId());
        locationService.unregister(httpServiceConnection).toCompletableFuture().get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().toCompletableFuture().get());
    }

    @Test
    public void testLocationServiceRegisterWithAkkaHttpTcpAsSequence() throws ExecutionException, InterruptedException {
        int port = 8080;
        locationService.register(new AkkaLocation(akkaHcdConnection, actorRef)).toCompletableFuture().get();
        locationService.register(new HttpLocation(httpServiceConnection, actorRuntime.hostname(), port, Path)).toCompletableFuture().get();
        locationService.register(new TcpLocation(tcpServiceConnection, actorRuntime.hostname(), port)).toCompletableFuture().get();

        Assert.assertEquals(3, locationService.list().toCompletableFuture().get().size());
        Assert.assertEquals(new AkkaLocation(akkaHcdConnection, actorRef), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
        Assert.assertEquals(new HttpLocation(httpServiceConnection, actorRuntime.hostname(), port, Path), locationService.resolve(httpServiceConnection).toCompletableFuture().get().get());
        Assert.assertEquals(new TcpLocation(tcpServiceConnection, actorRuntime.hostname(), port), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException {
        //#register_tcp_connection

        int port = 1234;
        TcpLocation tcpLocation = new TcpLocation(tcpServiceConnection, actorRuntime.hostname(), port);

        //To register a tcp endpoint on host tcp://10.1.2.22:1234
        CompletionStage<RegistrationResult> completionStage = locationService.register(tcpLocation);
        CompletableFuture<RegistrationResult> completableFuture = completionStage.toCompletableFuture();

        RegistrationResult registrationResult = completableFuture.get();
        //#register_tcp_connection
        Assert.assertEquals(tcpLocation, locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException {

        locationService.register(new AkkaLocation(akkaHcdConnection, actorRef)).toCompletableFuture().get();
        Assert.assertEquals(new AkkaLocation(akkaHcdConnection, actorRef), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testListComponents() throws ExecutionException, InterruptedException {
        String Path = "path123";
        int port = 8080;
        HttpLocation httpLocation = new HttpLocation(httpServiceConnection, actorRuntime.hostname(), port, Path);
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
        int port = 8080;
        TcpLocation tcpLocation = new TcpLocation(tcpServiceConnection, actorRuntime.hostname(), port);
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
        int port = 8080;
        TcpLocation tcpLocation = new TcpLocation(tcpServiceConnection, actorRuntime.hostname(), port);
        locationService.register(tcpLocation).toCompletableFuture().get();
        ArrayList<Location> locations = new ArrayList<>();
        locations.add(tcpLocation);
        Assert.assertEquals(locations, locationService.list(JConnectionType.TcpType).toCompletableFuture().get());
    }
    
}
