package example.command;

import akka.actor.testkit.typed.javadsl.TestInbox;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import csw.command.api.DemandMatcher;
import csw.command.api.StateMatcher;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandServiceFactory;
import csw.framework.internal.wiring.FrameworkWiring;
import csw.framework.internal.wiring.Standalone;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.JComponentType;
import csw.location.api.models.AkkaLocation;
import csw.location.api.models.ComponentId;
import csw.location.api.models.Connection.AkkaConnection;
import csw.location.client.ActorSystemFactory;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.location.server.http.JHTTPLocationService;
import csw.params.commands.*;
import csw.params.commands.CommandResponse.Completed;
import csw.params.commands.CommandResponse.SubmitResponse;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Id;
import csw.params.core.states.CurrentState;
import csw.params.core.states.DemandState;
import csw.params.core.states.StateName;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import io.lettuce.core.RedisClient;
import msocket.api.Subscription;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static csw.params.commands.CommandResponse.*;
import static org.tmt.csw.sample.ComponentStateForCommand.*;

public class JCommandServiceExample extends JUnitSuite {
    private static final ActorSystem<SpawnProtocol.Command> hcdActorSystem = ActorSystemFactory.remote(SpawnProtocol.create(), "test");

    private static JHTTPLocationService jHttpLocationService;
    private static ILocationService locationService;
    private static ICommandService hcdCmdService;
    private static AkkaLocation hcdLocation;
    private final Timeout timeout = new Timeout(20, TimeUnit.SECONDS);

    @BeforeClass
    public static void setup() throws Exception {
        jHttpLocationService = new JHTTPLocationService();
        jHttpLocationService.beforeAll();

        locationService = JHttpLocationServiceFactory.makeLocalClient(hcdActorSystem);
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
        Await.result(Standalone.spawn(ConfigFactory.load("aps_hcd_java.conf"), wiring), new FiniteDuration(5, TimeUnit.SECONDS));

        AkkaConnection akkaConnection = new AkkaConnection(new ComponentId(Prefix.apply(JSubsystem.IRIS, "test_component_running_long_command_java"), JComponentType.HCD));
        CompletableFuture<Optional<AkkaLocation>> eventualLocation = locationService.resolve(akkaConnection, java.time.Duration.ofSeconds(5));
        Optional<AkkaLocation> maybeLocation = eventualLocation.get();
        Assert.assertTrue(maybeLocation.isPresent());

        return maybeLocation.orElseThrow();
    }

    @Test
    public void testCommandExecutionBetweenComponents() throws Exception {
//
        Key<Integer> encoder = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Parameter<Integer> encoderValue = encoder.set(22, 23);
        Setup immediateCmd = new Setup(prefix(), immediateCmd(), Optional.empty()).add(encoderValue);

        //#immediate-response
        CompletableFuture<SubmitResponse> immediateCommandF =
                hcdCmdService.submitAndWait(immediateCmd, timeout).thenApply(
                        response -> {
                            if (response instanceof Completed) {
                                //do something with completed result
                            } else {
                                // do something with unexpected response
                            }
                            return response;
                        }
                );
        //#immediate-response
        Assert.assertTrue(immediateCommandF.get() instanceof Completed);
        Completed completed = (Completed) immediateCommandF.get();
        Assert.assertFalse(completed.result().nonEmpty());

        // immediate response - Completed with a result equal to input
        Setup imdResCommand = new Setup(prefix(), immediateResCmd(), Optional.empty()).add(encoderValue);

        CompletableFuture<SubmitResponse> imdResCmdResponseCompletableFuture = hcdCmdService.submitAndWait(imdResCommand, timeout);
        SubmitResponse actualImdCmdResponse = imdResCmdResponseCompletableFuture.get();
        Assert.assertTrue(actualImdCmdResponse instanceof Completed);
        completed = (Completed) actualImdCmdResponse;
        Assert.assertTrue(completed.result().nonEmpty());
        Assert.assertEquals(imdResCommand.paramSet(), completed.result().paramSet());

        //#invalidCmd
        Setup invalidSetup = new Setup(prefix(), invalidCmd(), Optional.empty()).add(encoderValue);
        CompletableFuture<SubmitResponse> invalidCommandF =
                hcdCmdService.submitAndWait(invalidSetup, timeout).thenApply(
                        response -> {
                            if (response instanceof Invalid invalid) {
                                // Cast the response to get the issue
                                assert (invalid.issue().reason().contains("failure"));
                            } else {
                                // Just do something to make the test fail
                                throw new IllegalArgumentException();
                            }
                            return response;
                        }
                );

        //#invalidCmd
        Assert.assertTrue(invalidCommandF.get() instanceof Invalid);
        Invalid invalidResponse = (Invalid) invalidCommandF.get();
        Assert.assertEquals(new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."), invalidResponse.issue());

        //#invalidSubmitCmd
        CompletableFuture<SubmitResponse> invalidSubmitCommandF =
                hcdCmdService.submit(invalidSetup).thenApply(
                        response -> {
                            if (response instanceof Started) {
                                //do something with completed result
                            } else if (response instanceof Invalid invalid) {
                                // Cast the response to get the issue
                                assert (invalid.issue().reason().contains("failure"));
                            }
                            return response;
                        }
                );
        //#invalidSubmitCmd
        Assert.assertTrue(invalidSubmitCommandF.get() instanceof Invalid);
        invalidResponse = (Invalid) invalidSubmitCommandF.get();
        Assert.assertEquals(new CommandIssue.OtherIssue("Testing: Received failure, will return Invalid."), invalidResponse.issue());

        // long running command which does not use matcher
        //#longRunning
        Setup longRunningSetup = new Setup(prefix(), longRunningCmd(), Optional.empty()).add(encoderValue);
        CompletableFuture<Optional<Integer>> longRunningResultF =
                hcdCmdService.submitAndWait(longRunningSetup, timeout)
                        .thenCompose(response -> {
                            if (response instanceof Completed) {
                                // This extracts and returns the the first value of parameter encoder
                                Result result = ((Completed) response).result();
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
        CompletableFuture<SubmitResponse> longRunningCommandResultF =
                hcdCmdService.submitAndWait(longRunningSetup, timeout);

        // do some work before querying for the result of above command as needed
        SubmitResponse sresponse = longRunningCommandResultF.get();
        CompletableFuture<SubmitResponse> queryResponseF = hcdCmdService.query(sresponse.runId());
        queryResponseF.thenAccept(r -> {
            if (r instanceof Started) {
                // happy case - no action needed
                // Do some other work
            } else {
                // log.error. This indicates that the command probably failed to start.
            }
        });

        CompletableFuture<Optional<Integer>> intF =
                longRunningCommandResultF.thenCompose(response -> {
                    if (response instanceof Completed) {
                        // This extracts and returns the the first value of parameter encoder
                        Result result = ((Completed) response).result();
                        Optional<Integer> rvalue = Optional.of(result.jGet(encoder).orElseThrow().head());
                        return CompletableFuture.completedFuture(rvalue);
                    } else {
                        // For some other response, return empty
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                });
        Assert.assertEquals(Optional.of(20), intF.get());
        //#queryLongRunning

        //#submitAndQueryFinal
        CompletableFuture<SubmitResponse> longRunningResultF2 = hcdCmdService.submit(longRunningSetup)
                .thenCompose(response -> {
                    if (response instanceof Started) {
                        // This extracts and returns the the first value of parameter encoder
                        Id id = ((Started) response).runId();
                        return hcdCmdService.queryFinal(id, timeout);
                    } else {
                        // For some other response (Invalid command)
                        return CompletableFuture.failedFuture(new RuntimeException("Submitted command is invalid"));
                    }
                });

        //#submitAndQueryFinal

        //#submitAndQuery
        CompletableFuture<SubmitResponse> longRunningSubmitResultF3 = hcdCmdService.submit(longRunningSetup);

        // do some work before querying for the result of above command as needed
        CompletableFuture<SubmitResponse> queryResponseF3 =
                hcdCmdService.query(longRunningSubmitResultF3.get().runId());
        queryResponseF3.thenAccept(r -> {
            if (r instanceof Started) {
                // happy case - no action needed
                // Do some other work
            } else {
                // log.error. This indicates that the command probably failed to start.
            }
        });
        //#submitAndQuery
//        CompletableFuture<SubmitResponse> queryResponseF3 =
//                hcdCmdService.query(longRunningSubmitResultF3.get().runId());

        //#queryFinal
        longRunningCommandResultF = hcdCmdService.submitAndWait(longRunningSetup, timeout);
        sresponse = longRunningCommandResultF.get();

        // longRunningSetup3 has already been submitted
        CompletableFuture<Optional<Integer>> int3F =
                hcdCmdService.queryFinal(sresponse.runId(), timeout).thenCompose(response -> {
                    if (response instanceof Completed) {
                        // This extracts and returns the the first value of parameter encoder
                        Result result = ((Completed) response).result();
                        Optional<Integer> rvalue = Optional.of(result.jGet(encoder).orElseThrow().head());
                        return CompletableFuture.completedFuture(rvalue);
                    } else {
                        // For some other response, return empty
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                });
        Assert.assertEquals(Optional.of(20), int3F.get());
        //#queryFinal

        //#query
        CompletableFuture<SubmitResponse> queryResponseF2 = hcdCmdService.query(sresponse.runId());
        Assert.assertTrue(queryResponseF2.get() instanceof Completed);
        //#query

        //#oneway
        Setup onewaySetup = new Setup(prefix(), onewayCmd(), Optional.empty()).add(encoderValue);
        CompletableFuture<Void> onewayF = hcdCmdService
                .oneway(onewaySetup)
                .thenAccept(onewayResponse -> {
                    if (onewayResponse instanceof Invalid) {
                        // log an error here
                    } else {
                        // Ignore anything other than invalid
                    }
                });
        //#oneway
        // just wait for command completion so that it will not impact other results
        onewayF.get();

        //#validate
        CompletableFuture<Boolean> validateCommandF =
                hcdCmdService.validate(immediateCmd)
                        .thenApply(
                                response -> {
                                    if (response instanceof Accepted) {
                                        //do something with completed result
                                        return true;
                                    } else if (response instanceof Invalid) {
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

        //#submitAll
        Setup submitAllSetup1 = new Setup(prefix(), immediateCmd(), Optional.empty()).add(encoderValue);
        Setup submitAllSetup2 = new Setup(prefix(), longRunningCmd(), Optional.empty()).add(encoderValue);
        Setup submitAllSetup3 = new Setup(prefix(), invalidCmd(), Optional.empty()).add(encoderValue);

        CompletableFuture<List<SubmitResponse>> submitAllF = hcdCmdService
                .submitAllAndWait(List.of(submitAllSetup1, submitAllSetup2, submitAllSetup3), timeout);

        List<SubmitResponse> submitAllResponse = submitAllF.get();
        Assert.assertEquals(submitAllResponse.size(), 3);
        Assert.assertTrue(submitAllResponse.get(0) instanceof Completed);
        Assert.assertTrue(submitAllResponse.get(1) instanceof Completed);
        Assert.assertTrue(submitAllResponse.get(2) instanceof Invalid);
        //#submitAll

        //#submitAllInvalid
        CompletableFuture<List<SubmitResponse>> submitAllF2 = hcdCmdService
                .submitAllAndWait(
                        List.of(submitAllSetup1, submitAllSetup3, submitAllSetup2),
                        timeout
                );

        List<SubmitResponse> submitAllResponse2 = submitAllF2.get();
        Assert.assertEquals(submitAllResponse2.size(), 2);
        Assert.assertTrue(submitAllResponse2.get(0) instanceof Completed);
        Assert.assertTrue(submitAllResponse2.get(1) instanceof Invalid);
        //#submitAllInvalid

        //#subscribeCurrentState
        // Subscriber cod
        int expectedEncoderValue = 234;
        Setup currStateSetup = new Setup(prefix(), hcdCurrentStateCmd(), Optional.empty()).add(encoder.set(expectedEncoderValue));
        // Setup a callback response to CurrentState - use AtomicInteger to capture final value
        final AtomicInteger cstate = new AtomicInteger((1));
        Subscription subscription = hcdCmdService.subscribeCurrentState(cs -> {
            // Example sets variable outside scope of closure
            cstate.set(cs.jGet(encoder).orElseThrow().head());
        });

        // Send a oneway to the HCD that will cause a publish of a CurrentState with the encoder value
        // in the command parameter "encoder"
        hcdCmdService.oneway(currStateSetup);

        // Wait for a bit for the callback
        Thread.sleep(200);
        // Check to see if CurrentState has the value we sent
        Assert.assertEquals(expectedEncoderValue, cstate.get());

        // Unsubscribe from CurrentState
        subscription.cancel();
        //#subscribeCurrentState

        // DEOPSCSW-229: Provide matchers infrastructure for comparison
        // long running command which uses matcher
        //#matcher
        Parameter<Integer> param = JKeyType.IntKey().make("encoder", JUnits.encoder).set(100);
        Setup setupWithMatcher = new Setup(prefix(), matcherCmd(), Optional.empty()).add(param);

        // create a StateMatcher which specifies the desired algorithm and state to be matched.
        DemandMatcher demandMatcher = new DemandMatcher(new DemandState(prefix(), new StateName("testStateName")).add(param), false, timeout);

        // Submit command as a oneway and if the command is successfully validated,
        // check for matching of demand state against current state
        CompletableFuture<MatchingResponse> matchResponseF = hcdCmdService.onewayAndMatch(setupWithMatcher, demandMatcher);
        MatchingResponse actualResponse = matchResponseF.get();
        Completed expectedResponse = new Completed(actualResponse.runId());
        Assert.assertEquals(expectedResponse, actualResponse);            // Not a great test for now
        //#matcher

        //#onewayAndMatch
        // create a DemandMatcher which specifies the desired state to be matched.
        StateMatcher stateMatcher = new DemandMatcher(new DemandState(prefix(), new StateName("testStateName")).add(param), false, timeout);

        CompletableFuture<MatchingResponse> matchedCommandResponseF =
                hcdCmdService.onewayAndMatch(setupWithMatcher, stateMatcher);

        //#onewayAndMatch
        MatchingResponse mresponse = matchedCommandResponseF.get();
        Assert.assertTrue(mresponse instanceof Completed);
    }


    @Test
    public void testCommandFailure() throws InterruptedException, ExecutionException {
        // using single submitAndSubscribe API
        Key<Integer> intKey1 = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Parameter<Integer> intParameter1 = intKey1.set(22, 23);
        Setup failureResCommand = new Setup(prefix(), failureAfterValidationCmd(), Optional.empty()).add(intParameter1);

        //#submitAndWait
        CompletableFuture<SubmitResponse> finalResponseCompletableFuture = hcdCmdService.submitAndWait(failureResCommand, timeout);
        SubmitResponse actualValidationResponse = finalResponseCompletableFuture.get();
        //#submitAndWait

        Assert.assertTrue(actualValidationResponse instanceof CommandResponse.Error);
    }


    @Test
    public void testSubscribeCurrentState() {
        Key<Integer> intKey1 = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Parameter<Integer> intParameter1 = intKey1.set(22, 23);
        Setup setup = new Setup(prefix(), acceptedCmd(), Optional.empty()).add(intParameter1);

        TestProbe<CurrentState> probe = TestProbe.create(hcdActorSystem);

        //#subscribeCurrentState
        // subscribe to the current state of an assembly component and use a callback which forwards each received
        // element to a test probe actor
        Subscription subscription = hcdCmdService.subscribeCurrentState(currentState -> probe.ref().tell(currentState));
        //#subscribeCurrentState

        hcdCmdService.submitAndWait(setup, timeout);

    }

    @Test
    public void testSubscribeOnlyCurrentState() throws InterruptedException {
        Key<Integer> intKey1 = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Parameter<Integer> intParameter1 = intKey1.set(22, 23);
        Setup setup = new Setup(prefix(), acceptedCmd(), Optional.empty()).add(intParameter1);

        TestInbox<CurrentState> inbox = TestInbox.create();
        Thread.sleep(1000);


        //#subscribeOnlyCurrentState
        // subscribe to the current state of an assembly component and use a callback which forwards each received
        // element to a test probe actor
        Subscription subscription = hcdCmdService.subscribeCurrentState(Set.of(StateName.apply("testStateSetup")), currentState -> inbox.getRef().tell(currentState));
        //#subscribeOnlyCurrentState

        hcdCmdService.submitAndWait(setup, timeout);

    }
}
