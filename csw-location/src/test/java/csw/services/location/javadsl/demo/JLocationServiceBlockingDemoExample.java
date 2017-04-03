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
import csw.services.location.commons.CswCluster;
import csw.services.location.internal.Networks;
import csw.services.location.javadsl.*;
import csw.services.location.models.*;
import csw.services.location.models.Connection.AkkaConnection;
import csw.services.location.models.Connection.HttpConnection;
import csw.services.location.models.Connection.TcpConnection;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import scala.sys.SystemProperties;
import scala.sys.SystemProperties$;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class JLocationServiceBlockingDemoExample {

    private ActorSystem actorSystem = ActorSystem.create("demo");
    private Materializer mat = ActorMaterializer.create(actorSystem);

    private ActorRef actorRef = actorSystem.actorOf(Props.create(AbstractActor.class, () -> new AbstractActor() {
                @Override
                public Receive createReceive() {
                    return ReceiveBuilder.create().build();
                }
            }),
            "my-actor-1"
    );

    //static instance is used in testing for reuse and to avoid creating/terminating it for each test.
    private static
    //#create-location-service
    ILocationService locationService = JLocationServiceFactory.make();
    //#create-location-service

    @AfterClass
    public static void shutdown() throws ExecutionException, InterruptedException {
        //#shutdown
        locationService.shutdown().toCompletableFuture().get();
        //#shutdown
    }

    //#Components-Connections-Registrations
    private TcpConnection tcpConnection = new Connection.TcpConnection(new ComponentId("redis", JComponentType.Service));
    private TcpRegistration tcpRegistration = new TcpRegistration(tcpConnection, 6380);

    private HttpConnection httpConnection = new Connection.HttpConnection(new ComponentId("configuration", JComponentType.Service));
    private HttpRegistration httpRegistration = new HttpRegistration(httpConnection, 8080, "path123");

    private AkkaConnection akkaConnection = new Connection.AkkaConnection(new ComponentId("hcd1", JComponentType.HCD));
    private AkkaRegistration akkaRegistration = new AkkaRegistration(akkaConnection, actorRef);
    //#Components-Connections-Registrations

    @Test
    public void filtering() throws ExecutionException, InterruptedException {
        //#filtering
        IRegistrationResult tcpRegistrationResult = locationService.register(tcpRegistration).toCompletableFuture().get();
        IRegistrationResult httpRegistrationResult = locationService.register(httpRegistration).toCompletableFuture().get();
        IRegistrationResult akkaRegistrationResult = locationService.register(akkaRegistration).toCompletableFuture().get();

        //expected list of locations
        ArrayList<Location> expectedLocations1 = new ArrayList<>();
        expectedLocations1.add(tcpRegistration.location(new Networks().hostname()));
        expectedLocations1.add(httpRegistration.location(new Networks().hostname()));
        expectedLocations1.add(akkaRegistration.location(new Networks().hostname()));
        Assert.assertEquals(expectedLocations1, locationService.list().toCompletableFuture().get());

        //Filter by type
        ArrayList<Location> expectedLocations2 = new ArrayList<>();
        expectedLocations2.add(akkaRegistration.location(new Networks().hostname()));
        Assert.assertEquals(expectedLocations2, locationService.list(JConnectionType.AkkaType).toCompletableFuture().get());

        //Filter by service
        ArrayList<Location> expectedLocations3 = new ArrayList<>();
        expectedLocations3.add(tcpRegistration.location(new Networks().hostname()));
        expectedLocations3.add(httpRegistration.location(new Networks().hostname()));
        Assert.assertEquals(expectedLocations3, locationService.list(JComponentType.Service).toCompletableFuture().get());

        //Filter by hostname
        ArrayList<Location> expectedLocations4 = new ArrayList<>();
        expectedLocations4.add(tcpRegistration.location(new Networks().hostname()));
        expectedLocations4.add(httpRegistration.location(new Networks().hostname()));
        Assert.assertEquals(expectedLocations4, locationService.list(new Networks().hostname()).toCompletableFuture().get());
        //#filtering

        tcpRegistrationResult.unregister().toCompletableFuture().get();
        httpRegistrationResult.unregister().toCompletableFuture().get();
        akkaRegistrationResult.unregister().toCompletableFuture().get();
    }

    @Test
    public void demo() throws ExecutionException, InterruptedException {
        //#register-list-resolve-unregister
        IRegistrationResult tcpRegistrationResult = locationService.register(tcpRegistration).toCompletableFuture().get();

        Assert.assertEquals(tcpRegistration.connection(), tcpRegistrationResult.location().connection());

        //expected list of locations
        ArrayList<Location> expectedLocations = new ArrayList<>();
        expectedLocations.add(tcpRegistration.location(new Networks().hostname()));

        Assert.assertEquals(expectedLocations, locationService.list().toCompletableFuture().get());
        Assert.assertEquals(tcpRegistration.location(new Networks().hostname()), locationService.resolve(tcpConnection).toCompletableFuture().get().get());

        System.out.println(tcpRegistrationResult.location().uri());

        tcpRegistrationResult.unregister().toCompletableFuture().get();

        Assert.assertEquals(Collections.EMPTY_LIST, locationService.list().toCompletableFuture().get());
        Assert.assertEquals(Optional.empty(), locationService.resolve(tcpConnection).toCompletableFuture().get());
        //#register-list-resolve-unregister
    }

    @Test
    public void tracking() throws ExecutionException, InterruptedException {
        //#tracking
        Pair<KillSwitch, CompletionStage<Done>> stream = locationService.track(tcpConnection).toMat(Sink.foreach(System.out::println), Keep.both()).run(mat);

        Thread.sleep(200);

        IRegistrationResult tcpRegistrationResult = locationService.register(tcpRegistration).toCompletableFuture().get();
        IRegistrationResult httpRegistrationResult = locationService.register(httpRegistration).toCompletableFuture().get();

        Thread.sleep(200);

        tcpRegistrationResult.unregister().toCompletableFuture().get();
        locationService.unregister(httpConnection).toCompletableFuture().get();

        Thread.sleep(200);
        stream.first().shutdown();
        //#tracking
    }
}

