package csw.services.location.javadsl;

import akka.Done;
import akka.actor.*;
import akka.japi.Pair;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.KillSwitch;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
import akka.testkit.TestProbe;
import csw.services.location.internal.Networks;
import csw.services.location.models.*;
import csw.services.location.models.Connection.AkkaConnection;
import csw.services.location.models.Connection.HttpConnection;
import csw.services.location.models.Connection.TcpConnection;
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
    private static ILocationService locationService = JLocationServiceFactory.make();
    private ActorSystem actorSystem = ActorSystem.create("test-actor-system");
    private Materializer mat = ActorMaterializer.create(actorSystem);

    private ComponentId akkaHcdComponentId = new ComponentId("hcd1", JComponentType.HCD);
    private AkkaConnection akkaHcdConnection = new Connection.AkkaConnection(akkaHcdComponentId);

    private ComponentId tcpServiceComponentId = new ComponentId("exampleTcpService", JComponentType.Service);
    private TcpConnection tcpServiceConnection = new Connection.TcpConnection(tcpServiceComponentId);

    private TestProbe actorTestProbe = new TestProbe(actorSystem, "test-actor");
    private ActorRef actorRef = actorTestProbe.ref();

    private ComponentId httpServiceComponentId = new ComponentId("exampleHTTPService", JComponentType.Service);
    private HttpConnection httpServiceConnection = new Connection.HttpConnection(httpServiceComponentId);
    private String Path = "/path/to/resource";


    @After
    public void unregisterAllServices() throws ExecutionException, InterruptedException {
        locationService.unregisterAll().toCompletableFuture().get();
    }

    @AfterClass
    public static void shutdown() {
        locationService.shutdown();
    }

    @Test
    public void testRegistrationAndUnregistrationOfHttpComponent() throws ExecutionException, InterruptedException {
        int port = 8080;

        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);

        IRegistrationResult registrationResult = locationService.register(httpRegistration).toCompletableFuture().get();
        Assert.assertEquals(httpRegistration.location(new Networks().hostname()), registrationResult.location());
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
        Assert.assertEquals(akkaRegistration.location(new Networks().hostname()), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
        Assert.assertEquals(httpRegistration.location(new Networks().hostname()), locationService.resolve(httpServiceConnection).toCompletableFuture().get().get());
        Assert.assertEquals(tcpRegistration.location(new Networks().hostname()), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException {
        int port = 1234;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(tcpRegistration).toCompletableFuture().get();
        Assert.assertEquals(tcpRegistration.location(new Networks().hostname()), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());
    }

    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException {

        AkkaRegistration registration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(registration).toCompletableFuture().get();
        Assert.assertEquals(registration.location(new Networks().hostname()), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());
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
        Assert.assertEquals(tcpRegistration.location(new Networks().hostname()), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());

        CompletionStage<Done> uCompletionStage = locationService.unregister(tcpServiceConnection);
        CompletableFuture<Done> uCompletableFuture = uCompletionStage.toCompletableFuture();
    }

    @Test
    public void testAkkaRegistration() throws ExecutionException, InterruptedException {

        AkkaRegistration registration = new AkkaRegistration(akkaHcdConnection, actorRef);

        CompletionStage<IRegistrationResult> completionStage = locationService.register(registration).toCompletableFuture();
        Assert.assertEquals(registration.location(new Networks().hostname()), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());

        locationService.unregister(akkaHcdConnection).toCompletableFuture().get();
    }

    @Test
    public void testListComponents() throws ExecutionException, InterruptedException {
        String Path = "path123";
        int port = 8080;
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);
        locationService.register(httpRegistration).toCompletableFuture().get();

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(httpRegistration.location(new Networks().hostname()));

        Assert.assertEquals(locations, locationService.list().toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByComponentType() throws ExecutionException, InterruptedException {
        AkkaRegistration akkaRegistration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(akkaRegistration).toCompletableFuture().get();
        ArrayList<Location> locations = new ArrayList<>();
        locations.add(akkaRegistration.location(new Networks().hostname()));
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
        locations.add(tcpRegistration.location(new Networks().hostname()));
        locations.add(akkaRegistration.location(new Networks().hostname()));

        Assert.assertTrue(locations.size() == 2);
        Assert.assertEquals(locations, locationService.list(new Networks().getIpv4Address().getHostAddress()).toCompletableFuture().get());
    }

    @Test
    public void testListComponentsByConnectionType() throws ExecutionException, InterruptedException {
        int port = 8080;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);
        locationService.register(tcpRegistration).toCompletableFuture().get();
        ArrayList<Location> locations = new ArrayList<>();
        locations.add(tcpRegistration.location(new Networks().hostname()));
        Assert.assertEquals(locations, locationService.list(JConnectionType.TcpType).toCompletableFuture().get());
    }

    @Test
    public void testTrackingConnection() throws ExecutionException, InterruptedException {
        int Port = 1234;
        TcpConnection redis1Connection = new TcpConnection(new ComponentId("redis1", JComponentType.Service));
        TcpRegistration redis1Registration = new TcpRegistration(redis1Connection, Port);

        TcpConnection redis2Connection = new TcpConnection(new ComponentId("redis2", JComponentType.Service));
        TcpRegistration redis2registration = new TcpRegistration(redis2Connection, Port);


        Pair<KillSwitch, TestSubscriber.Probe<TrackingEvent>> source = locationService.track(redis1Connection).toMat(TestSink.probe(actorSystem), Keep.both()).run(mat);


        IRegistrationResult result = locationService.register(redis1Registration).toCompletableFuture().get();
        IRegistrationResult result2 = locationService.register(redis2registration).toCompletableFuture().get();

        source.second().request(1);
        source.second().expectNext(new LocationUpdated(redis1Registration.location(new Networks().hostname())));

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

        ActorRef actorRef = actorSystem.actorOf(Props.create(AbstractActor.class, () -> new AbstractActor() {
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
        Location location = new AkkaRegistration(connection, actorRef).location(new Networks().hostname());
        locations.add(location);
        Assert.assertEquals(locations, locationService.list().toCompletableFuture().get());

        actorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());

        Thread.sleep(2000);

        Assert.assertEquals(0, locationService.list().toCompletableFuture().get().size());
    }
}
