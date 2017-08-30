package csw.common.framework.javadsl.integration;

import akka.Done;
import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.Props;
import akka.typed.javadsl.Adapter;
import akka.typed.scaladsl.Actor;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import akka.util.Timeout;
import csw.common.ccs.CommandStatus;
import csw.common.ccs.DemandMatcher;
import csw.common.components.SampleComponentState;
import csw.common.framework.internal.supervisor.SupervisorBehaviorFactory;
import csw.common.framework.internal.supervisor.SupervisorMode;
import csw.common.framework.javadsl.JComponentInfo;
import csw.common.framework.javadsl.commons.JClassTag;
import csw.common.framework.models.*;
import csw.param.commands.CommandInfo;
import csw.param.commands.Setup;
import csw.param.generics.JKeyTypes;
import csw.param.generics.Key;
import csw.param.generics.Parameter;
import csw.param.models.Choice;
import csw.param.states.CurrentState;
import csw.param.states.DemandState;
import csw.services.location.javadsl.JComponentType;
import csw.services.location.models.AkkaRegistration;
import csw.services.location.models.Connection;
import csw.services.location.models.RegistrationResult;
import csw.services.location.scaladsl.ActorSystemFactory;
import csw.services.location.scaladsl.LocationService;
import csw.services.location.scaladsl.RegistrationFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import scala.Some$;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.Promise$;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import csw.common.framework.models.LocationServiceUsage;

import java.util.Collections;

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
// DEOPSCSW-165: CSW Assembly Creation
// DEOPSCSW-166: CSW HCD Creation
// DEOPSCSW-176: Provide Infrastructure to manage TMT lifecycle
// DEOPSCSW-177: Hooks for lifecycle management
public class JFrameworkIntegrationTest extends Mockito {
    private static ActorSystem system = ActorSystem.create(Actor.empty(), "Hcd");
    private static TestKitSettings settings = TestKitSettings.apply(system);
    private ComponentInfo hcdInfo = JComponentInfo.from(
            "trombone",
            JComponentType.HCD,
            "wfos",
            "csw.common.framework.javadsl.integration.JSampleComponentBehaviorFactory",
            LocationServiceUsage.JDoNotRegister(),
            Collections.emptySet());

    private static akka.actor.ActorSystem untypedSystem = ActorSystemFactory.remote();

    private static AkkaRegistration akkaRegistration = AkkaRegistration.apply(
            mock(Connection.AkkaConnection.class),
            TestProbe.apply(Adapter.toTyped(untypedSystem), settings).ref()
    );
    private static RegistrationResult registrationResult = mock(RegistrationResult.class);
    private static LocationService locationService = mock(LocationService.class);
    private static RegistrationFactory registrationFactory = mock(RegistrationFactory.class);

    private Timeout seconds = Timeout.durationToTimeout(FiniteDuration.apply(5, "seconds"));
    private TestProbe<ContainerIdleMessage> containerIdleMessageProbe = TestProbe.apply(system, settings);
    private Behavior<SupervisorExternalMessage> supervisorBehavior = SupervisorBehaviorFactory.make(Some$.MODULE$.apply(containerIdleMessageProbe.ref()), hcdInfo, locationService, registrationFactory);
    private FiniteDuration duration = Duration.create(5, "seconds");
    private Future<ActorRef<SupervisorExternalMessage>> systemActorOf;
    private ActorRef<SupervisorExternalMessage> supervisorRef;
    private TestProbe<CurrentState> compStateProbe;
    private TestProbe<LifecycleStateChanged> lifecycleStateChangedProbe;

    private void createSupervisorAndStartTLA() throws Exception {
        compStateProbe  = TestProbe.apply(system, settings);
        lifecycleStateChangedProbe  = TestProbe.apply(system, settings);
        systemActorOf = system.<SupervisorMessage>systemActorOf(supervisorBehavior, "hcd", Props.empty(), seconds);
        supervisorRef = Await.result(systemActorOf, duration);
        Thread.sleep(200);
        supervisorRef.tell(new SupervisorCommonMessage.ComponentStateSubscription(new PubSub.Subscribe<>(compStateProbe.ref())));
        supervisorRef.tell(new SupervisorCommonMessage.LifecycleStateSubscription(new PubSub.Subscribe<>(lifecycleStateChangedProbe.ref())));
    }

    @BeforeClass
    public static void beforeAll() {
        when(registrationFactory.akkaTyped(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(akkaRegistration);

        Future<RegistrationResult> eventualRegistrationResult = Promise$.MODULE$.successful(registrationResult).future();
        when(locationService.register(akkaRegistration)).thenReturn(eventualRegistrationResult);

        Future<Done> eventualDone = Promise$.MODULE$.successful(Done.getInstance()).future();
        when(registrationResult.unregister()).thenReturn(eventualDone);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        Await.result(system.terminate(), Duration.create(5, "seconds"));
        Await.result(untypedSystem.terminate(), Duration.create(5, "seconds"));
    }

    @Test
    public void shouldInvokeOnInitializeAndOnRun() throws Exception {
        compStateProbe  = TestProbe.apply(system, settings);
        lifecycleStateChangedProbe  = TestProbe.apply(system, settings);
        systemActorOf = system.<SupervisorMessage>systemActorOf(supervisorBehavior, "hcd", Props.empty(), seconds);
        supervisorRef = Await.result(systemActorOf, duration);
        supervisorRef.tell(new SupervisorCommonMessage.ComponentStateSubscription(new PubSub.Subscribe<>(compStateProbe.ref())));
        supervisorRef.tell(new SupervisorCommonMessage.LifecycleStateSubscription(new PubSub.Subscribe<>(lifecycleStateChangedProbe.ref())));

        CurrentState initCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> initParam = SampleComponentState.choiceKey().set(SampleComponentState.initChoice());
        DemandState initDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(initParam);
        Assert.assertTrue(new DemandMatcher(initDemandState, false).check(initCurrentState));

        CurrentState runCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> runParam = SampleComponentState.choiceKey().set(SampleComponentState.runChoice());
        DemandState runDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(runParam);
        Assert.assertTrue(new DemandMatcher(runDemandState, false).check(runCurrentState));

        lifecycleStateChangedProbe.expectMsg(new LifecycleStateChanged(supervisorRef, SupervisorMode.Running$.MODULE$));
    }

    // DEOPSCSW-179: Unique Action for a component
    @Test
    public void shouldInvokeOnDomainMsg() throws Exception {
        createSupervisorAndStartTLA();

        JComponentDomainMessage myDomainSpecificMsg = new JComponentDomainMessage();
        supervisorRef.tell(myDomainSpecificMsg);

        CurrentState domainCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> domainParam = SampleComponentState.choiceKey().set(SampleComponentState.domainChoice());
        DemandState domainDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(domainParam);
        Assert.assertTrue(new DemandMatcher(domainDemandState, false).check(domainCurrentState));
    }

    @Test
    public void shouldInvokeOnControlCommand() throws Exception {
        createSupervisorAndStartTLA();

        String prefix = "wfos.red.detector";
        Key<Integer> encoderIntKey = JKeyTypes.IntKey().make("encoder");
        Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
        CommandInfo commandInfo = new CommandInfo("obsId");
        Setup setup = new Setup(commandInfo, prefix).add(encoderParam);

        ActorRef<CommandStatus.CommandResponse> testRef = TestProbe.<CommandStatus.CommandResponse>apply(system, settings).ref();
        supervisorRef.tell(new CommandMessage.Oneway(setup, testRef));

        CurrentState commandCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> commandParam = SampleComponentState.choiceKey().set(SampleComponentState.commandChoice());
        DemandState commandDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(commandParam);
        Assert.assertTrue(new DemandMatcher(commandDemandState, false).check(commandCurrentState));
    }

    @Test
    public void shouldInvokeOnGoOfflineAndOnGoOnline() throws Exception {
        createSupervisorAndStartTLA();

        supervisorRef.tell(new RunningMessage.Lifecycle(ToComponentLifecycleMessage.GoOffline$.MODULE$));

        CurrentState offlineCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> offlineParam = SampleComponentState.choiceKey().set(SampleComponentState.offlineChoice());
        DemandState offlineDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(offlineParam);
        Assert.assertTrue(new DemandMatcher(offlineDemandState, false).check(offlineCurrentState));

        supervisorRef.tell(new RunningMessage.Lifecycle(ToComponentLifecycleMessage.GoOnline$.MODULE$));

        CurrentState onlineCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> onlineParam = SampleComponentState.choiceKey().set(SampleComponentState.onlineChoice());
        DemandState onlineDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(onlineParam);
        Assert.assertTrue(new DemandMatcher(onlineDemandState, false).check(onlineCurrentState));
    }

    @Test
    public void shouldInvokeOnShutdown() throws Exception {
        createSupervisorAndStartTLA();

        supervisorRef.tell(new RunningMessage.Lifecycle(ToComponentLifecycleMessage.Shutdown$.MODULE$));

        CurrentState shutdownCurrentState = compStateProbe.expectMsgType(JClassTag.make(CurrentState.class));
        Parameter<Choice> shutdownParam = SampleComponentState.choiceKey().set(SampleComponentState.shutdownChoice());
        DemandState shutdownDemandState  = new DemandState(SampleComponentState.prefix().prefix()).add(shutdownParam);
        Assert.assertTrue(new DemandMatcher(shutdownDemandState, false).check(shutdownCurrentState));

        lifecycleStateChangedProbe.expectMsg(new LifecycleStateChanged(supervisorRef, SupervisorMode.PreparingToShutdown$.MODULE$));
    }
}
