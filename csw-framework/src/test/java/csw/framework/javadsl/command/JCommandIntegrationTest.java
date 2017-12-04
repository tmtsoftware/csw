package csw.framework.javadsl.command;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.typed.ActorRef;
import akka.typed.javadsl.Adapter;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.javadsl.TestProbe;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import csw.common.components.command.ComponentStateForCommand;
import csw.framework.internal.wiring.FrameworkWiring;
import csw.framework.internal.wiring.Standalone;
import csw.messages.SupervisorExternalMessage;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.CommandResponse.Completed;
import csw.messages.ccs.commands.Setup;
import csw.messages.framework.SupervisorLifecycleState;
import csw.messages.location.AkkaLocation;
import csw.messages.location.ComponentId;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.states.CurrentState;
import csw.messages.params.states.DemandState;
import csw.services.ccs.internal.matchers.DemandMatcher;
import csw.services.ccs.internal.matchers.Matcher;
import csw.services.ccs.internal.matchers.MatcherResponse;
import csw.services.ccs.internal.matchers.MatcherResponses;
import csw.services.ccs.javadsl.CommandExecutionService;
import csw.services.location.commons.ClusterAwareSettings;
import csw.services.location.javadsl.ILocationService;
import csw.services.location.javadsl.JLocationServiceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
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
    private ExecutionContext ec = hcdActorSystem.dispatcher();
    private Materializer mat = ActorMaterializer.create(hcdActorSystem);

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


        Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

        // long running command which does not use matcher
        Key<Integer> encoder = JKeyTypes.IntKey().make("encoder");
        Parameter<Integer> parameter = encoder.set(22, 23);
        Setup controlCommand = new Setup(ComponentStateForCommand.success().prefix()).add(parameter);

        CompletableFuture<CommandResponse> commandResponseCompletableFuture = CommandExecutionService
                .submit(hcd, controlCommand, timeout, hcdActorSystem.scheduler());

        CompletableFuture<CommandResponse> testCommandResponse = commandResponseCompletableFuture.thenCompose(commandResponse -> {
            if (commandResponse.getClass().equals(CommandResponse.Accepted.class))
                return CommandExecutionService.getCommandResponse(hcd, commandResponse.runId(), timeout, hcdActorSystem.scheduler());
            else
                return CompletableFuture.completedFuture(new CommandResponse.Error(commandResponse.runId(), "test error"));
        });

        // long running command which uses matcher
        Parameter<Integer> param = JKeyTypes.IntKey().make("encoder").set(100);
        DemandMatcher demandMatcher = new DemandMatcher(new DemandState(ComponentStateForCommand.acceptWithMatcherCmdPrefix().prefix()).add(param), false, timeout);
        Setup setup = new Setup(ComponentStateForCommand.acceptWithMatcherCmdPrefix().prefix()).add(parameter);
        Matcher matcher = new Matcher(hcd.narrow(), demandMatcher, ec, mat);

        CompletableFuture<MatcherResponse> matcherResponseFuture = matcher.jStart();

        CompletableFuture<CommandResponse> commandResponseToBeMatched = CommandExecutionService
                .submit(hcd, setup, timeout, hcdActorSystem.scheduler())
                .thenCompose(initialCommandResponse -> {
                    if (initialCommandResponse.getClass().equals(CommandResponse.Accepted.class)) {
                        return matcherResponseFuture.thenApply(matcherResponse -> {
                            if (matcherResponse.getClass().isAssignableFrom(MatcherResponses.jMatchCompleted().getClass()))
                                return new Completed(initialCommandResponse.runId());
                            else
                                return new CommandResponse.Error(initialCommandResponse.runId(), "Match not completed");
                        });
                    } else {
                        matcher.stop();
                        return CompletableFuture.completedFuture(initialCommandResponse);
                    }
                });

        Assert.assertEquals(Completed.class, commandResponseToBeMatched.get().getClass());
    }

}
