package csw.framework.command;

import akka.actor.testkit.typed.javadsl.TestInbox;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import akka.stream.Materializer;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import csw.command.api.CurrentStateSubscription;
import csw.command.api.StateMatcher;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandServiceFactory;
import csw.command.client.extensions.AkkaLocationExt;
import csw.command.client.messages.SupervisorLockMessage;
import csw.command.client.models.framework.LockingResponse;
import csw.command.client.models.matchers.DemandMatcher;
import csw.command.client.models.matchers.Matcher;
import csw.command.client.models.matchers.MatcherResponse;
import csw.command.client.models.matchers.MatcherResponses;
import csw.common.components.framework.SampleComponentState;
import csw.framework.internal.wiring.FrameworkWiring;
import csw.framework.internal.wiring.Standalone;
import csw.location.api.javadsl.ILocationService;
import csw.location.client.ActorSystemFactory;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.location.models.AkkaLocation;
import csw.location.models.ComponentId;
import csw.location.server.http.JHTTPLocationService;
import csw.params.commands.CommandIssue;
import csw.params.commands.CommandResponse;
import csw.params.commands.Result;
import csw.params.commands.Setup;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Id;
import csw.params.core.states.CurrentState;
import csw.params.core.states.DemandState;
import csw.params.core.states.StateName;
import csw.params.javadsl.JKeyType;
import io.lettuce.core.RedisClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static csw.common.components.command.ComponentStateForCommand.*;
import csw.location.api.javadsl.JComponentType;
import static csw.location.api.javadsl.JComponentType.HCD;
import static csw.location.models.Connection.AkkaConnection;

// DEOPSCSW-212: Send oneway command
// DEOPSCSW-217: Execute RPC like commands
// DEOPSCSW-224: Inter component command sending
// DEOPSCSW-225: Allow components to receive commands
// DEOPSCSW-227: Distribute commands to multiple destinations
// DEOPSCSW-228: Assist Components with command completion
// DEOPSCSW-234: CCS accessibility to all CSW component builders
// DEOPSCSW-317: Use state values of HCD to determine command completion
// DEOPSCSW-321: AkkaLocation provides wrapper for ActorRef[ComponentMessage]
public class JCommandIntegrationTest extends JUnitSuite {
    private static ActorSystem<SpawnProtocol.Command> hcdActorSystem = ActorSystemFactory.remote(SpawnProtocol.create(), "test");
    private ExecutionContext ec = hcdActorSystem.executionContext();
    private static Materializer mat = Materializer.createMaterializer(hcdActorSystem);

    private static JHTTPLocationService jHttpLocationService;
    private static ILocationService locationService;
    private static ICommandService hcdCmdService;
    private static AkkaLocation hcdLocation;
    private Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

    @BeforeClass
    public static void setup() throws Exception {
        jHttpLocationService = new JHTTPLocationService();
        jHttpLocationService.beforeAll();

        locationService = JHttpLocationServiceFactory.makeLocalClient(hcdActorSystem, mat);
        hcdLocation = getLocation();
        hcdCmdService = CommandServiceFactory.jMake(hcdLocation, hcdActorSystem);
    }

    @AfterClass
    public static void teardown() throws Exception {
        hcdActorSystem.terminate();
        Await.result(hcdActorSystem.whenTerminated(), Duration.create(20, "seconds"));
        jHttpLocationService.afterAll();
    }

    private static AkkaLocation getLocation() throws Exception {
        RedisClient redisClient = null;
        FrameworkWiring wiring = FrameworkWiring.make(hcdActorSystem, redisClient);
        Await.result(Standalone.spawn(ConfigFactory.load("mcs_hcd_java.conf"), wiring), new FiniteDuration(5, TimeUnit.SECONDS));

        AkkaConnection akkaConnection = new AkkaConnection(new ComponentId("Test_Component_Running_Long_Command_Java", JComponentType.HCD()));
        CompletableFuture<Optional<AkkaLocation>> eventualLocation = locationService.resolve(akkaConnection, java.time.Duration.ofSeconds(5));
        Optional<AkkaLocation> maybeLocation = eventualLocation.get();
        Assert.assertTrue(maybeLocation.isPresent());

        return maybeLocation.orElseThrow();
    }

    @Test
    public void testCommandExecutionBetweenComponents() throws Exception {

        // immediate response - Completed with a result
        Key<Integer> intKey1 = JKeyType.IntKey().make("encoder");
        Parameter<Integer> intParameter1 = intKey1.set(22, 23);
        Setup imdResCommand = new Setup(prefix(), immediateResCmd(), Optional.empty()).add(intParameter1);

        CompletableFuture<CommandResponse.SubmitResponse> imdResCmdResponseCompletableFuture = hcdCmdService.submitAndWait(imdResCommand, timeout);
        CommandResponse.SubmitResponse actualImdCmdResponse = imdResCmdResponseCompletableFuture.get();
        Assert.assertTrue(actualImdCmdResponse instanceof CommandResponse.Completed);

        Setup immediateCmd = new Setup(prefix(), immediateCmd(), Optional.empty()).add(intParameter1);
        //#immediate-response
        CompletableFuture<CommandResponse.SubmitResponse> immediateCommandF =
                hcdCmdService
                        .submitAndWait(immediateCmd, timeout)
                        .thenApply(
                                response -> {
                                    if (response instanceof CommandResponse.Completed) {
                                        //do something with completed result
                                    } else {
                                        // do something with unexpected response
                                    }
                                    return response;
                                }
                        );
        //#immediate-response
        Assert.assertTrue(immediateCommandF.get() instanceof CommandResponse.Completed);

        Key<Integer> intKey2 = JKeyType.IntKey().make("encoder");
        Parameter<Integer> intParameter2 = intKey2.set(22, 23);
        //#invalidCmd
        Setup invalidSetup = new Setup(prefix(), invalidCmd(), Optional.empty()).add(intParameter2);
        CompletableFuture<CommandResponse.SubmitResponse> invalidCommandF =
                hcdCmdService.submitAndWait(invalidSetup, timeout).thenApply(
                        response -> {
                            if (response instanceof CommandResponse.Completed) {
                                //do something with completed result
                            } else if (response instanceof CommandResponse.Invalid) {
                                // Cast the response to get the issue
                                CommandResponse.Invalid invalid = (CommandResponse.Invalid) response;
                                assert (invalid.issue().reason().contains("failure"));
                            }
                            return response;
                        }
                );
        //#invalidCmd
        //CommandResponse.Invalid expectedInvalidResponse = new CommandResponse.Invalid(_, new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."));
        //Assert.assert(expectedInvalidResponse, invalidCommandF.get());
        Assert.assertTrue(invalidCommandF.get() instanceof CommandResponse.Invalid);
        CommandResponse.Invalid invalidResponse = (CommandResponse.Invalid)invalidCommandF.get();
        Assert.assertEquals(new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."), invalidResponse.issue());

        CompletableFuture<CommandResponse.SubmitResponse> imdInvalidCmdResponseCompletableFuture = hcdCmdService.submitAndWait(invalidSetup, timeout);
        CommandResponse.SubmitResponse actualImdInvalidCmdResponse = imdInvalidCmdResponseCompletableFuture.get();
        Assert.assertTrue(actualImdInvalidCmdResponse instanceof CommandResponse.Invalid);

        // long running command which does not use matcher
        //#longRunning
        Setup longRunningSetup = new Setup(prefix(), longRunningCmd(), Optional.empty()).add(intParameter1);
        Key<Integer> encoder = JKeyType.IntKey().make("encoder");
        CompletableFuture<Optional<Integer>> longRunningResultF =
                hcdCmdService.submitAndWait(longRunningSetup, timeout)
                        .thenCompose(response -> {
                            if (response instanceof CommandResponse.Completed) {
                                // This extracts and returns the the first value of parameter encoder
                                Result result = ((CommandResponse.Completed) response).result();
                                Optional<Integer> rvalue = Optional.of(result.jGet(encoder).orElseThrow().head());
                                return CompletableFuture.completedFuture(rvalue);
                            } else {
                                // For some other response, return empty
                                return CompletableFuture.completedFuture(Optional.empty());
                            }

                        });
        //#longRunning
        Optional<Integer> expectedCmdResponse = Optional.of(20);
        Optional<Integer> actualCmdResponse = longRunningResultF.get();
        Assert.assertEquals(expectedCmdResponse, actualCmdResponse);

        //#queryLongRunning
        //Setup longRunningSetup2 = longRunningSetup1.cloneCommand();
        CompletableFuture<CommandResponse.SubmitResponse> longRunningCommandResultF =
                hcdCmdService.submitAndWait(longRunningSetup, timeout);

        // do some work before querying for the result of above command as needed
        CommandResponse.SubmitResponse sresponse = longRunningCommandResultF.get();
        CompletableFuture<CommandResponse.QueryResponse> queryResponseF = hcdCmdService.query(sresponse.runId(), timeout);
        queryResponseF.thenAccept(r -> {
            if (r instanceof CommandResponse.Started) {
                // happy case - no action needed
                // Do some other work
            } else {
                // log.error. This indicates that the command probably failed to start.
            }
        });

        CompletableFuture<Optional<Integer>> intF =
                longRunningCommandResultF.thenCompose(response -> {
            if (response instanceof CommandResponse.Completed) {
                // This extracts and returns the the first value of parameter encoder
                Result result = ((CommandResponse.Completed) response).result();
                Optional<Integer> rvalue = Optional.of(result.jGet(encoder).orElseThrow().head());
                return CompletableFuture.completedFuture(rvalue);
            } else {
                // For some other response, return empty
                return CompletableFuture.completedFuture(Optional.empty());
            }
        });
        Assert.assertEquals(Optional.of(20), intF.get());
        //#queryLongRunning

        //#queryFinal
        longRunningCommandResultF = hcdCmdService.submitAndWait(longRunningSetup, timeout);
        sresponse = longRunningCommandResultF.get();

        CompletableFuture<Optional<Integer>> int3F =
                hcdCmdService.queryFinal(sresponse.runId(), timeout).thenCompose(response -> {
                    if (response instanceof CommandResponse.Completed) {
                        // This extracts and returns the the first value of parameter encoder
                        Result result = ((CommandResponse.Completed) response).result();
                        Optional<Integer> rvalue = Optional.of(result.jGet(encoder).orElseThrow().head());
                        return CompletableFuture.completedFuture(rvalue);
                    } else {
                        // For some other response, return empty
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                });
        Assert.assertEquals(Optional.of(20), int3F.get());
        //#queryFinal

        //#oneway
        Setup onewaySetup = new Setup(prefix(), onewayCmd(), Optional.empty()).add(intParameter1);
        CompletableFuture onewayF = hcdCmdService
                .oneway(onewaySetup, timeout)
                .thenAccept(onewayResponse -> {
                    if (onewayResponse instanceof CommandResponse.Invalid) {
                        // log an error here
                    } else {
                        // Ignore anything other than invalid
                    }
                });
        //#oneway
        // just wait for command completion so that it ll not impact other results
        onewayF.get();

        //#validate
        CompletableFuture<Boolean> validateCommandF =
                hcdCmdService
                        .validate(immediateCmd)
                        .thenApply(
                                response -> {
                                    if (response instanceof CommandResponse.Accepted) {
                                        //do something with completed result
                                        return true;
                                    } else if (response instanceof CommandResponse.Invalid) {
                                        // do something with unexpected response
                                        return false;
                                    } else {
                                        // Locked
                                        return false;
                                    }
                                }
                        );
        Assert.assertTrue(validateCommandF.get());
        //#validate

        //#query
        CompletableFuture<CommandResponse.QueryResponse> queryResponseF2 = hcdCmdService.query(sresponse.runId(), timeout);
        Assert.assertTrue(queryResponseF2.get() instanceof CommandResponse.Completed);
        //#query

        Parameter<Integer> encoderParam = JKeyType.IntKey().make("encoder").set(22, 23);

        //#submitAll
        Setup submitAllSetup1 = new Setup(prefix(), immediateCmd(), Optional.empty()).add(encoderParam);
        Setup submitAllSetup2 = new Setup(prefix(), longRunningCmd(), Optional.empty()).add(encoderParam);
        Setup submitAllSetup3 = new Setup(prefix(), invalidCmd(), Optional.empty()).add(encoderParam);

        CompletableFuture<List<CommandResponse.SubmitResponse>> submitAllF = hcdCmdService
                .submitAllAndWait(
                        List.of(submitAllSetup1, submitAllSetup2, submitAllSetup3),
                        timeout
                );

        List<CommandResponse.SubmitResponse> submitAllResponse = submitAllF.get();
        Assert.assertEquals(submitAllResponse.size(), 3);
        Assert.assertTrue(submitAllResponse.get(0) instanceof CommandResponse.Completed);
        Assert.assertTrue(submitAllResponse.get(1) instanceof CommandResponse.Completed);
        Assert.assertTrue(submitAllResponse.get(2) instanceof CommandResponse.Invalid);
        //#submitAll

        //#submitAllInvalid
        CompletableFuture<List<CommandResponse.SubmitResponse>> submitAllF2 = hcdCmdService
                .submitAllAndWait(
                        List.of(submitAllSetup1, submitAllSetup3, submitAllSetup2),
                        timeout
                );

        List<CommandResponse.SubmitResponse> submitAllResponse2 = submitAllF2.get();
        Assert.assertEquals(submitAllResponse2.size(), 2);
        Assert.assertTrue(submitAllResponse2.get(0) instanceof CommandResponse.Completed);
        Assert.assertTrue(submitAllResponse2.get(1) instanceof CommandResponse.Invalid);
        //#submitAllInvalid

        //#subscribeCurrentState
        // Subscriber code
        int expectedEncoderValue = 234;
        Setup currStateSetup = new Setup(prefix(), hcdCurrentStateCmd(), Optional.empty()).add(encoder.set(expectedEncoderValue));
        // Setup a callback response to CurrentState - use AtomicInteger to capture final value
        final AtomicInteger cstate = new AtomicInteger((1));
        CurrentStateSubscription subscription = hcdCmdService.subscribeCurrentState(cs -> {
            // Example sets variable outside scope of closure
            cstate.set(cs.jGet(encoder).orElseThrow().head());
        });
        // Send a oneway to the HCD that will cause a publish of a CurrentState with the encoder value
        // in the command parameter "encoder"
        hcdCmdService.oneway(currStateSetup, timeout);

        // Wait for a bit for the callback
        Thread.sleep(200);
        // Check to see if CurrentState has the value we sent
        Assert.assertEquals(expectedEncoderValue, cstate.get());

        // Unsubscribe from CurrentState
        subscription.unsubscribe();
        //#subscribeCurrentState


        // DEOPSCSW-229: Provide matchers infrastructure for comparison
        // long running command which uses matcher
        //#matcher
        Parameter<Integer> param = JKeyType.IntKey().make("encoder").set(100);
        Setup setupWithMatcher = new Setup(prefix(), matcherCmd(), Optional.empty()).add(param);

        // create a StateMatcher which specifies the desired algorithm and state to be matched.
        DemandMatcher demandMatcher = new DemandMatcher(new DemandState(prefix(), new StateName("testStateName")).add(param), false, timeout);

        // create the matcher instance
        Matcher matcher = new Matcher(AkkaLocationExt.RichAkkaLocation(hcdLocation).componentRef(hcdActorSystem).narrow(), demandMatcher, ec, mat);

        // start the matcher so that it is ready to receive state published by the source
        CompletableFuture<MatcherResponse> matcherResponseFuture = matcher.jStart();

        // Submit command as a oneway and if the command is successfully validated,
        // check for matching of demand state against current state
        CompletableFuture<CommandResponse.MatchingResponse> matchResponseF = hcdCmdService
                .oneway(setupWithMatcher, timeout)
                .thenCompose(initialCommandResponse -> {
                    if (initialCommandResponse instanceof CommandResponse.Accepted) {
                        return matcherResponseFuture.thenApply(matcherResponse -> {
                            if (matcherResponse.getClass().isAssignableFrom(MatcherResponses.jMatchCompleted().getClass()))
                                return new CommandResponse.Completed(setupWithMatcher.commandName(), initialCommandResponse.runId());
                            else
                                return new CommandResponse.Error(setupWithMatcher.commandName(), initialCommandResponse.runId(), "Match not completed");
                        });

                    } else {
                        matcher.stop();
                        return CompletableFuture.completedFuture(new CommandResponse.Error(setupWithMatcher.commandName(), initialCommandResponse.runId(), "Matcher failed"));
                    }
                });

        CommandResponse.MatchingResponse actualResponse = matchResponseF.get();
        CommandResponse.Completed expectedResponse = new CommandResponse.Completed(setupWithMatcher.commandName(), actualResponse.runId());   // Not a great test for now
        Assert.assertEquals(expectedResponse, actualResponse);
        //#matcher

        //#onewayAndMatch
        // create a DemandMatcher which specifies the desired state to be matched.
        StateMatcher stateMatcher = new DemandMatcher(new DemandState(prefix(), new StateName("testStateName")).add(param), false, timeout);

        // create matcher instance
        //Matcher matcher1 = new Matcher(AkkaLocationExt.RichAkkaLocation(hcdLocation).componentRef().narrow(), demandMatcher, ec, mat);

        // start the matcher so that it is ready to receive state published by the source
       // CompletableFuture<MatcherResponse> matcherResponse = matcher1.jStart();

        CompletableFuture<CommandResponse.MatchingResponse> matchedCommandResponseF =
                hcdCmdService.onewayAndMatch(setupWithMatcher, stateMatcher, timeout);

        //#onewayAndMatch
        CommandResponse.MatchingResponse mresponse = matchedCommandResponseF.get();
        Assert.assertTrue(mresponse instanceof CommandResponse.MatchingResponse.Completed);
        //Assert.assertEquals(new CommandResponse.Completed(setupWithMatcher.runId()), matchedCommandResponse.get());
        //Assert.assertEquals(MatcherResponses.MatchCompleted$.MODULE$, matcherResponse.get());
    }

    @Test
    public void testSupervisorLock() throws ExecutionException, InterruptedException {
        // Lock scenarios
        TestProbe<LockingResponse> probe = TestProbe.create(hcdActorSystem);
        FiniteDuration duration = new FiniteDuration(5, TimeUnit.SECONDS);

        // Lock component
        AkkaLocationExt.RichAkkaLocation(hcdLocation).componentRef(hcdActorSystem).tell(new SupervisorLockMessage.Lock(prefix(), probe.ref(), duration));
        probe.expectMessage(LockingResponse.lockAcquired());

        Key<Integer> intKey2 = JKeyType.IntKey().make("encoder");
        Parameter<Integer> intParameter2 = intKey2.set(22, 23);

        // Send command to locked component and verify that it is not allowed
        Setup imdSetupCommand = new Setup(invalidPrefix(), immediateCmd(), Optional.empty()).add(intParameter2);
        CompletableFuture<CommandResponse.SubmitResponse> lockedCmdResCompletableFuture = hcdCmdService.submit(imdSetupCommand, timeout);
        CommandResponse.SubmitResponse actualLockedCmdResponse = lockedCmdResCompletableFuture.get();

        String reason = "This component is locked by component " + prefix();
        Assert.assertTrue(actualLockedCmdResponse instanceof CommandResponse.Locked);

        // Unlock component
        AkkaLocationExt.RichAkkaLocation(hcdLocation).componentRef(hcdActorSystem).tell(new SupervisorLockMessage.Unlock(prefix(), probe.ref()));
        probe.expectMessage(LockingResponse.lockReleased());

        CompletableFuture<CommandResponse.SubmitResponse> cmdAfterUnlockResCompletableFuture = hcdCmdService.submit(imdSetupCommand, timeout);
        CommandResponse.SubmitResponse actualCmdResponseAfterUnlock = cmdAfterUnlockResCompletableFuture.get();
        Assert.assertTrue(actualCmdResponseAfterUnlock instanceof CommandResponse.Completed);
    }

    // DEOPSCSW-208: Report failure on Configuration Completion command
    @Test
    public void testCommandFailure() throws ExecutionException, InterruptedException {
        // using single submitAndSubscribe API
        Key<Integer> intKey1 = JKeyType.IntKey().make("encoder");
        Parameter<Integer> intParameter1 = intKey1.set(22, 23);
        Setup failureResCommand = new Setup(prefix(), failureAfterValidationCmd(), Optional.empty()).add(intParameter1);

        //#submit
        CompletableFuture<CommandResponse.SubmitResponse> finalResponseCompletableFuture = hcdCmdService.submitAndWait(failureResCommand, timeout);
        CommandResponse.SubmitResponse actualValidationResponse = finalResponseCompletableFuture.get();
        //#submit

        Assert.assertTrue(actualValidationResponse instanceof CommandResponse.Error);

        // using separate submit and wait API
        CompletableFuture<CommandResponse.SubmitResponse> validationResponse = hcdCmdService.submit(failureResCommand, timeout);
        Id runId = validationResponse.get().runId();
        CompletableFuture<CommandResponse.QueryResponse> finalResponse = hcdCmdService.queryFinal(runId, timeout);
        Assert.assertTrue(finalResponse.get() instanceof CommandResponse.Error);  // This should fail but I guess not typed by compiler
    }

    @Test
    public void testSubmitAll() throws ExecutionException, InterruptedException {

        Parameter<Integer> encoderParam = JKeyType.IntKey().make("encoder").set(22, 23);

        Setup setupHcd1 = new Setup(prefix(), shortRunning(), Optional.empty()).add(encoderParam);
        Setup setupHcd2 = new Setup(prefix(), mediumRunning(), Optional.empty()).add(encoderParam);

        CompletableFuture<List<CommandResponse.SubmitResponse>> finalCommandResponse = hcdCmdService
                .submitAllAndWait(
                        List.of(setupHcd1, setupHcd2),
                        timeout
                );

        List<CommandResponse.SubmitResponse> actualSubmitResponses = finalCommandResponse.get();
        Assert.assertEquals(2, actualSubmitResponses.size());
        //List<CommandResponse.Completed> expectedResponses = List.of(new CommandResponse.Completed(setupHcd1.runId()), new CommandResponse.Completed(setupHcd2.runId()));
        //Assert.assertEquals(expectedResponses, actualSubmitResponses);
    }

    @Test
    public void testSubscribeCurrentState() throws InterruptedException {
        Key<Integer> intKey1 = JKeyType.IntKey().make("encoder");
        Parameter<Integer> intParameter1 = intKey1.set(22, 23);
        Setup setup = new Setup(prefix(), acceptedCmd(), Optional.empty()).add(intParameter1);

        TestProbe<CurrentState> probe = TestProbe.create(hcdActorSystem);

        // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
        //#subscribeCurrentState
        // subscribe to the current state of an assembly component and use a callback which forwards each received
        // element to a test probe actor
        CurrentStateSubscription subscription = hcdCmdService.subscribeCurrentState(currentState -> probe.ref().tell(currentState));
        //#subscribeCurrentState

        hcdCmdService.submit(setup, timeout);

        CurrentState currentState = new CurrentState(SampleComponentState.prefix(), new StateName("testStateName"));
        CurrentState expectedValidationCurrentState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.commandValidationChoice()));
        CurrentState expectedSubmitCurrentState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.submitCommandChoice()));
        CurrentState expectedSetupCurrentState = new CurrentState(SampleComponentState.prefix(), new StateName("testStateSetup")).madd(SampleComponentState.choiceKey().set(SampleComponentState.setupConfigChoice()), intParameter1);

        probe.expectMessage(expectedValidationCurrentState);
        probe.expectMessage(expectedSubmitCurrentState);
        probe.expectMessage(expectedSetupCurrentState);

        // unsubscribe and verify no messages received by probe
        subscription.unsubscribe();
        Thread.sleep(1000);

        hcdCmdService.submit(setup, timeout);
        probe.expectNoMessage(java.time.Duration.ofMillis(20));
    }

    @Test
    public void testSubscribeOnlyCurrentState() throws InterruptedException {
        Key<Integer> intKey1 = JKeyType.IntKey().make("encoder");
        Parameter<Integer> intParameter1 = intKey1.set(22, 23);
        Setup setup = new Setup(prefix(), acceptedCmd(), Optional.empty()).add(intParameter1);

        TestInbox<CurrentState> inbox = TestInbox.create();
        Thread.sleep(1000);

        // DEOPSCSW-434: Provide an API for PubSubActor that hides actor based interaction
        //#subscribeOnlyCurrentState
        // subscribe to the current state of an assembly component and use a callback which forwards each received
        // element to a test probe actor
        CurrentStateSubscription subscription = hcdCmdService.subscribeCurrentState(Set.of(StateName.apply("testStateSetup")), currentState -> inbox.getRef().tell(currentState));
        //#subscribeOnlyCurrentState

        hcdCmdService.submit(setup, timeout);

        CurrentState currentState = new CurrentState(SampleComponentState.prefix(), new StateName("testStateName"));
        CurrentState expectedValidationCurrentState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.commandValidationChoice()));
        CurrentState expectedSubmitCurrentState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.submitCommandChoice()));
        CurrentState expectedSetupCurrentState = new CurrentState(SampleComponentState.prefix(), new StateName("testStateSetup")).madd(SampleComponentState.choiceKey().set(SampleComponentState.setupConfigChoice()), intParameter1);

        Thread.sleep(1000);
        List<CurrentState> receivedStates = inbox.getAllReceived();

        Assert.assertFalse(receivedStates.contains(expectedValidationCurrentState));
        Assert.assertFalse(receivedStates.contains(expectedSubmitCurrentState));
        Assert.assertTrue(receivedStates.contains(expectedSetupCurrentState));

        subscription.unsubscribe();
    }

    @Test
    public void testCRMUsageForCompletion() throws Exception {
        Setup crmAddSetup = new Setup(prefix(), crmAddOrUpdateCmd(), Optional.empty());
        CompletableFuture<CommandResponse.SubmitResponse> addOrUpdateCommandF =
                hcdCmdService.submitAndWait(crmAddSetup, timeout);
        Assert.assertTrue(addOrUpdateCommandF.get() instanceof CommandResponse.Completed);
    }

    @Test
    // Note this is exactly the same as the previous test
    public void testCRMUsageForSubCommands() throws Exception {
        Setup crmAddSetup = new Setup(prefix(), crmAddOrUpdateCmd(), Optional.empty());
        CompletableFuture<CommandResponse.SubmitResponse> addOrUpdateCommandF =
                hcdCmdService.submitAndWait(crmAddSetup, timeout);
        //CommandResponse.Completed expectedResponse = new CommandResponse.Completed(crmAddSetup.runId());
        //Assert.assertEquals(expectedResponse, addOrUpdateCommandF.get());
        Assert.assertTrue(addOrUpdateCommandF.get() instanceof CommandResponse.Completed);
    }

}
