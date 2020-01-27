package csw.location.server.javadsl;

import akka.actor.PoisonPill;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.*;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.javadsl.Behaviors;
import akka.japi.Pair;
import akka.stream.javadsl.Keep;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.scaladsl.TestSink;
import csw.location.api.commons.Constants;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.IRegistrationResult;
import csw.location.api.javadsl.JComponentType;
import csw.location.api.javadsl.JConnectionType;
import csw.location.client.ActorSystemFactory;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.location.models.*;
import csw.location.models.Connection.AkkaConnection;
import csw.location.models.Connection.HttpConnection;
import csw.location.models.Connection.TcpConnection;
import csw.location.server.internal.ServerWiring;
import csw.location.server.scaladsl.RegistrationFactory;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.commons.AkkaTypedExtension;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.network.utils.Networks;
import csw.prefix.models.Prefix;
import csw.prefix.javadsl.JSubsystem;
import msocket.api.Subscription;
import org.junit.*;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class JLocationServiceImplTest extends JUnitSuite {

    private static ServerWiring wiring;

    private static akka.actor.ActorSystem untypedSystem;
    private static ActorSystem<SpawnProtocol.Command> typedSystem;
    private static ILocationService locationService;

    private ComponentId akkaHcdComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "hcd1"), JComponentType.HCD());
    private AkkaConnection akkaHcdConnection = new AkkaConnection(akkaHcdComponentId);

    private ComponentId tcpServiceComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "exampleTcpService"), JComponentType.Service());
    private TcpConnection tcpServiceConnection = new TcpConnection(tcpServiceComponentId);

    private static ActorRef<Object> actorRef;

    private ComponentId httpServiceComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "exampleHTTPService"), JComponentType.Service());
    private HttpConnection httpServiceConnection = new HttpConnection(httpServiceComponentId);
    private String Path = "/path/to/resource";

    private Prefix prefix = new Prefix(JSubsystem.NFIRAOS, "ncc.trombone");

    @BeforeClass
    public static void setup() throws Exception {
        wiring = new ServerWiring();
        typedSystem = ActorSystemFactory.remote(SpawnProtocol.create(), "test");
        untypedSystem = Adapter.toClassic(typedSystem);
        TestProbe<Object> actorTestProbe = TestProbe.create("test-actor", typedSystem);
        actorRef = actorTestProbe.ref();
        locationService = JHttpLocationServiceFactory.makeLocalClient(typedSystem);
        Await.result(wiring.locationHttpService().start(), FiniteDuration.create(5, TimeUnit.SECONDS));
    }

    @After
    public void unregisterAllServices() throws ExecutionException, InterruptedException {
        locationService.unregisterAll().get();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        typedSystem.terminate();
        Await.result(typedSystem.whenTerminated(), FiniteDuration.create(5, TimeUnit.SECONDS));
        Await.result(wiring.actorRuntime().shutdown(), FiniteDuration.create(5, TimeUnit.SECONDS));
    }

    // DEOPSCSW-13: Java API for location service
    @Test
    public void testRegistrationAndUnregistrationOfHttpComponent() throws ExecutionException, InterruptedException {
        int port = 8080;
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);

        IRegistrationResult registrationResult = locationService.register(httpRegistration).get();
        Assert.assertEquals(httpRegistration.location(Networks.apply().hostname()), registrationResult.location());
        locationService.unregister(httpServiceConnection).get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().get());
    }

    @Test
    // DEOPSCSW-39: examples of Location Service
    public void testLocationServiceRegisterWithAkkaHttpTcpAsSequence() throws ExecutionException, InterruptedException {
        int port = 8080;
        AkkaRegistration akkaRegistration = new RegistrationFactory().akkaTyped(akkaHcdConnection, actorRef);
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(akkaRegistration).get();
        locationService.register(httpRegistration).get();
        locationService.register(tcpRegistration).get();

        Assert.assertEquals(3, locationService.list().get().size());
        Assert.assertEquals(akkaRegistration.location(Networks.apply().hostname()), locationService.find(akkaHcdConnection).get().orElseThrow());
        Assert.assertEquals(httpRegistration.location(Networks.apply().hostname()), locationService.find(httpServiceConnection).get().orElseThrow());
        Assert.assertEquals(tcpRegistration.location(Networks.apply().hostname()), locationService.find(tcpServiceConnection).get().orElseThrow());
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException {
        int port = 1234;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(tcpRegistration).get();
        Assert.assertEquals(tcpRegistration.location(Networks.apply().hostname()), locationService.find(tcpServiceConnection).get().orElseThrow());
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException {
        AkkaRegistration registration = new RegistrationFactory().akkaTyped(akkaHcdConnection, actorRef);
        locationService.register(registration).get();
        Assert.assertEquals(registration.location(Networks.apply().hostname()), locationService.find(akkaHcdConnection).get().orElseThrow());
    }

    @Test
    // DEOPSCSW-39: examples of Location Service
    public void testHttpRegistration() throws ExecutionException, InterruptedException {
        int port = 8080;
        String Path = "/path/to/resource";

        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);

        IRegistrationResult registrationResult = locationService.register(httpRegistration).get();
        Assert.assertEquals(httpServiceComponentId, registrationResult.location().connection().componentId());
        locationService.unregister(httpServiceConnection).get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().get());
    }

    @Test
    // DEOPSCSW-39: examples of Location Service
    public void testTcpRegistration() throws ExecutionException, InterruptedException {
        int port = 8080;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(tcpRegistration).get();
        Assert.assertEquals(1, locationService.list().get().size());
        Assert.assertEquals(tcpRegistration.location(Networks.apply().hostname()), locationService.find(tcpServiceConnection).get().orElseThrow());

        locationService.unregister(tcpServiceConnection).get();
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testAkkaRegistration() throws ExecutionException, InterruptedException {
        AkkaRegistration registration = new RegistrationFactory().akkaTyped(akkaHcdConnection, actorRef);

        locationService.register(registration).get();
        Assert.assertEquals(registration.location(Networks.apply().hostname()), locationService.find(akkaHcdConnection).get().orElseThrow());

        locationService.unregister(akkaHcdConnection).get();
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testListComponents() throws ExecutionException, InterruptedException {
        //  Register Http connection
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, 4000, "/svn/");
        locationService.register(httpRegistration).get();

        //  Register Akka connection
        AkkaRegistration akkaHcdRegistration = new RegistrationFactory().akkaTyped(akkaHcdConnection, actorRef);
        locationService.register(akkaHcdRegistration).get();

        //  Register Tcp service
        TcpRegistration tcpServiceRegistration = new TcpRegistration(tcpServiceConnection, 80);
        locationService.register(tcpServiceRegistration).get();

        Set<Location> locations = Set.of(
                httpRegistration.location(Networks.apply().hostname()),
                akkaHcdRegistration.location(Networks.apply().hostname()),
                tcpServiceRegistration.location(Networks.apply().hostname())
        );

        Set<Location> actualSetOfLocations = Set.copyOf(locationService.list().get());
        Assert.assertEquals(locations, actualSetOfLocations);
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-24: Filter by comp/service type
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testListComponentsByComponentType() throws ExecutionException, InterruptedException {
        //  Register HCD type component
        ComponentId akkaHcdComponentId = new ComponentId(new Prefix(JSubsystem.NFIRAOS, "tromboneHCD"), JComponentType.HCD());
        AkkaConnection akkaHcdConnection = new AkkaConnection(akkaHcdComponentId);
        AkkaRegistration akkaHcdRegistration = new RegistrationFactory().akkaTyped(akkaHcdConnection, actorRef);
        locationService.register(akkaHcdRegistration).get();

        //  Register Assembly type component
        ComponentId akkaAssemblyComponentId = new ComponentId(new Prefix(JSubsystem.NFIRAOS, "tromboneAssembly"), JComponentType.Assembly());
        AkkaConnection akkaAssemblyConnection = new AkkaConnection(akkaAssemblyComponentId);
        AkkaRegistration akkaAssemblyRegistration = new RegistrationFactory().akkaTyped(akkaAssemblyConnection, actorRef);
        locationService.register(akkaAssemblyRegistration).get();

        //  Register Container type component
        ComponentId akkaContainerComponentId = new ComponentId(new Prefix(JSubsystem.NFIRAOS, "tromboneContainer"), JComponentType.Container());
        AkkaConnection akkaContainerConnection = new AkkaConnection(akkaContainerComponentId);
        AkkaRegistration akkaContainerRegistration = new RegistrationFactory().akkaTyped(akkaContainerConnection, actorRef);
        locationService.register(akkaContainerRegistration).get();

        //  Register Tcp and Http Service
        ComponentId tcpComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "redis"), JComponentType.Service());
        TcpConnection tcpConnection = new TcpConnection(tcpComponentId);
        TcpRegistration tcpServiceRegistration = new TcpRegistration(tcpConnection, 80);
        locationService.register(tcpServiceRegistration).get();

        ComponentId httpServiceComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "ConfigService"), JComponentType.Service());
        HttpConnection httpServiceConnection = new HttpConnection(httpServiceComponentId);
        HttpRegistration httpServiceRegistration = new HttpRegistration(httpServiceConnection, 4000, "/config/svn/");
        locationService.register(httpServiceRegistration).get();

        //  Filter by HCD type
        List<Location> hcdLocations = List.of(akkaHcdRegistration.location(Networks.apply().hostname()));
        Assert.assertEquals(hcdLocations, locationService.list(JComponentType.HCD()).get());

        //  Filter by Assembly type
        List<Location> assemblyLocations = List.of(akkaAssemblyRegistration.location(Networks.apply().hostname()));
        Assert.assertEquals(assemblyLocations, locationService.list(JComponentType.Assembly()).get());

        //  Filter by Container type
        List<Location> containerLocations = List.of(akkaContainerRegistration.location(Networks.apply().hostname()));
        Assert.assertEquals(containerLocations, locationService.list(JComponentType.Container()).get());

        //  Filter by Service type
        Set<Location> serviceLocations = Set.of(
                tcpServiceRegistration.location(Networks.apply().hostname()),
                httpServiceRegistration.location(Networks.apply().hostname())
        );
        Set<Location> actualSetOfLocations = Set.copyOf(locationService.list(JComponentType.Service()).get());
        Assert.assertEquals(serviceLocations, actualSetOfLocations);
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-31: Filter by hostname
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testListComponentsByHostname() throws ExecutionException, InterruptedException {
        //  Register Tcp connection
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, 8080);
        locationService.register(tcpRegistration).get();

        //  Register Akka connection
        AkkaRegistration akkaRegistration = new RegistrationFactory().akkaTyped(akkaHcdConnection, actorRef);
        locationService.register(akkaRegistration).get();

        Set<Location> locations = Set.of(
                tcpRegistration.location(Networks.apply().hostname()),
                akkaRegistration.location(Networks.apply().hostname())
        );

        Set<Location> actualSetOfLocations = Set.copyOf(locationService.list(Networks.apply().hostname()).get());
        Assert.assertEquals(locations, actualSetOfLocations);
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-32: Filter by connection type
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testListComponentsByConnectionType() throws ExecutionException, InterruptedException {
        // Register Tcp connection
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, 80);
        locationService.register(tcpRegistration).get();

        // Register Http connection
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, 4000, "/config/svn/");
        locationService.register(httpRegistration).get();

        // Register Akka connection
        AkkaRegistration akkaRegistration = new RegistrationFactory().akkaTyped(akkaHcdConnection, actorRef);
        locationService.register(akkaRegistration).get();

        //  filter by Tcp type
        List<Location> tcpLocations = List.of(tcpRegistration.location(Networks.apply().hostname()));
        Assert.assertEquals(tcpLocations, locationService.list(JConnectionType.TcpType()).get());

        //  filter by Http type
        List<Location> httpLocations = List.of(httpRegistration.location(Networks.apply().hostname()));
        Assert.assertEquals(httpLocations, locationService.list(JConnectionType.HttpType()).get());

        //  filter by Akka type
        List<Location> akkaLocations = List.of(akkaRegistration.location(Networks.apply().hostname()));
        Assert.assertEquals(akkaLocations, locationService.list(JConnectionType.AkkaType()).get());
    }

    //DEOPSCSW-308: Add prefix in Location service models
    //CSW-86: Subsystem should be case-insensitive
    @Test
    public void testListakkaComponentsByPrefix() throws ExecutionException, InterruptedException {
        AkkaConnection akkaHcdConnection1 = new AkkaConnection(new ComponentId(new Prefix(JSubsystem.NFIRAOS, "ncc.trombone.hcd1"), JComponentType.HCD()));
        AkkaConnection akkaHcdConnection2 = new AkkaConnection(new ComponentId(new Prefix(JSubsystem.NFIRAOS, "ncc.trombone.assembly2"), JComponentType.HCD()));
        AkkaConnection akkaHcdConnection3 = new AkkaConnection(new ComponentId(new Prefix(JSubsystem.NFIRAOS, "ncc.trombone.hcd3"), JComponentType.HCD()));

        // Register Akka connection
        AkkaRegistration akkaRegistration1 = new RegistrationFactory().akkaTyped(akkaHcdConnection1, actorRef);
        AkkaRegistration akkaRegistration2 = new RegistrationFactory().akkaTyped(akkaHcdConnection2, actorRef);
        AkkaRegistration akkaRegistration3 = new RegistrationFactory().akkaTyped(akkaHcdConnection3, actorRef);
        locationService.register(akkaRegistration1).get();
        locationService.register(akkaRegistration2).get();
        locationService.register(akkaRegistration3).get();

        // filter akka locations by prefix
        List<AkkaLocation> akkaLocations = List.of(
                (AkkaLocation) akkaRegistration1.location(Networks.apply().hostname()),
                (AkkaLocation) akkaRegistration2.location(Networks.apply().hostname()),
                (AkkaLocation) akkaRegistration3.location(Networks.apply().hostname())
        );
        Assert.assertEquals(akkaLocations, locationService.listByPrefix("NFIRAOS.ncc.trombone").get());
    }

    // DEOPSCSW-26: Track a connection
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testTrackingConnection() throws ExecutionException, InterruptedException {
        int Port = 1234;
        TcpConnection redis1Connection = new TcpConnection(new ComponentId(new Prefix(JSubsystem.CSW, "redis1"), JComponentType.Service()));
        TcpRegistration redis1Registration = new TcpRegistration(redis1Connection, Port);

        TcpConnection redis2Connection = new TcpConnection(new ComponentId(new Prefix(JSubsystem.CSW, "redis2"), JComponentType.Service()));
        TcpRegistration redis2registration = new TcpRegistration(redis2Connection, Port);

        Pair<Subscription, TestSubscriber.Probe<TrackingEvent>> source = locationService.track(redis1Connection).toMat(TestSink.probe(untypedSystem), Keep.both()).run(typedSystem);

        IRegistrationResult result = locationService.register(redis1Registration).get();
        IRegistrationResult result2 = locationService.register(redis2registration).get();

        source.second().request(1);
        source.second().expectNext(new LocationUpdated(redis1Registration.location(Networks.apply().hostname())));

        result.unregister();
        result2.unregister();

        source.second().request(1);
        source.second().expectNext(new LocationRemoved(redis1Connection));

        source.first().cancel();
        source.second().expectComplete();
    }

    // DEOPSCSW-26: Track a connection
    @Test
    public void testSubscribeConnection() throws ExecutionException, InterruptedException {
        int Port = 1234;
        TcpConnection redis1Connection = new TcpConnection(new ComponentId(new Prefix(JSubsystem.CSW, "redis1"), JComponentType.Service()));
        TcpRegistration redis1Registration = new TcpRegistration(redis1Connection, Port);

        //Test probe actor to receive the TrackingEvent notifications
        TestProbe probe = TestProbe.create(typedSystem);

        Subscription killSwitch = locationService.subscribe(redis1Connection, trackingEvent -> probe.ref().tell(trackingEvent));

        locationService.register(redis1Registration).toCompletableFuture().get();
        probe.expectMessage(new LocationUpdated(redis1Registration.location(Networks.apply().hostname())));

        locationService.unregister(redis1Connection).toCompletableFuture().get();
        probe.expectMessage(new LocationRemoved(redis1Registration.connection()));

        //shutdown the notification stream, should no longer receive any notifications
        killSwitch.cancel();
        probe.expectNoMessage(Duration.of(200, ChronoUnit.MILLIS));
    }

    class TestActor {
        public Behavior<Object> behavior() {
            return Behaviors.setup(ctx -> {
                ILogger log = new JLoggerFactory(new Prefix(JSubsystem.CSW, Constants.LocationService())).getLogger(ctx, TestActor.class);
                log.info(() -> "in the test actor");
                return Behaviors.same();
            });
        }
    }

    // DEOPSCSW-35: CRDT detects comp/service crash
    @Test
    public void testUnregisteringDeadActorByDeathWatch() throws ExecutionException, InterruptedException {
        ComponentId componentId = new ComponentId(new Prefix(JSubsystem.NFIRAOS, "hcd1"), JComponentType.HCD());
        AkkaConnection connection = new AkkaConnection(componentId);

        ActorRef<Object> actorRef =
                AkkaTypedExtension.UserActorFactory(typedSystem).spawn(new TestActor().behavior(), "my-actor-to-die", Props.empty());

        Assert.assertEquals(connection, locationService.register(new RegistrationFactory().akkaTyped(connection, actorRef)).get().location().connection());

        Thread.sleep(10);

        ArrayList<Location> locations = new ArrayList<>();
        Location location = new RegistrationFactory().akkaTyped(connection, actorRef).location(Networks.apply().hostname());
        locations.add(location);
        Assert.assertEquals(locations, locationService.list().get());

        actorRef.tell(PoisonPill.getInstance());

        Thread.sleep(2000);

        Assert.assertEquals(0, locationService.list().get().size());
    }
}
