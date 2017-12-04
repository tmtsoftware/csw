package csw.framework.javadsl.command;

import akka.actor.ActorSystem;
import akka.typed.ActorRef;
import akka.typed.javadsl.Adapter;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.javadsl.TestProbe;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import csw.framework.internal.wiring.FrameworkWiring;
import csw.framework.internal.wiring.Standalone;
import csw.messages.SupervisorExternalMessage;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.Setup;
import csw.messages.framework.SupervisorLifecycleState;
import csw.messages.location.AkkaLocation;
import csw.messages.location.ComponentId;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.ObsId;
import csw.messages.params.states.CurrentState;
import csw.services.ccs.javadsl.CommandExecutionService;
import csw.services.location.commons.ClusterAwareSettings;
import csw.services.location.javadsl.ILocationService;
import csw.services.location.javadsl.JLocationServiceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static csw.messages.location.Connection.AkkaConnection;
import static csw.services.location.javadsl.JComponentType.HCD;

public class JCommandIntegrationTest {
    private static ILocationService locationService = JLocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552));

    private static ActorSystem hcdActorSystem = ClusterAwareSettings.joinLocal(3552, new scala.collection.mutable.ArrayBuffer()).system();
    private akka.typed.ActorSystem typedHcdActorSystem = Adapter.toTyped(hcdActorSystem);

    private final TestKitSettings testKitSettings = TestKitSettings.apply(typedHcdActorSystem);

    @AfterClass
    public static void teardown() throws Exception {
        Await.result(hcdActorSystem.terminate(), Duration.create(20, "seconds"));
        locationService.shutdown().get();
    }

    @Test
    public void testCommandExecutionBetweenComponents() throws Exception {
        FrameworkWiring wiring = FrameworkWiring.make(hcdActorSystem);
        ActorRef<SupervisorExternalMessage> hcd =
                Await.result(Standalone.spawn(ConfigFactory.load("mcs_hcd_java.conf"), wiring),
                        new FiniteDuration(5, TimeUnit.SECONDS));

        TestProbe supervisorLifecycleStateProbe = new TestProbe<SupervisorLifecycleState>("supervisor-lifecycle-state-probe", typedHcdActorSystem, testKitSettings);
        TestProbe supervisorStateProbe = new TestProbe<CurrentState>("supervisor-state-probe", typedHcdActorSystem, testKitSettings);

        AkkaConnection akkaConnection = new AkkaConnection(new ComponentId("Test_Component_Running_Long_Command_Java", HCD));
        CompletableFuture<Optional<AkkaLocation>> eventualLocation = locationService.resolve(akkaConnection, new FiniteDuration(5, TimeUnit.SECONDS));
        Optional<AkkaLocation> maybeLocation = eventualLocation.get();

        Assert.assertTrue(maybeLocation.isPresent());

        Key<Integer> encoder = JKeyTypes.IntKey().make("encoder");
        Parameter<Integer> parameter = encoder.set(22, 23);

        Setup controlCommand = new Setup("success", new ObsId("")).add(parameter);

        Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
        CompletableFuture<CommandResponse> commandResponseCompletableFuture = CommandExecutionService
                .submit(hcd, controlCommand, timeout, hcdActorSystem.scheduler());

        CompletableFuture<CommandResponse> testCommandResponse = commandResponseCompletableFuture.thenCompose(commandResponse -> {
            if (commandResponse.getClass().equals(CommandResponse.Accepted.class))
                return CommandExecutionService.getCommandResponse(hcd, commandResponse.runId(), timeout, hcdActorSystem.scheduler());
            else
                return CompletableFuture.completedFuture(new CommandResponse.Error(commandResponse.runId(), "test error"));
        });

        Assert.assertEquals(CommandResponse.Completed.class, testCommandResponse.get().getClass());
    }

}
