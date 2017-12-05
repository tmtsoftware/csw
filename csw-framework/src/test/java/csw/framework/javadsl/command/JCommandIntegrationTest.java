package csw.framework.javadsl.command;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.typed.ActorRef;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import csw.common.components.command.ComponentStateForCommand;
import csw.framework.internal.wiring.FrameworkWiring;
import csw.framework.internal.wiring.Standalone;
import csw.messages.SupervisorExternalMessage;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.CommandResponse.Completed;
import csw.messages.ccs.commands.Setup;
import csw.messages.location.AkkaLocation;
import csw.messages.location.ComponentId;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
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

    private static ActorSystem hcdActorSystem = ClusterAwareSettings.joinLocal(3552).system();

    private ExecutionContext ec = hcdActorSystem.dispatcher();
    private Materializer mat = ActorMaterializer.create(hcdActorSystem);

    @AfterClass
    public static void teardown() throws Exception {
        locationService.shutdown().get();
        Await.result(hcdActorSystem.terminate(), Duration.create(20, "seconds"));
    }

    @Test
    public void testCommandExecutionBetweenComponents() throws Exception {
        FrameworkWiring wiring = FrameworkWiring.make(hcdActorSystem);
        ActorRef<SupervisorExternalMessage> hcdRef =
                Await.result(Standalone.spawn(ConfigFactory.load("mcs_hcd_java.conf"), wiring),
                        new FiniteDuration(5, TimeUnit.SECONDS));

        AkkaConnection akkaConnection = new AkkaConnection(new ComponentId("Test_Component_Running_Long_Command_Java", HCD));
        CompletableFuture<Optional<AkkaLocation>> eventualLocation = locationService.resolve(akkaConnection, new FiniteDuration(5, TimeUnit.SECONDS));
        Optional<AkkaLocation> maybeLocation = eventualLocation.get();
        Assert.assertTrue(maybeLocation.isPresent());

        Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

        // long running command which does not use matcher
        Key<Integer> encoder = JKeyTypes.IntKey().make("encoder");
        Parameter<Integer> parameter = encoder.set(22, 23);
        Setup controlCommand = new Setup(ComponentStateForCommand.acceptWithNoMatcherCmdPrefix().prefix(), Optional.empty()).add(parameter);

        CompletableFuture<CommandResponse> commandResponseCompletableFuture = CommandExecutionService
                .submit(hcdRef, controlCommand, timeout, hcdActorSystem.scheduler());

        CompletableFuture<CommandResponse> testCommandResponse = commandResponseCompletableFuture.thenCompose(commandResponse -> {
            if (commandResponse instanceof CommandResponse.Accepted)
                return CommandExecutionService.getCommandResponse(hcdRef, commandResponse.runId(), timeout, hcdActorSystem.scheduler());
            else
                return CompletableFuture.completedFuture(new CommandResponse.Error(commandResponse.runId(), "test error"));
        });

        Completed expectedCmdResponse = new Completed(controlCommand.runId());
        CommandResponse actualCmdResponse = testCommandResponse.get();
        Assert.assertEquals(expectedCmdResponse, actualCmdResponse);

        // long running command which uses matcher
        Parameter<Integer> param = JKeyTypes.IntKey().make("encoder").set(100);
        DemandMatcher demandMatcher = new DemandMatcher(new DemandState(ComponentStateForCommand.acceptWithMatcherCmdPrefix().prefix()).add(param), false, timeout);
        Setup setup = new Setup(ComponentStateForCommand.acceptWithMatcherCmdPrefix().prefix(), Optional.empty()).add(parameter);
        Matcher matcher = new Matcher(hcdRef.narrow(), demandMatcher, ec, mat);

        CompletableFuture<MatcherResponse> matcherResponseFuture = matcher.jStart();

        CompletableFuture<CommandResponse> commandResponseToBeMatched = CommandExecutionService
                .submit(hcdRef, setup, timeout, hcdActorSystem.scheduler())
                .thenCompose(initialCommandResponse -> {
                    if (initialCommandResponse instanceof CommandResponse.Accepted) {
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

        Completed expectedResponse = new Completed(setup.runId());
        CommandResponse actualResponse = commandResponseToBeMatched.get();
        Assert.assertEquals(expectedResponse, actualResponse);
    }

}
