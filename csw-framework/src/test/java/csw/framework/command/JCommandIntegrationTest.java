package csw.framework.command;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import csw.framework.internal.wiring.FrameworkWiring;
import csw.framework.internal.wiring.Standalone;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.CommandResponse.Completed;
import csw.messages.ccs.commands.JComponentRef;
import csw.messages.ccs.commands.Setup;
import csw.messages.location.AkkaLocation;
import csw.messages.location.ComponentId;
import csw.messages.models.CoordinatedShutdownReasons;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.states.DemandState;
import csw.services.ccs.internal.matchers.DemandMatcher;
import csw.services.ccs.internal.matchers.Matcher;
import csw.services.ccs.internal.matchers.MatcherResponse;
import csw.services.ccs.internal.matchers.MatcherResponses;
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

import static csw.common.components.command.ComponentStateForCommand.*;
import static csw.messages.location.Connection.AkkaConnection;
import static csw.services.location.javadsl.JComponentType.HCD;

// DEOPSCSW-217: Execute RPC like commands
// DEOPSCSW-224: Inter component command sending
// DEOPSCSW-225: Allow components to receive commands
// DEOPSCSW-228: Assist Components with command completion
// DEOPSCSW-317: Use state values of HCD to determine command completion
// DEOPSCSW-321: AkkaLocation provides wrapper for ActorRef[ComponentMessage]
public class JCommandIntegrationTest {
    private static ILocationService locationService = JLocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552));

    private static ActorSystem hcdActorSystem = ClusterAwareSettings.joinLocal(3552).system();

    private ExecutionContext ec = hcdActorSystem.dispatcher();
    private Materializer mat = ActorMaterializer.create(hcdActorSystem);

    @AfterClass
    public static void teardown() throws Exception {
        locationService.shutdown(CoordinatedShutdownReasons.testFinishedReason()).get();
        Await.result(hcdActorSystem.terminate(), Duration.create(20, "seconds"));
    }

    @Test
    public void testCommandExecutionBetweenComponents() throws Exception {
        FrameworkWiring wiring = FrameworkWiring.make(hcdActorSystem);
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
        Setup controlCommand = new Setup(prefix(), withoutMatcherCmd(), Optional.empty()).add(parameter);

        JComponentRef hcdComponent = maybeLocation.get().jComponent();
        CompletableFuture<CommandResponse> commandResponseCompletableFuture = hcdComponent.submit(controlCommand, timeout, hcdActorSystem.scheduler());

        CompletableFuture<CommandResponse> testCommandResponse = commandResponseCompletableFuture.thenCompose(commandResponse -> {
            if (commandResponse instanceof CommandResponse.Accepted)
                return hcdComponent.getCommandResponse(commandResponse.runId(), timeout, hcdActorSystem.scheduler());
            else
                return CompletableFuture.completedFuture(new CommandResponse.Error(commandResponse.runId(), "test error"));
        });

        Completed expectedCmdResponse = new Completed(controlCommand.runId());
        CommandResponse actualCmdResponse = testCommandResponse.get();
        Assert.assertEquals(expectedCmdResponse, actualCmdResponse);

        // DEOPSCSW-229: Provide matchers infrastructure for comparison
        // long running command which uses matcher
        Parameter<Integer> param = JKeyTypes.IntKey().make("encoder").set(100);
        DemandMatcher demandMatcher = new DemandMatcher(new DemandState(prefix().prefix()).add(param), false, timeout);
        Setup setup = new Setup(prefix(), matcherCmd(), Optional.empty()).add(parameter);
        Matcher matcher = new Matcher(hcdComponent.value().narrow(), demandMatcher, ec, mat);

        CompletableFuture<MatcherResponse> matcherResponseFuture = matcher.jStart();

        CompletableFuture<CommandResponse> commandResponseToBeMatched = hcdComponent
                .submit(setup, timeout, hcdActorSystem.scheduler())
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
