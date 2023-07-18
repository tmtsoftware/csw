/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.javadsl;

import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.testkit.TestSubscriber;
import org.apache.pekko.stream.testkit.scaladsl.TestSink;
import csw.location.api.CswVersionJvm;
import csw.location.api.JPekkoRegistrationFactory;
import csw.location.api.commons.Constants;
import csw.location.api.javadsl.*;
import csw.location.api.models.*;
import csw.location.api.models.Connection.PekkoConnection;
import csw.location.api.models.Connection.HttpConnection;
import csw.location.api.models.Connection.TcpConnection;
import csw.location.client.ActorSystemFactory;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.location.server.internal.ServerWiring;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.commons.PekkoTypedExtension;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.logging.client.utils.Eventually;
import csw.network.utils.Networks;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import msocket.api.Subscription;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class JLocationServiceImplTest {

    private static ServerWiring wiring;

    private static org.apache.pekko.actor.ActorSystem untypedSystem;
    private static ActorSystem<SpawnProtocol.Command> typedSystem;
    private static ILocationService locationService;
    private static final NetworkType insideNetwork = JNetworkType.Inside;
    private static String hostname;

    private final ComponentId pekkoHcdComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "hcd1"),
            JComponentType.HCD);
    private final PekkoConnection pekkoHcdConnection = new PekkoConnection(pekkoHcdComponentId);

    private final ComponentId tcpServiceComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "exampleTcpService")
            , JComponentType.Service);
    private final TcpConnection tcpServiceConnection = new TcpConnection(tcpServiceComponentId);

    private static ActorRef<Object> actorRef;

    private final ComponentId httpServiceComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "exampleHTTPService"
    ), JComponentType.Service);
    private final HttpConnection httpServiceConnection = new HttpConnection(httpServiceComponentId);
    private final String Path = "/path/to/resource";

    @BeforeClass
    public static void setup() throws Exception {
        wiring = new ServerWiring(false);
        typedSystem = ActorSystemFactory.remote(SpawnProtocol.create(), "test");
        untypedSystem = Adapter.toClassic(typedSystem);
        hostname = Networks.apply(insideNetwork.envKey()).hostname();
        TestProbe<Object> actorTestProbe = TestProbe.create("test-actor", typedSystem);
        actorRef = actorTestProbe.ref();
        locationService = JHttpLocationServiceFactory.makeLocalClient(typedSystem);
        Await.result(wiring.locationHttpService().start("127.0.0.1"), FiniteDuration.create(5, TimeUnit.SECONDS));
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
    public void testRegistrationAndUnregistrationOfHttpComponent__DEOPSCSW_13() throws ExecutionException,
            InterruptedException {
        int port = 8080;
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);

        IRegistrationResult registrationResult = locationService.register(httpRegistration).get();
        Assert.assertEquals(withCswVersion(httpRegistration).location(hostname), registrationResult.location());
        locationService.unregister(httpServiceConnection).get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().get());
    }

    @Test
    // DEOPSCSW-39: examples of Location Service
    public void testLocationServiceRegisterWithPekkoHttpTcpAsSequence__DEOPSCSW_39() throws ExecutionException,
            InterruptedException {
        int port = 8080;

        Metadata metadata = new Metadata(Map.of("", ""));
        PekkoRegistration pekkoRegistration = JPekkoRegistrationFactory.make(pekkoHcdConnection, actorRef, metadata);
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(pekkoRegistration).get();
        locationService.register(httpRegistration).get();
        locationService.register(tcpRegistration).get();

        Assert.assertEquals(3, locationService.list().get().size());
        Assert.assertEquals(withCswVersion(pekkoRegistration).location(hostname),
                locationService.find(pekkoHcdConnection).get().orElseThrow());
        Assert.assertEquals(withCswVersion(pekkoRegistration).metadata(),
                locationService.find(pekkoHcdConnection).get().orElseThrow().metadata());
        Assert.assertEquals(withCswVersion(httpRegistration).location(hostname),
                locationService.find(httpServiceConnection).get().orElseThrow());
        Assert.assertEquals(withCswVersion(tcpRegistration).location(hostname),
                locationService.find(tcpServiceConnection).get().orElseThrow());
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testResolveTcpConnection__DEOPSCSW_13_DEOPSCSW_39() throws ExecutionException, InterruptedException {
        int port = 1234;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(tcpRegistration).get();
        Assert.assertEquals(withCswVersion(tcpRegistration).location(hostname),
                locationService.find(tcpServiceConnection).get().orElseThrow());
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testResolvePekkoConnection__DEOPSCSW_13_DEOPSCSW_39() throws ExecutionException, InterruptedException {
        PekkoRegistration registration = JPekkoRegistrationFactory.make(pekkoHcdConnection, actorRef);
        locationService.register(registration).get();
        Assert.assertEquals(withCswVersion(registration).location(hostname),
                locationService.find(pekkoHcdConnection).get().orElseThrow());
    }

    @Test
    // DEOPSCSW-39: examples of Location Service
    public void testHttpRegistration__DEOPSCSW_39() throws ExecutionException, InterruptedException {
        int port = 8080;
        String Path = "/path/to/resource";
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);

        IRegistrationResult registrationResult = locationService.register(httpRegistration).get();
        Assert.assertEquals(httpServiceComponentId, registrationResult.location().connection().componentId());
    }

    @Test
    // DEOPSCSW-39: examples of Location Service
    public void testTcpRegistration__DEOPSCSW_39() throws ExecutionException, InterruptedException {
        int port = 8080;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(tcpRegistration).get();
        Assert.assertEquals(1, locationService.list().get().size());
        Assert.assertEquals(withCswVersion(tcpRegistration).location(hostname),
                locationService.find(tcpServiceConnection).get().orElseThrow());
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testPekkoRegistration__DEOPSCSW_13_DEOPSCSW_39() throws ExecutionException, InterruptedException {
        PekkoRegistration registration = JPekkoRegistrationFactory.make(pekkoHcdConnection, actorRef);

        locationService.register(registration).get();
        Assert.assertEquals(withCswVersion(registration).location(hostname),
                locationService.find(pekkoHcdConnection).get().orElseThrow());
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testListComponents__DEOPSCSW_13_DEOPSCSW_39() throws ExecutionException, InterruptedException {
        //  Register Http connection
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, 4000, "/svn/");
        locationService.register(httpRegistration).get();

        //  Register Pekko connection
        PekkoRegistration pekkoHcdRegistration = JPekkoRegistrationFactory.make(pekkoHcdConnection, actorRef);
        locationService.register(pekkoHcdRegistration).get();

        //  Register Tcp service
        TcpRegistration tcpServiceRegistration = new TcpRegistration(tcpServiceConnection, 80);
        locationService.register(tcpServiceRegistration).get();

        Set<Location> locations = Set.of(
                withCswVersion(httpRegistration).location(hostname),
                withCswVersion(pekkoHcdRegistration).location(hostname),
                withCswVersion(tcpServiceRegistration).location(hostname)
        );

        Set<Location> actualSetOfLocations = Set.copyOf(locationService.list().get());
        Assert.assertEquals(locations, actualSetOfLocations);
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-24: Filter by comp/service type
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testListComponentsByComponentType__DEOPSCSW_13_DEOPSCSW_24_DEOPSCSW_39() throws ExecutionException,
            InterruptedException {
        //  Register HCD type component
        ComponentId pekkoHcdComponentId = new ComponentId(new Prefix(JSubsystem.NFIRAOS, "tromboneHCD"),
                JComponentType.HCD);
        PekkoConnection pekkoHcdConnection = new PekkoConnection(pekkoHcdComponentId);
        PekkoRegistration pekkoHcdRegistration = JPekkoRegistrationFactory.make(pekkoHcdConnection, actorRef);
        locationService.register(pekkoHcdRegistration).get();

        //  Register Assembly type component
        ComponentId pekkoAssemblyComponentId = new ComponentId(new Prefix(JSubsystem.NFIRAOS, "tromboneAssembly"),
                JComponentType.Assembly);
        PekkoConnection pekkoAssemblyConnection = new PekkoConnection(pekkoAssemblyComponentId);
        PekkoRegistration pekkoAssemblyRegistration = JPekkoRegistrationFactory.make(pekkoAssemblyConnection,
                actorRef);
        locationService.register(pekkoAssemblyRegistration).get();

        //  Register Container type component
        ComponentId pekkoContainerComponentId = new ComponentId(new Prefix(JSubsystem.NFIRAOS, "tromboneContainer"),
                JComponentType.Container);
        PekkoConnection pekkoContainerConnection = new PekkoConnection(pekkoContainerComponentId);
        PekkoRegistration pekkoContainerRegistration = JPekkoRegistrationFactory.make(pekkoContainerConnection,
                actorRef);
        locationService.register(pekkoContainerRegistration).get();

        //  Register Tcp and Http Service
        ComponentId tcpComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "redis"), JComponentType.Service);
        TcpConnection tcpConnection = new TcpConnection(tcpComponentId);
        TcpRegistration tcpServiceRegistration = new TcpRegistration(tcpConnection, 80);
        locationService.register(tcpServiceRegistration).get();

        ComponentId httpServiceComponentId = new ComponentId(new Prefix(JSubsystem.CSW, "ConfigService"),
                JComponentType.Service);
        HttpConnection httpServiceConnection = new HttpConnection(httpServiceComponentId);
        HttpRegistration httpServiceRegistration = new HttpRegistration(httpServiceConnection, 4000, "/config/svn/");
        locationService.register(httpServiceRegistration).get();

        //  Filter by HCD type
        List<Location> hcdLocations = List.of(withCswVersion(pekkoHcdRegistration).location(hostname));
        Assert.assertEquals(hcdLocations, locationService.list(JComponentType.HCD).get());

        //  Filter by Assembly type
        List<Location> assemblyLocations = List.of(withCswVersion(pekkoAssemblyRegistration).location(hostname));
        Assert.assertEquals(assemblyLocations, locationService.list(JComponentType.Assembly).get());

        //  Filter by Container type
        List<Location> containerLocations = List.of(withCswVersion(pekkoContainerRegistration).location(hostname));
        Assert.assertEquals(containerLocations, locationService.list(JComponentType.Container).get());

        //  Filter by Service type
        Set<Location> serviceLocations = Set.of(
                withCswVersion(tcpServiceRegistration).location(hostname),
                withCswVersion(httpServiceRegistration).location(hostname)
        );
        Set<Location> actualSetOfLocations = Set.copyOf(locationService.list(JComponentType.Service).get());
        Assert.assertEquals(serviceLocations, actualSetOfLocations);
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-31: Filter by hostname
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testListComponentsByHostname__DEOPSCSW_13_DEOPSCSW_31_DEOPSCSW_39() throws ExecutionException,
            InterruptedException {
        //  Register Tcp connection
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, 8080);
        locationService.register(tcpRegistration).get();

        //  Register Pekko connection
        PekkoRegistration pekkoRegistration = JPekkoRegistrationFactory.make(pekkoHcdConnection, actorRef);
        locationService.register(pekkoRegistration).get();

        Set<Location> locations = Set.of(
                withCswVersion(tcpRegistration).location(hostname),
                withCswVersion(pekkoRegistration).location(hostname)
        );

        Set<Location> actualSetOfLocations = Set.copyOf(locationService.list(hostname).get());
        Assert.assertEquals(locations, actualSetOfLocations);
    }

    // DEOPSCSW-13: Java API for location service
    // DEOPSCSW-32: Filter by connection type
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testListComponentsByConnectionType__DEOPSCSW_13_DEOPSCSW_32_DEOPSCSW_39() throws ExecutionException,
            InterruptedException {
        // Register Tcp connection
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, 80);
        locationService.register(tcpRegistration).get();

        // Register Http connection
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, 4000, "/config/svn/");
        locationService.register(httpRegistration).get();

        // Register Pekko connection
        PekkoRegistration pekkoRegistration = JPekkoRegistrationFactory.make(pekkoHcdConnection, actorRef);
        locationService.register(pekkoRegistration).get();

        //  filter by Tcp type
        List<Location> tcpLocations = List.of(withCswVersion(tcpRegistration).location(hostname));
        Assert.assertEquals(tcpLocations, locationService.list(JConnectionType.TcpType).get());

        //  filter by Http type
        List<Location> httpLocations = List.of(withCswVersion(httpRegistration).location(hostname));
        Assert.assertEquals(httpLocations, locationService.list(JConnectionType.HttpType).get());

        //  filter by Pekko type
        List<Location> pekkoLocations = List.of(withCswVersion(pekkoRegistration).location(hostname));
        Assert.assertEquals(pekkoLocations, locationService.list(JConnectionType.PekkoType).get());
    }

    //DEOPSCSW-308: Add prefix in Location service models
    //CSW-86: Subsystem should be case-insensitive
    @Test
    public void testListpekkoComponentsByPrefix__DEOPSCSW_308() throws ExecutionException, InterruptedException {
        PekkoConnection pekkoHcdConnection1 = new PekkoConnection(new ComponentId(new Prefix(JSubsystem.NFIRAOS, "ncc" +
                ".trombone.hcd1"), JComponentType.HCD));
        PekkoConnection pekkoHcdConnection2 = new PekkoConnection(new ComponentId(new Prefix(JSubsystem.NFIRAOS, "ncc" +
                ".trombone.assembly2"), JComponentType.HCD));
        PekkoConnection pekkoHcdConnection3 = new PekkoConnection(new ComponentId(new Prefix(JSubsystem.NFIRAOS, "ncc" +
                ".trombone.hcd3"), JComponentType.HCD));

        // Register Pekko connection
        PekkoRegistration pekkoRegistration1 = JPekkoRegistrationFactory.make(pekkoHcdConnection1, actorRef);
        PekkoRegistration pekkoRegistration2 = JPekkoRegistrationFactory.make(pekkoHcdConnection2, actorRef);
        PekkoRegistration pekkoRegistration3 = JPekkoRegistrationFactory.make(pekkoHcdConnection3, actorRef);
        locationService.register(pekkoRegistration1).get();
        locationService.register(pekkoRegistration2).get();
        locationService.register(pekkoRegistration3).get();

        // filter pekko locations by prefix
        List<PekkoLocation> pekkoLocations = List.of(
                (PekkoLocation) withCswVersion(pekkoRegistration1).location(hostname),
                (PekkoLocation) withCswVersion(pekkoRegistration2).location(hostname),
                (PekkoLocation) withCswVersion(pekkoRegistration3).location(hostname)
        );
        Assert.assertEquals(pekkoLocations.size(), locationService.listByPrefix("NFIRAOS.ncc.trombone").get().size());
        Assert.assertTrue(locationService.listByPrefix("NFIRAOS.ncc.trombone").get().containsAll(pekkoLocations));
    }

    // DEOPSCSW-26: Track a connection
    // DEOPSCSW-39: examples of Location Service
    @Test
    public void testTrackingConnection__DEOPSCSW_26_DEOPSCSW_39() throws ExecutionException, InterruptedException {
        int Port = 1234;
        TcpConnection redis1Connection = new TcpConnection(new ComponentId(new Prefix(JSubsystem.CSW, "redis1"),
                JComponentType.Service));
        TcpRegistration redis1Registration = new TcpRegistration(redis1Connection, Port);

        TcpConnection redis2Connection = new TcpConnection(new ComponentId(new Prefix(JSubsystem.CSW, "redis2"),
                JComponentType.Service));
        TcpRegistration redis2registration = new TcpRegistration(redis2Connection, Port);

        Pair<Subscription, TestSubscriber.Probe<TrackingEvent>> source =
                locationService.track(redis1Connection).toMat(TestSink.probe(untypedSystem), Keep.both()).run(typedSystem);
        Subscription subscription = source.first();
        TestSubscriber.Probe<TrackingEvent> trackingEventProbe = source.second();

        IRegistrationResult result = locationService.register(redis1Registration).get();
        IRegistrationResult result2 = locationService.register(redis2registration).get();

        trackingEventProbe.request(1);
        trackingEventProbe.expectNext(new LocationUpdated(withCswVersion(redis1Registration).location(hostname)));

        result.unregister().get();
        result2.unregister().get();

        trackingEventProbe.request(1);
        trackingEventProbe.expectNext(new LocationRemoved(redis1Connection));

        subscription.cancel();
        trackingEventProbe.expectComplete();
    }

    // DEOPSCSW-26: Track a connection
    @Test
    public void testSubscribeConnection__DEOPSCSW_26() throws ExecutionException, InterruptedException {
        int Port = 1234;
        TcpConnection redis1Connection = new TcpConnection(new ComponentId(new Prefix(JSubsystem.CSW, "redis1"),
                JComponentType.Service));
        TcpRegistration redis1Registration = new TcpRegistration(redis1Connection, Port);

        //Test probe actor to receive the TrackingEvent notifications
        TestProbe<TrackingEvent> probe = TestProbe.create(typedSystem);

        Subscription killSwitch = locationService.subscribe(redis1Connection,
                trackingEvent -> probe.ref().tell(trackingEvent));

        locationService.register(redis1Registration).toCompletableFuture().get();
        probe.expectMessage(new LocationUpdated(withCswVersion(redis1Registration).location(hostname)));

        locationService.unregister(redis1Connection).toCompletableFuture().get();
        probe.expectMessage(new LocationRemoved(redis1Registration.connection()));

        //shutdown the notification stream, should no longer receive any notifications
        killSwitch.cancel();
        probe.expectNoMessage(Duration.of(200, ChronoUnit.MILLIS));
    }

    static class TestActor {
        static public Behavior<Object> behavior() {
            return Behaviors.setup(ctx -> {
                ILogger log =
                        new JLoggerFactory(new Prefix(JSubsystem.CSW, Constants.LocationService())).getLogger(ctx,
                                TestActor.class);
                log.info(() -> "in the test actor");

                return Behaviors
                        .receive(Object.class)
                        .onMessageEquals("Kill", Behaviors::stopped)
                        .onAnyMessage(obj -> Behaviors.same())
                        .build();
            });
        }
    }

    // DEOPSCSW-35: CRDT detects comp/service crash
    @Test
    public void testUnregisteringDeadActorByDeathWatch__DEOPSCSW_35() throws ExecutionException, InterruptedException {
        ComponentId componentId = new ComponentId(new Prefix(JSubsystem.NFIRAOS, "hcd1"), JComponentType.HCD);
        PekkoConnection connection = new PekkoConnection(componentId);

        ActorRef<Object> actorRef =
                PekkoTypedExtension.UserActorFactory(typedSystem).spawn(TestActor.behavior(), "test-actor",
                        Props.empty());

        PekkoRegistration registration = JPekkoRegistrationFactory.make(connection, actorRef);
        Location location = withCswVersion(registration).location(hostname);
        IRegistrationResult registrationResult = locationService.register(registration).get();
        Assert.assertEquals(location, registrationResult.location());

        List<Location> locations = Collections.singletonList(location);
        Assert.assertEquals(locations, locationService.list().get());

        actorRef.tell("Kill");

        Eventually.eventually(Duration.ofSeconds(5), () -> {
            try {
                Assert.assertEquals(0, locationService.list().get().size());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    private Registration withCswVersion(Registration registration) {
        return registration.withCswVersion(new CswVersionJvm().get());
    }
}
