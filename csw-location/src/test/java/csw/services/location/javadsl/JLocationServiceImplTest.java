package csw.services.location.javadsl;

import akka.Done;
import akka.actor.*;
import akka.japi.Pair;
import akka.japi.pf.ReceiveBuilder;
import akka.serialization.Serialization;
import akka.stream.KillSwitch;
import akka.stream.javadsl.Keep;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
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
    static ActorRuntime actorRuntime = new ActorRuntime();
    static ILocationService locationService = JLocationServiceFactory.make(actorRuntime);

    private ComponentId akkaHcdComponentId = new ComponentId("hcd1", JComponentType.HCD);
    private AkkaConnection akkaHcdConnection = new Connection.AkkaConnection(akkaHcdComponentId);

    ComponentId tcpServiceComponentId = new ComponentId("exampleTcpService", JComponentType.Service);
    TcpConnection tcpServiceConnection = new Connection.TcpConnection(tcpServiceComponentId);

    private TestProbe actorTestProbe = new TestProbe(actorRuntime.actorSystem(), "test-actor");
    private ActorRef actorRef = actorTestProbe.ref();
    private ActorPath actorPath = ActorPaths.fromString(Serialization.serializedActorPath(actorRef));

    private String prefix = "prefix";

    ComponentId httpServiceComponentId = new ComponentId("exampleHTTPService", JComponentType.Service);
    HttpConnection httpServiceConnection = new Connection.HttpConnection(httpServiceComponentId);
    String Path = "/path/to/resource";


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
        int port = 8080;

        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);

        IRegistrationResult registrationResult = locationService.register(httpRegistration).toCompletableFuture().get();
        Assert.assertEquals(httpRegistration.location(Networks.hostname()), registrationResult.location());
        locationService.unregister(httpServiceConnection).toCompletableFuture().get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().toCompletableFuture().get());
    }

    @Test
    public void testLocationServiceRegisterWithAkkaHttpTcpAsSequence() throws ExecutionException, InterruptedException {
        int port = 8080;
        AkkaRegistration akkaRegistration = new AkkaRegistration(akkaHcdConnection, actorRef);
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(akkaRegistration).toCompletableFuture().get();
        locationService.register(httpRegistration).toCompletableFuture().get();
        locationService.register(tcpRegistration).toCompletableFuture().get();

        Assert.assertEquals(3, locationService.list().toCompletableFuture().get().size());
        Assert.assertEquals(akkaRegistration.location(Networks.hostname()), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
        Assert.assertEquals(httpRegistration.location(Networks.hostname()), locationService.resolve(httpServiceConnection).toCompletableFuture().get().get());
        Assert.assertEquals(tcpRegistration.location(Networks.hostname()), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException {
        int port = 1234;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(tcpRegistration).toCompletableFuture().get();
        Assert.assertEquals(tcpRegistration.location(Networks.hostname()), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException {

        AkkaRegistration registration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(registration).toCompletableFuture().get();
        Assert.assertEquals(registration.location(Networks.hostname()), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testHttpRegistration() throws ExecutionException, InterruptedException {
        int port = 8080;
        String Path = "/path/to/resource";

        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);

        CompletionStage<IRegistrationResult> completionStage = locationService.register(httpRegistration);
        CompletableFuture<IRegistrationResult> completableFuture = completionStage.toCompletableFuture();
        IRegistrationResult registrationResult = completableFuture.get();
        Assert.assertEquals(httpServiceComponentId, registrationResult.location().connection().componentId());
        CompletionStage<Done> uCompletionStage = locationService.unregister(httpServiceConnection);
        CompletableFuture<Done> uCompletableFuture = uCompletionStage.toCompletableFuture();
        Done done = uCompletableFuture.get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().toCompletableFuture().get());
    }

    @Test
    public void testTcpRegistration() throws ExecutionException, InterruptedException {
        int port = 8080;

        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        CompletionStage<IRegistrationResult> completionStage = locationService.register(tcpRegistration);
        CompletableFuture<IRegistrationResult> completableFuture = completionStage.toCompletableFuture();
        IRegistrationResult registrationResult = completableFuture.get();
        Assert.assertEquals(1, locationService.list().toCompletableFuture().get().size());
        Assert.assertEquals(tcpRegistration.location(actorRuntime.hostname()), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());

        CompletionStage<Done> uCompletionStage = locationService.unregister(tcpServiceConnection);
        CompletableFuture<Done> uCompletableFuture = uCompletionStage.toCompletableFuture();
        Done done = uCompletableFuture.get();
    }

    @Test
    public void testAkkaRegistration() throws ExecutionException, InterruptedException {

        AkkaRegistration registration = new AkkaRegistration(akkaHcdConnection, actorRef);

        CompletionStage<IRegistrationResult> completionStage = locationService.register(registration);
        CompletableFuture<IRegistrationResult> completableFuture = completionStage.toCompletableFuture();
        IRegistrationResult registrationResult = completableFuture.get();
        Assert.assertEquals(registration.location(actorRuntime.hostname()), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());

        CompletionStage<Done> uCompletionStage = locationService.unregister(akkaHcdConnection);
        CompletableFuture<Done> uCompletableFuture = uCompletionStage.toCompletableFuture();
        Done done = uCompletableFuture.get();
    }

    @Test
    public void testListComponents() throws ExecutionException, InterruptedException {
        String Path = "path123";
        int port = 8080;
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);
        locationService.register(httpRegistration).toCompletableFuture().get();

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(httpRegistration.location(Networks.hostname()));

        Assert.assertEquals(locations, locationService.list().toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByComponentType() throws ExecutionException, InterruptedException {
        AkkaRegistration akkaRegistration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(akkaRegistration).toCompletableFuture().get();
        ArrayList<Location> locations = new ArrayList<>();
        locations.add(akkaRegistration.location(Networks.hostname()));
        Assert.assertEquals(locations, locationService.list(JComponentType.HCD).toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByHostname() throws ExecutionException, InterruptedException {
        int port = 8080;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);
        locationService.register(tcpRegistration).toCompletableFuture().get();

        AkkaRegistration akkaRegistration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(akkaRegistration).toCompletableFuture().get();

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(tcpRegistration.location(Networks.hostname()));
        locations.add(akkaRegistration.location(Networks.hostname()));

        Assert.assertTrue(locations.size() == 2);
        Assert.assertEquals(locations, locationService.list(Networks.getIpv4Address("").getHostAddress()).toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByConnectionType() throws ExecutionException, InterruptedException {
        int port = 8080;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);
        locationService.register(tcpRegistration).toCompletableFuture().get();
        ArrayList<Location> locations = new ArrayList<>();
        locations.add(tcpRegistration.location(Networks.hostname()));
        Assert.assertEquals(locations, locationService.list(JConnectionType.TcpType).toCompletableFuture().get());
    }

    @Test
    public void testTrackingConnection() throws ExecutionException, InterruptedException {
        int Port = 1234;
        TcpConnection redis1Connection = new TcpConnection(new ComponentId("redis1", JComponentType.Service));
        TcpRegistration redis1Registration = new TcpRegistration(redis1Connection, Port);

        TcpConnection redis2Connection = new TcpConnection(new ComponentId("redis2", JComponentType.Service));
        TcpRegistration redis2registration = new TcpRegistration(redis2Connection, Port);


        Pair<KillSwitch, TestSubscriber.Probe<TrackingEvent>> source = locationService.track(redis1Connection).toMat(TestSink.probe(actorRuntime.actorSystem()), Keep.both()).run(actorRuntime.mat());


        IRegistrationResult result = locationService.register(redis1Registration).toCompletableFuture().get();
        IRegistrationResult result2 = locationService.register(redis2registration).toCompletableFuture().get();

        source.second().request(1);
        source.second().expectNext(new LocationUpdated(redis1Registration.location(Networks.hostname())));

        result.unregister().toCompletableFuture().get();
        result2.unregister().toCompletableFuture().get();

        source.second().request(1);
        source.second().expectNext(new LocationRemoved(redis1Connection));

        source.first().shutdown();
        source.second().expectComplete();
    }

    @Test
    public void testUnregisteringDeadActorByDeathWatch() throws ExecutionException, InterruptedException {
        ComponentId componentId = new ComponentId("hcd1", JComponentType.HCD);
        AkkaConnection connection = new AkkaConnection(componentId);
        String Prefix = "prefix";

        ActorRef actorRef = actorRuntime.actorSystem().actorOf(Props.create(AbstractActor.class, () -> new AbstractActor() {
                    @Override
                    public Receive createReceive() {
                        return ReceiveBuilder.create().build();
                    }
                }),
                "my-actor-to-die"
        );

        Assert.assertEquals(connection, locationService.register(new AkkaRegistration(connection, actorRef)).toCompletableFuture().get().location().connection());

        Thread.sleep(10);

        ArrayList<Location> locations = new ArrayList<>();
        Location location = new AkkaRegistration(connection, actorRef).location(Networks.hostname());
        locations.add(location);
        Assert.assertEquals(locations, locationService.list().toCompletableFuture().get());

        actorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());

        Thread.sleep(2000);

        Assert.assertEquals(0, locationService.list().toCompletableFuture().get().size());
    }
}
