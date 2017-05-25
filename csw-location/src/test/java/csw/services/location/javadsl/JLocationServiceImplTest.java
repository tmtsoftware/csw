package csw.services.location.javadsl;

import akka.actor.*;
import akka.japi.Pair;
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
import csw.services.location.scaladsl.ActorSystemFactory;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.scaladsl.LoggingSystem;
import csw.services.logging.scaladsl.LoggingSystemFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class JLocationServiceImplTest implements JLocationServiceLogger {
    private ILogger jLogger = getLogger();

    private static ILocationService locationService = JLocationServiceFactory.make();
    private ActorSystem actorSystem = ActorSystemFactory.remote();
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
        locationService.unregisterAll().get();
    }

    @AfterClass
    public static void shutdown() throws ExecutionException, InterruptedException {
        locationService.shutdown().get();
    }

    @Test
    public void testRegistrationAndUnregistrationOfHttpComponent() throws ExecutionException, InterruptedException {
        int port = 8080;

        jLogger.info(() -> "in the test class");

        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);

        IRegistrationResult registrationResult = locationService.register(httpRegistration).get();
        Assert.assertEquals(httpRegistration.location(new Networks().hostname()), registrationResult.location());
        locationService.unregister(httpServiceConnection).get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().get());
    }

    @Test
    public void testLocationServiceRegisterWithAkkaHttpTcpAsSequence() throws ExecutionException, InterruptedException {
        int port = 8080;
        AkkaRegistration akkaRegistration = new AkkaRegistration(akkaHcdConnection, actorRef);
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, port, Path);
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(akkaRegistration).get();
        locationService.register(httpRegistration).get();
        locationService.register(tcpRegistration).get();

        Assert.assertEquals(3, locationService.list().get().size());
        Assert.assertEquals(akkaRegistration.location(new Networks().hostname()), locationService.find(akkaHcdConnection).get().get());
        Assert.assertEquals(httpRegistration.location(new Networks().hostname()), locationService.find(httpServiceConnection).get().get());
        Assert.assertEquals(tcpRegistration.location(new Networks().hostname()), locationService.find(tcpServiceConnection).get().get());
    }

    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException {
        int port = 1234;
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(tcpRegistration).get();
        Assert.assertEquals(tcpRegistration.location(new Networks().hostname()), locationService.find(tcpServiceConnection).get().get());
    }

    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException {

        AkkaRegistration registration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(registration).get();
        Assert.assertEquals(registration.location(new Networks().hostname()), locationService.find(akkaHcdConnection).get().get());
    }

    @Test
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
    public void testTcpRegistration() throws ExecutionException, InterruptedException {
        int port = 8080;

        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, port);

        locationService.register(tcpRegistration).get();
        Assert.assertEquals(1, locationService.list().get().size());
        Assert.assertEquals(tcpRegistration.location(new Networks().hostname()), locationService.find(tcpServiceConnection).get().get());

        locationService.unregister(tcpServiceConnection).get();
    }

    @Test
    public void testAkkaRegistration() throws ExecutionException, InterruptedException {

        AkkaRegistration registration = new AkkaRegistration(akkaHcdConnection, actorRef);

        locationService.register(registration).get();
        Assert.assertEquals(registration.location(new Networks().hostname()), locationService.find(akkaHcdConnection).get().get());

        locationService.unregister(akkaHcdConnection).get();
    }

    @Test
    public void testListComponents() throws ExecutionException, InterruptedException {
        //  Register Http connection
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, 4000, "/svn/");
        locationService.register(httpRegistration).get();

        //  Register Akka connection
        AkkaRegistration akkaHcdRegistration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(akkaHcdRegistration).get();

        //  Register Tcp service
        TcpRegistration tcpServiceRegistration = new TcpRegistration(tcpServiceConnection, 80);
        locationService.register(tcpServiceRegistration).get();

        Set<Location> locations = new HashSet<>();
        locations.add(httpRegistration.location(new Networks().hostname()));
        locations.add(akkaHcdRegistration.location(new Networks().hostname()));
        locations.add(tcpServiceRegistration.location(new Networks().hostname()));

        Set<Location> actualSetOfLocations = new HashSet(locationService.list().get());
        Assert.assertEquals(locations, actualSetOfLocations);
    }

    @Test
    public void testListComponentsByComponentType() throws ExecutionException, InterruptedException {
        //  Register HCD type component
        ComponentId akkaHcdComponentId = new ComponentId("tromboneHCD", JComponentType.HCD);
        AkkaConnection akkaHcdConnection = new Connection.AkkaConnection(akkaHcdComponentId);
        AkkaRegistration akkaHcdRegistration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(akkaHcdRegistration).get();

        //  Register Assembly type component
        ComponentId akkaAssemblyComponentId = new ComponentId("tromboneAssembly", JComponentType.Assembly);
        AkkaConnection akkaAssemblyConnection = new Connection.AkkaConnection(akkaAssemblyComponentId);
        AkkaRegistration akkaAssemblyRegistration = new AkkaRegistration(akkaAssemblyConnection, actorRef);
        locationService.register(akkaAssemblyRegistration).get();

        //  Register Container type component
        ComponentId akkaContainerComponentId = new ComponentId("tromboneContainer", JComponentType.Container);
        AkkaConnection akkaContainerConnection = new Connection.AkkaConnection(akkaContainerComponentId);
        AkkaRegistration akkaContainerRegistration = new AkkaRegistration(akkaContainerConnection, actorRef);
        locationService.register(akkaContainerRegistration).get();

        //  Register Tcp and Http Service
        ComponentId tcpComponentId = new ComponentId("redis", JComponentType.Service);
        TcpConnection tcpConnection = new Connection.TcpConnection(tcpComponentId);
        TcpRegistration tcpServiceRegistration = new TcpRegistration(tcpConnection, 80);
        locationService.register(tcpServiceRegistration).get();

        ComponentId httpServiceComponentId = new ComponentId("ConfigService", JComponentType.Service);
        HttpConnection httpServiceConnection = new Connection.HttpConnection(httpServiceComponentId);
        HttpRegistration httpServiceRegistration = new HttpRegistration(httpServiceConnection, 4000, "/config/svn/");
        locationService.register(httpServiceRegistration).get();

        //  Filter by HCD type
        List<Location> hcdLocations = new ArrayList<>();
        hcdLocations.add(akkaHcdRegistration.location(new Networks().hostname()));
        Assert.assertEquals(hcdLocations, locationService.list(JComponentType.HCD).get());

        //  Filter by Assembly type
        List<Location> assemblyLocations = new ArrayList<>();
        assemblyLocations.add(akkaAssemblyRegistration.location(new Networks().hostname()));
        Assert.assertEquals(assemblyLocations, locationService.list(JComponentType.Assembly).get());

        //  Filter by Container type
        List<Location> containerLocations = new ArrayList<>();
        containerLocations.add(akkaContainerRegistration.location(new Networks().hostname()));
        Assert.assertEquals(containerLocations, locationService.list(JComponentType.Container).get());

        //  Filter by Service type
        Set<Location> serviceLocations = new HashSet<>();
        serviceLocations.add(tcpServiceRegistration.location(new Networks().hostname()));
        serviceLocations.add(httpServiceRegistration.location(new Networks().hostname()));
        Set<Location> actualSetOfLocations = new HashSet(locationService.list(JComponentType.Service).get());
        Assert.assertEquals(serviceLocations, actualSetOfLocations);
    }

    @Test
    public void testListComponentsByHostname() throws ExecutionException, InterruptedException {
        //  Register Tcp connection
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, 8080);
        locationService.register(tcpRegistration).get();

        //  Register Akka connection
        AkkaRegistration akkaRegistration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(akkaRegistration).get();

        Set<Location> locations = new HashSet<>();
        locations.add(tcpRegistration.location(new Networks().hostname()));
        locations.add(akkaRegistration.location(new Networks().hostname()));

        Set<Location> actualSetOfLocations = new HashSet(locationService.list(new Networks().hostname()).get());
        Assert.assertEquals(locations, actualSetOfLocations);
    }

    @Test
    public void testListComponentsByConnectionType() throws ExecutionException, InterruptedException {
        // Register Tcp connection
        TcpRegistration tcpRegistration = new TcpRegistration(tcpServiceConnection, 80);
        locationService.register(tcpRegistration).get();

        // Register Http connection
        HttpRegistration httpRegistration = new HttpRegistration(httpServiceConnection, 4000, "/config/svn/");
        locationService.register(httpRegistration).get();

        // Register Akka connection
        AkkaRegistration akkaRegistration = new AkkaRegistration(akkaHcdConnection, actorRef);
        locationService.register(akkaRegistration).get();

        //  filter by Tcp type
        ArrayList<Location> tcpLocations = new ArrayList<>();
        tcpLocations.add(tcpRegistration.location(new Networks().hostname()));
        Assert.assertEquals(tcpLocations, locationService.list(JConnectionType.TcpType).get());

        //  filter by Http type
        ArrayList<Location> httpLocations = new ArrayList<>();
        httpLocations.add(httpRegistration.location(new Networks().hostname()));
        Assert.assertEquals(httpLocations, locationService.list(JConnectionType.HttpType).get());

        //  filter by Akka type
        ArrayList<Location> akkaLocations = new ArrayList<>();
        akkaLocations.add(akkaRegistration.location(new Networks().hostname()));
        Assert.assertEquals(akkaLocations, locationService.list(JConnectionType.AkkaType).get());
    }

    @Test
    public void testTrackingConnection() throws ExecutionException, InterruptedException {
        int Port = 1234;
        TcpConnection redis1Connection = new TcpConnection(new ComponentId("redis1", JComponentType.Service));
        TcpRegistration redis1Registration = new TcpRegistration(redis1Connection, Port);

        TcpConnection redis2Connection = new TcpConnection(new ComponentId("redis2", JComponentType.Service));
        TcpRegistration redis2registration = new TcpRegistration(redis2Connection, Port);


        Pair<KillSwitch, TestSubscriber.Probe<TrackingEvent>> source = locationService.track(redis1Connection).toMat(TestSink.probe(actorSystem), Keep.both()).run(mat);


        IRegistrationResult result = locationService.register(redis1Registration).get();
        IRegistrationResult result2 = locationService.register(redis2registration).get();

        source.second().request(1);
        source.second().expectNext(new LocationUpdated(redis1Registration.location(new Networks().hostname())));

        result.unregister();
        result2.unregister();

        source.second().request(1);
        source.second().expectNext(new LocationRemoved(redis1Connection));

        source.first().shutdown();
        source.second().expectComplete();
    }

    @Test
    public void testSubscribeConnection() throws ExecutionException, InterruptedException {
        int Port = 1234;
        TcpConnection redis1Connection = new TcpConnection(new ComponentId("redis1", JComponentType.Service));
        TcpRegistration redis1Registration = new TcpRegistration(redis1Connection, Port);

        //Test probe actor to receive the TrackingEvent notifications
        TestProbe probe = new TestProbe(actorSystem);

        KillSwitch killSwitch = locationService.subscribe(redis1Connection, new Consumer<TrackingEvent>() {
            @Override
            public void accept(TrackingEvent trackingEvent) {
                probe.ref().tell(trackingEvent, ActorRef.noSender());
            }
        });

        locationService.register(redis1Registration).toCompletableFuture().get();
        probe.expectMsg(new LocationUpdated(redis1Registration.location(new Networks().hostname())));

        locationService.unregister(redis1Connection).toCompletableFuture().get();
        probe.expectMsg(new LocationRemoved(redis1Registration.connection()));

        //shutdown the notification stream, should no longer receive any notifications
        killSwitch.shutdown();
        probe.expectNoMsg();
    }

    class TestActor extends JLocationServiceLoggerActor {
        private ILogger jLogger = getLogger();
        @Override
        public AbstractActor.Receive createReceive() {
            jLogger.info(() -> "in the test actor");
            return receiveBuilder().build();
        }
    }

    @Test
    public void testUnregisteringDeadActorByDeathWatch() throws ExecutionException, InterruptedException {
        ComponentId componentId = new ComponentId("hcd1", JComponentType.HCD);
        AkkaConnection connection = new AkkaConnection(componentId);

        ActorRef actorRef = actorSystem.actorOf(Props.create(AbstractActor.class, TestActor::new),"my-actor-to-die");

        Assert.assertEquals(connection, locationService.register(new AkkaRegistration(connection, actorRef)).get().location().connection());

        Thread.sleep(10);

        ArrayList<Location> locations = new ArrayList<>();
        Location location = new AkkaRegistration(connection, actorRef).location(new Networks().hostname());
        locations.add(location);
        Assert.assertEquals(locations, locationService.list().get());

        actorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());

        Thread.sleep(2000);

        Assert.assertEquals(0, locationService.list().get().size());
    }
}
