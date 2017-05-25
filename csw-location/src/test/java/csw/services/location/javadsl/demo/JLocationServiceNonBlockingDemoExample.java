package csw.services.location.javadsl.demo;

import akka.Done;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Pair;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.KillSwitch;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.testkit.TestProbe;
import csw.services.location.internal.Networks;
import csw.services.location.javadsl.ILocationService;
import csw.services.location.javadsl.JComponentType;
import csw.services.location.javadsl.JConnectionType;
import csw.services.location.javadsl.JLocationServiceFactory;
import csw.services.location.models.*;
import csw.services.location.models.Connection.AkkaConnection;
import csw.services.location.models.Connection.HttpConnection;
import csw.services.location.models.Connection.TcpConnection;
import csw.services.location.scaladsl.ActorSystemFactory;
import csw.services.logging.scaladsl.LoggingSystem;
import csw.services.logging.scaladsl.LoggingSystemFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class JLocationServiceNonBlockingDemoExample {

    private
    //#create-actor-system
    ActorSystem actorSystem = ActorSystemFactory.remote();
    //#create-actor-system

    private Materializer mat = ActorMaterializer.create(actorSystem);

    //static instance is used in testing for reuse and to avoid creating/terminating it for each test.
    private static
    //#create-location-service
    ILocationService locationService = JLocationServiceFactory.make();
    //#create-location-service

    @After
    public void afterEach() {
        locationService.unregisterAll();
    }

    @AfterClass
    public static void shutdown() throws ExecutionException, InterruptedException {

        //#shutdown
        locationService.shutdown().get();
        //#shutdown
    }

    //#Components-Connections-Registrations
    private TcpConnection tcpConnection = new Connection.TcpConnection(new ComponentId("redis", JComponentType.Service));
    private TcpRegistration tcpRegistration = new TcpRegistration(tcpConnection, 6380);

    private HttpConnection httpConnection = new Connection.HttpConnection(new ComponentId("configuration", JComponentType.Service));
    private HttpRegistration httpRegistration = new HttpRegistration(httpConnection, 8080, "path123");

    private AkkaConnection akkaConnection = new Connection.AkkaConnection(new ComponentId("hcd1", JComponentType.HCD));
    private AkkaRegistration akkaRegistration = new AkkaRegistration(akkaConnection, actorSystem.actorOf(Props.create(AbstractActor.class, () -> new AbstractActor() {
                @Override
                public Receive createReceive() {
                    return receiveBuilder().build();
                }
            }),
            "my-actor-1"
    ));
    //#Components-Connections-Registrations

    @Test
    public void demo() throws ExecutionException, InterruptedException {
        //expected list of locations
        ArrayList<Location> expectedLocations = new ArrayList<>();
        expectedLocations.add(tcpRegistration.location(new Networks().hostname()));

        //#register-list-resolve-unregister
        CompletableFuture<Void> completableFuture = locationService.register(tcpRegistration).thenCompose(tcpRegistrationResult -> {
            Assert.assertEquals(tcpRegistration.connection(), tcpRegistrationResult.location().connection());
            return locationService.list().thenCompose(locations -> {
                Assert.assertEquals(expectedLocations, locations);
                return locationService.resolve(tcpConnection, new FiniteDuration(5, TimeUnit.SECONDS)).thenCompose(locationOption -> {
                    Assert.assertEquals(tcpRegistration.location(new Networks().hostname()), locationOption.get());
                    System.out.println(tcpRegistrationResult.location().uri());
                    return tcpRegistrationResult.unregister().thenCompose(done -> {
                        return locationService.list().thenCompose(locations1 -> {
                            Assert.assertEquals(Collections.EMPTY_LIST, locations1);
                            return locationService.find(tcpConnection).thenApply(location -> {
                                Assert.assertEquals(Optional.empty(), location);
                                return null;
                            });
                        });
                    });
                });
            });
        });

        completableFuture.get();
        //#register-list-resolve-unregister
    }

    @Test
    public void tracking() throws ExecutionException, InterruptedException {
        //#tracking
        Pair<KillSwitch, CompletionStage<Done>> stream = locationService.track(tcpConnection).toMat(Sink.foreach(System.out::println), Keep.both()).run(mat);

        Thread.sleep(200);

        CompletableFuture<Void> registrationFuture = locationService.register(tcpRegistration).thenCompose(tcpRegistrationResult -> {
            return locationService.register(httpRegistration).thenApply(httpRegistrationResult -> {
                return null;
            });
        });
        registrationFuture.get();

        Thread.sleep(200);

        CompletableFuture<Object> unRegistrationFuture = locationService.unregister(tcpConnection).thenCompose(uResult1 -> {
            return locationService.unregister(httpConnection).thenApply(uResult -> {
                return null;
            });
        });
        unRegistrationFuture.toCompletableFuture().get();

        Thread.sleep(400);
        stream.first().shutdown();
        //#tracking
    }


    @Test
    public void filtering() throws ExecutionException, InterruptedException {
        //#filtering
        //expected list of locations
        ArrayList<Location> expectedLocations1 = new ArrayList<>();
        expectedLocations1.add(tcpRegistration.location(new Networks().hostname()));
        expectedLocations1.add(httpRegistration.location(new Networks().hostname()));
        expectedLocations1.add(akkaRegistration.location(new Networks().hostname()));

        //Filter by type
        ArrayList<Location> expectedLocations2 = new ArrayList<>();
        expectedLocations2.add(akkaRegistration.location(new Networks().hostname()));

        //Filter by service
        ArrayList<Location> expectedLocations3 = new ArrayList<>();
        expectedLocations3.add(tcpRegistration.location(new Networks().hostname()));
        expectedLocations3.add(httpRegistration.location(new Networks().hostname()));

        //Filter by hostname
        ArrayList<Location> expectedLocations4 = new ArrayList<>();
        expectedLocations4.add(tcpRegistration.location(new Networks().hostname()));
        expectedLocations4.add(httpRegistration.location(new Networks().hostname()));
        expectedLocations4.add(akkaRegistration.location(new Networks().hostname()));

        CompletableFuture<Void> completableFuture = locationService.register(tcpRegistration).thenCompose(tcpRegistrationResult -> {
            return locationService.register(httpRegistration).thenCompose(httpRegistrationResult -> {
                return locationService.register(akkaRegistration).thenCompose(akkaRegistrationResult -> {
                    return locationService.list().thenCompose(locations1 -> {
                        Assert.assertEquals(expectedLocations1, locations1);
                        return locationService.list(JConnectionType.AkkaType).thenCompose(locations2 -> {
                            Assert.assertEquals(expectedLocations2, locations2);
                            return locationService.list(JComponentType.Service).thenCompose(locations3 -> {
                                Assert.assertEquals(expectedLocations3, locations3);
                                return locationService.list(new Networks().hostname()).thenApply(locations4 -> {
                                    Assert.assertEquals(expectedLocations4, locations4);
                                    return null;
                                });
                            });
                        });
                    });
                });
            });
        });

        completableFuture.get();
        //#filtering
        locationService.unregisterAll();
    }

    @Test
    public void subscribing() throws ExecutionException, InterruptedException {
        //#subscribing
        //Test probe actor to receive the TrackingEvent notifications
        TestProbe probe = new TestProbe(actorSystem);

        KillSwitch killSwitch = locationService.subscribe(tcpRegistration.connection(), new Consumer<TrackingEvent>() {
            @Override
            public void accept(TrackingEvent trackingEvent) {
                probe.ref().tell(trackingEvent, ActorRef.noSender());
            }
        });

        locationService.register(tcpRegistration).toCompletableFuture().get();
        probe.expectMsg(new LocationUpdated(tcpRegistration.location(new Networks().hostname())));

        locationService.unregister(tcpConnection).toCompletableFuture().get();
        probe.expectMsg(new LocationRemoved(tcpRegistration.connection()));

        //shutdown the notification stream, should no longer receive any notifications
        killSwitch.shutdown();
        probe.expectNoMsg();
        //#subscribing
        locationService.unregisterAll();
    }
}
