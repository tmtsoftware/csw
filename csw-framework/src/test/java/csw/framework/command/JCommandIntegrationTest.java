package csw.framework.command;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.typed.internal.adapter.ActorSystemAdapter;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.javadsl.TestProbe;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import csw.framework.internal.wiring.FrameworkWiring;
import csw.framework.internal.wiring.Standalone;
import csw.messages.SupervisorLockMessage;
import csw.messages.ccs.CommandIssue;
import csw.messages.ccs.commands.*;
import csw.messages.ccs.commands.CommandResponse.Completed;
import csw.messages.ccs.commands.matchers.Matcher;
import csw.messages.ccs.commands.matchers.MatcherResponse;
import csw.messages.ccs.commands.matchers.MatcherResponses;
import csw.messages.location.AkkaLocation;
import csw.messages.location.ComponentId;
import csw.messages.models.CoordinatedShutdownReasons;
import csw.messages.models.LockingResponse;
import csw.messages.models.LockingResponses;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.states.DemandState;
import csw.services.ccs.javadsl.JCommandDistributor;
import csw.services.location.commons.ClusterAwareSettings;
import csw.services.location.javadsl.ILocationService;
import csw.services.location.javadsl.JLocationServiceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static csw.common.components.command.ComponentStateForCommand.*;
import static csw.messages.location.Connection.AkkaConnection;
import static csw.services.location.javadsl.JComponentType.HCD;

// DEOPSCSW-217: Execute RPC like commands
// DEOPSCSW-224: Inter component command sending
// DEOPSCSW-225: Allow components to receive commands
// DEOPSCSW-227: Distribute commands to multiple destinations
// DEOPSCSW-228: Assist Components with command completion
// DEOPSCSW-317: Use state values of HCD to determine command completion
// DEOPSCSW-321: AkkaLocation provides wrapper for ActorRef[ComponentMessage]
public class JCommandIntegrationTest {
    private static ILocationService locationService = JLocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552));

    private static ActorSystem hcdActorSystem = ClusterAwareSettings.joinLocal(3552).system();

    private ExecutionContext ec = hcdActorSystem.dispatcher();
    private Materializer mat = ActorMaterializer.create(hcdActorSystem);
    private static JComponentRef hcdComponent;
    private Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

    @BeforeClass
    public static void setup() throws Exception {
        hcdComponent = startHcd();
    }

    @AfterClass
    public static void teardown() throws Exception {
        locationService.shutdown(CoordinatedShutdownReasons.testFinishedReason()).get();
        Await.result(hcdActorSystem.terminate(), Duration.create(20, "seconds"));
    }

    private static JComponentRef startHcd() throws Exception {
        FrameworkWiring wiring = FrameworkWiring.make(hcdActorSystem);
        Await.result(Standalone.spawn(ConfigFactory.load("mcs_hcd_java.conf"), wiring), new FiniteDuration(5, TimeUnit.SECONDS));

        AkkaConnection akkaConnection = new AkkaConnection(new ComponentId("Test_Component_Running_Long_Command_Java", HCD));
        CompletableFuture<Optional<AkkaLocation>> eventualLocation = locationService.resolve(akkaConnection, new FiniteDuration(5, TimeUnit.SECONDS));
        Optional<AkkaLocation> maybeLocation = eventualLocation.get();
        Assert.assertTrue(maybeLocation.isPresent());

        return maybeLocation.get().jComponent();
    }

    @Test
    public void testCommandExecutionBetweenComponents() throws Exception {

        // immediate response - CompletedWithResult
        Key<Integer> intKey1 = JKeyTypes.IntKey().make("encoder");
        Parameter<Integer> intParameter1 = intKey1.set(22, 23);
        Setup imdResCommand = new Setup(prefix(), immediateResCmd(), Optional.empty()).add(intParameter1);

        CompletableFuture<CommandResponse> imdResCmdResponseCompletableFuture = hcdComponent.submit(imdResCommand, timeout, hcdActorSystem.scheduler());
        CommandResponse actualImdCmdResponse = imdResCmdResponseCompletableFuture.get();
        Assert.assertTrue(actualImdCmdResponse instanceof CommandResponse.CompletedWithResult);

        // immediate response - Invalid
        Key<Integer> intKey2 = JKeyTypes.IntKey().make("encoder");
        Parameter<Integer> intParameter2 = intKey2.set(22, 23);
        Setup imdInvalidCommand = new Setup(prefix(), invalidCmd(), Optional.empty()).add(intParameter2);

        CompletableFuture<CommandResponse> imdInvalidCmdResponseCompletableFuture = hcdComponent.submit(imdInvalidCommand, timeout, hcdActorSystem.scheduler());
        CommandResponse actualImdInvalidCmdResponse = imdInvalidCmdResponseCompletableFuture.get();
        Assert.assertTrue(actualImdInvalidCmdResponse instanceof CommandResponse.Invalid);

        // long running command which does not use matcher
        Key<Integer> encoder = JKeyTypes.IntKey().make("encoder");
        Parameter<Integer> parameter = encoder.set(22, 23);
        Setup controlCommand = new Setup(prefix(), withoutMatcherCmd(), Optional.empty()).add(parameter);

        CompletableFuture<CommandResponse> commandResponseCompletableFuture = hcdComponent.submit(controlCommand, timeout, hcdActorSystem.scheduler());

        CompletableFuture<CommandResponse> testCommandResponse = commandResponseCompletableFuture.thenCompose(commandResponse -> {
            if (commandResponse instanceof CommandResponse.Accepted)
                return hcdComponent.subscribe(commandResponse.runId(), timeout, hcdActorSystem.scheduler());
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

    @Test
    public void testSupervisorLock() throws ExecutionException, InterruptedException {
        // Lock scenarios
        akka.typed.ActorSystem<?> typedSystem = ActorSystemAdapter.apply(hcdActorSystem);
        TestKitSettings settings = new TestKitSettings(hcdActorSystem.settings().config());
        TestProbe<LockingResponse> probe = new TestProbe<>(typedSystem, settings);
        FiniteDuration duration = new FiniteDuration(5, TimeUnit.SECONDS);

        // Lock component
        hcdComponent.value().tell(new SupervisorLockMessage.Lock(prefix(), probe.ref(), duration));
        probe.expectMsg(LockingResponses.lockAcquired());

        Key<Integer> intKey2 = JKeyTypes.IntKey().make("encoder");
        Parameter<Integer> intParameter2 = intKey2.set(22, 23);

        // Send command to locked component and verify that it is not allowed
        Setup imdSetupCommand = new Setup(invalidPrefix(), immediateCmd(), Optional.empty()).add(intParameter2);
        CompletableFuture<CommandResponse> lockedCmdResCompletableFuture = hcdComponent.submit(imdSetupCommand, timeout, typedSystem.scheduler());
        CommandResponse actualLockedCmdResponse = lockedCmdResCompletableFuture.get();

        String reason = "This component is locked by component " + prefix();
        CommandResponse.NotAllowed expectedLockedCmdResponse = new CommandResponse.NotAllowed(imdSetupCommand.runId(), new CommandIssue.ComponentLockedIssue(reason));
        Assert.assertEquals(expectedLockedCmdResponse, actualLockedCmdResponse);

        // Unlock component
        hcdComponent.value().tell(new SupervisorLockMessage.Unlock(prefix(), probe.ref()));
        probe.expectMsg(LockingResponses.lockReleased());

        CompletableFuture<CommandResponse> cmdAfterUnlockResCompletableFuture = hcdComponent.submit(imdSetupCommand, timeout, typedSystem.scheduler());
        CommandResponse actualCmdResponseAfterUnlock = cmdAfterUnlockResCompletableFuture.get();
        Assert.assertTrue(actualCmdResponseAfterUnlock instanceof CommandResponse.Completed);
    }

    @Test
    public void testCommandDistributor() throws ExecutionException, InterruptedException {
        Parameter<Integer> encoderParam = JKeyTypes.IntKey().make("encoder").set(22, 23);

        Setup setupHcd1 = new Setup(prefix(), shortRunning(), Optional.empty()).add(encoderParam);
        Setup setupHcd2 = new Setup(prefix(), mediumRunning(), Optional.empty()).add(encoderParam);

        HashMap<JComponentRef, Set<ControlCommand>> componentsToCommands = new HashMap<JComponentRef, Set<ControlCommand>>() {
            {
                put(hcdComponent, new HashSet<ControlCommand>(Arrays.asList(setupHcd1, setupHcd2)));
            }
        };

        CompletableFuture<CommandResponse> cmdValidationResponseF = new JCommandDistributor(componentsToCommands).
                aggregatedValidationResponse(timeout, hcdActorSystem.scheduler(),ec, mat);
        Assert.assertTrue(cmdValidationResponseF.get() instanceof CommandResponse.Accepted);

        CompletableFuture<CommandResponse> cmdCompletionResponseF = new JCommandDistributor(componentsToCommands).
                aggregatedCompletionResponse(timeout, hcdActorSystem.scheduler(),ec, mat);
        Assert.assertTrue(cmdCompletionResponseF.get() instanceof CommandResponse.Completed);
    }
}
