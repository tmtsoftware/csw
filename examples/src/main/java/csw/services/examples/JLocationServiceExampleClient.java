package csw.services.examples;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import csw.services.location.javadsl.*;
import csw.services.location.models.*;
import csw.services.location.scaladsl.ActorSystemFactory;
import csw.services.location.models.Connection.AkkaConnection;
import csw.services.location.models.Connection.HttpConnection;
import csw.services.location.scaladsl.LocationService;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static csw.services.examples.LocationServiceExampleComponent.*;

/**
 * An example location service client application.
 */
public class JLocationServiceExampleClient extends AbstractActor {
    //#create-location-service
    private ILocationService locationService = JLocationServiceFactory.make();
    //#create-location-service



    private Connection exampleConnection = connection();

    private IRegistrationResult httpRegResult;
    private IRegistrationResult hcdRegResult;
    private IRegistrationResult assemblyRegResult;


    public JLocationServiceExampleClient() throws ExecutionException, InterruptedException {

        // EXAMPLE DEMO START

        // This demo shows some basics of using the location service.  Before running this example,
        // optionally use the location service csw-location-agent to start a redis server:
        //
        //   $ csw-location-agent --name redis --command "redis-server --port %port"
        //
        // Not only does this serve as an example for starting applications that are not written in CSW,
        // but it also will help demonstrate location filtering later in this demo.


        registerConnectionsBlocking();
        findAndResolveConnectionsBlocking();
        listingAndFilteringBlocking();
        trackingAndSubscribingBlocking();
    }

    private class AllDone {}

    private void registerConnectionsBlocking() throws ExecutionException, InterruptedException {
        //#Components-Connections-Registrations
        // dummy http connection
        HttpConnection httpConnection   = new HttpConnection(new ComponentId("configuration", JComponentType.Service));
        HttpRegistration httpRegistration = new HttpRegistration(httpConnection, 8080, "path123");
        httpRegResult = locationService.register(httpRegistration).get();

        // dummy HCD connection
        AkkaConnection hcdConnection = new AkkaConnection(new ComponentId("hcd1", JComponentType.HCD));
        AkkaRegistration hcdRegistration = new AkkaRegistration(hcdConnection, getContext().actorOf(Props.create(AbstractActor.class, () -> new AbstractActor() {
                    @Override
                    public Receive createReceive() {
                        return ReceiveBuilder.create().build();
                    }
                }),
                "my-actor-1"
        ));
        hcdRegResult = locationService.register(hcdRegistration).get();

        //. register the client "assembly" created in this example
        AkkaConnection assemblyConnection = new AkkaConnection(new ComponentId("assembly1", JComponentType.Assembly));
        AkkaRegistration assemblyRegistration = new AkkaRegistration(assemblyConnection, getSelf());
        assemblyRegResult = locationService.register(assemblyRegistration).get();
        //#Components-Connections-Registrations
    }

    private void findAndResolveConnectionsBlocking() throws ExecutionException, InterruptedException {

        //#find-resolve
        // find connection to LocationServiceExampleComponent in location service
        // [do this before starting LocationServiceExampleComponent.  this should return Future[None]]

        System.out.println("Attempting to find connection " + exampleConnection + "...");
        Optional<Location> findResult = locationService.find(exampleConnection).get();
        if (findResult.isPresent()) {
            System.out.println("Find result: " + connectionInfo(findResult.get().connection()));
        } else {
            System.out.println("Find result: None");
        }
        // Output should be:
        //    Attempting to find connection AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) ...
        //    Find result: None

        // resolve connection to LocationServiceExampleComponent
        // [start LocationServiceExampleComponent after this command but before timeout]
        FiniteDuration waitForResolveLimit = new FiniteDuration(30, TimeUnit.SECONDS);
        System.out.println("Attempting to resolve " + exampleConnection + " with a wait of " + waitForResolveLimit + "...");
        Optional<Location> resolveResult = locationService.resolve(exampleConnection, waitForResolveLimit).get();
        if (resolveResult.isPresent()) {
            System.out.println("Resolve result: " + connectionInfo(resolveResult.get().connection()));
        } else {
            System.out.println("Timeout waiting for location " + exampleConnection + " to resolve.");
        }

        // Output should be:
        //    Attempting to resolve AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) with a wait of 30 seconds ...

        // If you then start the LocationServiceExampleComponentApp,
        // Output should be:
        //    Resolve result: LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

        // If not,
        // Output should be:
        //    Timeout waiting for location AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) to resolve.
        //#find-resolve

        if (resolveResult.isPresent()) {
            Location loc = resolveResult.get();
            if (loc instanceof AkkaLocation) {
                ActorRef actorRef = ((AkkaLocation)loc).actorRef();
                actorRef.tell(LocationServiceExampleComponent.ClientMessage$.MODULE$, getSelf());
            }
        }
    }

    private void listingAndFilteringBlocking() throws ExecutionException, InterruptedException {
        //#filtering
        // list connections in location service
        List<Location> connectionList = locationService.list().get();
        System.out.println("All Registered Connections:");
        for (Location loc: connectionList) {
            System.out.println("--- " + connectionInfo(loc.connection()));
        }

        // Output should be:
        //    All Registered Connections:
        //    --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
        //    --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
        //    --- redis-service-tcp, component type=Service, connection type=TcpType
        //    --- configuration-service-http, component type=Service, connection type=HttpType
        //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType


        // filter connections based on connection type
        List<Location> componentList = locationService.list(JComponentType.Assembly).get();
        System.out.println("Registered Assemblies:");
        for (Location loc: componentList) {
            System.out.println("--- " + connectionInfo(loc.connection()));
        }

        // Output should be:
        //    Registered Assemblies:
        //    --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
        //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

        // filter connections based on component type
        List<Location> akkaList = locationService.list(JConnectionType.AkkaType).get();
        System.out.println("Registered Akka connections:");
        for (Location loc : akkaList) {
            System.out.println("--- " + connectionInfo(loc.connection()));
        }

        // Output should be:
        //    Registered Akka connections:
        //    --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
        //    --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
        //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

        //#filtering
    }

    private void trackingAndSubscribingBlocking() {
        //#tracking
        // the following two methods are examples of two ways to track a connection.
        // both are implemented but only one is really needed.

        // track connection to LocationServiceExampleComponent
        // Calls track method for example connection and forwards location messages to this actor
        Materializer mat = ActorMaterializer.create(getContext());
        System.out.println("Starting to track " + exampleConnection);
        locationService.track(exampleConnection).toMat(Sink.actorRef(getSelf(), AllDone.class), Keep.both()).run(mat);

        // subscribe to LocationServiceExampleComponent events
        System.out.println("Starting a subscription to " + exampleConnection);
        locationService.subscribe(exampleConnection, trackingEvent -> {
            System.out.println("subscription event");
            getSelf().tell(trackingEvent, ActorRef.noSender());
        });

        // [tracking shows component unregister and re-register]



        // Output should be:
        //    Starting to track AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
        //    Starting a subscription to AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
        //    subscription event
        //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
        //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

        // If you now stop the LocationServiceExampleComponentApp,
        // Output should be:
        //    subscription event
        //    Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
        //    Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))

        // If you start the LocationServiceExampleComponentApp again,
        // Output should be:
        //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
        //    subscription event
        //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

        //#tracking

    }

    private String connectionInfo(Connection connection) {
        // construct string with useful information about a connection
        return connection.name()+", component type="+connection.componentId().componentType()+", connection type="+connection.connectionType();
    }

    @Override
    public void postStop() throws ExecutionException, InterruptedException {
        //#unregister
        httpRegResult.unregister();
        hcdRegResult.unregister();
        assemblyRegResult.unregister();
        //#unregister

        //#shutdown
        locationService.shutdown().get();
        //#shutdown
    }

    @Override
    public Receive createReceive() {
        // message handler for this actor
        return receiveBuilder()
                .match(LocationUpdated.class, l -> System.out.println("Location updated " + connectionInfo(l.connection())))
                .match(LocationRemoved.class, l -> System.out.println("Location removed " + l.connection()))
                .match(AllDone.class, x -> System.out.println("Tracking of " + exampleConnection + " complete."))
                .matchAny(x -> System.out.println("Unexpected message: "+ x))
                .build();
    }

    public static void main(String[] args) {
        //#create-actor-system
        ActorSystem actorSystem = ActorSystemFactory.remote("csw-examples-locationServiceClient");
        //#create-actor-system
        actorSystem.actorOf(Props.create(JLocationServiceExampleClient.class), "LocationServiceExampleClient");

    }


}
