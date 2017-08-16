package csw.common.framework.javadsl.integration;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.Props;
import akka.typed.scaladsl.Actor;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import akka.util.Timeout;
import csw.common.ccs.CommandStatus;
import csw.common.ccs.DemandMatcher;
import csw.common.components.SampleComponentState;
import csw.common.framework.javadsl.commons.JClassTag;
import csw.common.framework.models.*;
import csw.common.framework.scaladsl.SupervisorBehaviorFactory;
import csw.param.commands.CommandInfo;
import csw.param.commands.Setup;
import csw.param.generics.JKeyTypes;
import csw.param.generics.Key;
import csw.param.generics.Parameter;
import csw.param.models.Choice;
import csw.param.states.CurrentState;
import csw.param.states.DemandState;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

// DEOPSCSW-165: CSW Assembly Creation
// DEOPSCSW-166: CSW HCD Creation
// DEOPSCSW-177: Hooks for lifecycle management
public class JComponentIntegrationTest {
    private static ActorSystem system = ActorSystem.create(Actor.empty(), "Hcd");
    private static TestKitSettings settings = TestKitSettings.apply(system);

    private static ComponentInfo.HcdInfo hcdInfo = new ComponentInfo.HcdInfo("trombone",
            "wfos",
            "csw.common.framework.javadsl.integration.JSampleComponentWiring",
            LocationServiceUsages.JDoNotRegister(),
            null,
            null);

    private static Timeout seconds = Timeout.durationToTimeout(FiniteDuration.apply(5, "seconds"));
    private static Behavior<SupervisorMsg> supervisorBehavior = SupervisorBehaviorFactory.make(hcdInfo);
    private static FiniteDuration duration = Duration.create(5, "seconds");
    private static Future<ActorRef<SupervisorMsg>> systemActorOf = system.<SupervisorMsg>systemActorOf(supervisorBehavior, "hcd", Props.empty(), seconds);
    private static ActorRef<SupervisorMsg> supervisorRef;
    private static TestProbe<CurrentState> compStateProbe  = TestProbe.apply(system, settings);

    @BeforeClass
    public static void beforeAll() throws Exception {
        supervisorRef = Await.result(systemActorOf, duration);
        supervisorRef.tell(new CommonSupervisorMsg.ComponentStateSubscription(new PubSub.Subscribe<>(compStateProbe.ref())));
    }

    @AfterClass
    public static void afterAll() throws Exception {
        Await.result(system.terminate(), Duration.create(5, "seconds"));
    }

    @Test
    public void shouldInvokeOnInitializeAndOnRun() throws Exception {
        CurrentState initCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> initParam = SampleComponentState.choiceKey().set(SampleComponentState.initChoice());
        DemandState initDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(initParam);
        Assert.assertTrue(new DemandMatcher(initDemandState, false).check(initCurrentState));

        CurrentState runCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> runParam = SampleComponentState.choiceKey().set(SampleComponentState.runChoice());
        DemandState runDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(runParam);
        Assert.assertTrue(new DemandMatcher(runDemandState, false).check(runCurrentState));
    }

    // DEOPSCSW-179: Unique Action for a component
    @Test
    public void shouldInvokeOnDomainMsg() throws Exception {

        JComponentDomainMsg myDomainSpecificMsg = new JComponentDomainMsg();
        supervisorRef.tell(myDomainSpecificMsg);

        CurrentState domainCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> domainParam = SampleComponentState.choiceKey().set(SampleComponentState.domainChoice());
        DemandState domainDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(domainParam);
        Assert.assertTrue(new DemandMatcher(domainDemandState, false).check(domainCurrentState));
    }

    @Test
    public void shouldInvokeOnControlCommand() throws Exception {

        String prefix = "wfos.red.detector";
        Key<Integer> encoderIntKey = JKeyTypes.IntKey().make("encoder");
        Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
        CommandInfo commandInfo = new CommandInfo("obsId");
        Setup setup = new Setup(commandInfo, prefix).add(encoderParam);

        ActorRef<CommandStatus.CommandResponse> testRef = TestProbe.<CommandStatus.CommandResponse>apply(system, settings).ref();
        supervisorRef.tell(new CommandMsg.Oneway(setup, testRef));

        CurrentState commandCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> commandParam = SampleComponentState.choiceKey().set(SampleComponentState.commandChoice());
        DemandState commandDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(commandParam);
        Assert.assertTrue(new DemandMatcher(commandDemandState, false).check(commandCurrentState));
    }

}
