package csw.services.location.javadsl.docexamples;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import csw.services.location.javadsl.ILocationService;
import csw.services.location.javadsl.JComponentType;
import csw.services.location.javadsl.JLocationServiceFactory;
import csw.services.location.models.*;
import csw.services.location.models.Connection.AkkaConnection;
import csw.services.location.models.Connection.HttpConnection;
import csw.services.location.models.Connection.TcpConnection;
import csw.services.location.scaladsl.ActorRuntime;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class JLocationServiceImplDocExamplesTest {

    @Test
    public void testRegistrationAndUnregistrationOfHttpComponent() throws ExecutionException, InterruptedException {
        //#register_http_connection
        ActorRuntime actorRuntime = new ActorRuntime();
        ILocationService locationService = JLocationServiceFactory.make(actorRuntime);

        //To register a http endpoint on host http://10.1.2.22:8080/path/to/resource
        ComponentId httpServiceComponentId = new ComponentId("exampleHTTPService", JComponentType.Service);
        HttpConnection httpServiceConnection = new Connection.HttpConnection(httpServiceComponentId);
        String Path = "/path/to/resource";
        int port = 8080;

        HttpRegistration httpLocation = new HttpRegistration(httpServiceConnection, port, Path);

        CompletionStage<RegistrationResult> completionStage = locationService.register(httpLocation);
        CompletableFuture<RegistrationResult> completableFuture = completionStage.toCompletableFuture();

        RegistrationResult registrationResult = completableFuture.get();
        //#register_http_connection

        Assert.assertEquals(httpServiceComponentId, registrationResult.componentId());
        locationService.unregister(httpServiceConnection).toCompletableFuture().get();
        Assert.assertEquals(Collections.emptyList(), locationService.list().toCompletableFuture().get());

        locationService.unregisterAll().toCompletableFuture().get();
        actorRuntime.terminate();
    }

    @Test
    public void testResolveTcpConnection() throws ExecutionException, InterruptedException {
        //#register_tcp_connection
        ActorRuntime actorRuntime = new ActorRuntime();
        ILocationService locationService = JLocationServiceFactory.make(actorRuntime);

        //To register a tcp endpoint on host tcp://10.1.2.22:1234
        ComponentId tcpServiceComponentId = new ComponentId("exampleTcpService", JComponentType.Service);
        TcpConnection tcpServiceConnection = new Connection.TcpConnection(tcpServiceComponentId);
        int port = 1234;

        TcpRegistration tcpLocation = new TcpRegistration(tcpServiceConnection, port);

        CompletionStage<RegistrationResult> completionStage = locationService.register(tcpLocation);
        CompletableFuture<RegistrationResult> completableFuture = completionStage.toCompletableFuture();

        RegistrationResult registrationResult = completableFuture.get();
        //#register_tcp_connection

        Assert.assertEquals(tcpLocation.location(actorRuntime.hostname()), locationService.resolve(tcpServiceConnection).toCompletableFuture().get().get());

        locationService.unregisterAll().toCompletableFuture().get();
        actorRuntime.terminate();
    }

    @Test
    public void testResolveAkkaConnection() throws ExecutionException, InterruptedException {
        //#register_akka_connection
        ActorRuntime actorRuntime = new ActorRuntime();
        ILocationService locationService = JLocationServiceFactory.make(actorRuntime);

        ComponentId akkaHcdComponentId = new ComponentId("tromboneHcd", JComponentType.HCD);
        AkkaConnection akkaHcdConnection = new AkkaConnection(akkaHcdComponentId);

        //Create an actor ref using Props for your actor
        ActorRef actorRef = actorRuntime.actorSystem().actorOf(Props.create(MyActor.class), "Test-System");

        AkkaRegistration akkaLocation = new AkkaRegistration(akkaHcdConnection, actorRef);

        CompletionStage<RegistrationResult> completionStage = locationService.register(akkaLocation);
        CompletableFuture<RegistrationResult> completableFuture = completionStage.toCompletableFuture();

        RegistrationResult registrationResult = completableFuture.get();
        //#register_akka_connection

        Assert.assertEquals(akkaLocation.location(actorRuntime.hostname()), locationService.resolve(akkaHcdConnection).toCompletableFuture().get().get());

        locationService.unregisterAll().toCompletableFuture().get();
        actorRuntime.terminate();
    }
}

class MyActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().build();
    }
}
