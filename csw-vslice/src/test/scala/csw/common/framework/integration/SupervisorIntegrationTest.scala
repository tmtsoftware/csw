package csw.common.framework.integration

import akka.typed.scaladsl.Actor
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.DemandMatcher
import csw.common.components.{ComponentDomainMessage, ComponentStatistics, SampleComponentHandlers}
import csw.common.framework.ComponentInfos._
import csw.common.framework.FrameworkTestSuite
import csw.common.framework.internal.component.ComponentBehavior
import csw.common.framework.internal.supervisor.{SupervisorBehavior, SupervisorBehaviorFactory, SupervisorMode}
import csw.common.framework.models.CommandMessage.Oneway
import csw.common.framework.models.PubSub.{Publish, PublisherMessage, Subscribe}
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.common.framework.models.{ContainerIdleMessage, LifecycleStateChanged, SupervisorExternalMessage, _}
import csw.common.framework.scaladsl.ComponentBehaviorFactory
import csw.param.commands.{CommandInfo, Setup}
import csw.param.generics.{KeyType, Parameter}
import csw.param.states.{CurrentState, DemandState}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
// DEOPSCSW-165: CSW Assembly Creation
// DEOPSCSW-166: CSW HCD Creation
// DEOPSCSW-176: Provide Infrastructure to manage TMT lifecycle
// DEOPSCSW-177: Hooks for lifecycle management
class SupervisorIntegrationTest extends FrameworkTestSuite with MockitoSugar with BeforeAndAfterEach {
  import csw.common.components.SampleComponentState._

  var compStateProbe: TestProbe[CurrentState]                    = _
  var lifecycleStateProbe: TestProbe[LifecycleStateChanged]      = _
  var supervisorBehavior: Behavior[SupervisorExternalMessage]    = _
  var supervisorRef: ActorRef[SupervisorExternalMessage]         = _
  var containerIdleMessageProbe: TestProbe[ContainerIdleMessage] = _

  def createSupervisorAndStartTLA(): Unit = {
    compStateProbe = TestProbe[CurrentState]
    lifecycleStateProbe = TestProbe[LifecycleStateChanged]
    containerIdleMessageProbe = TestProbe[ContainerIdleMessage]

    supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(containerIdleMessageProbe.testActor),
      hcdInfo,
      locationService,
      registrationFactory
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = Await.result(system.systemActorOf(supervisorBehavior, "comp-supervisor"), 5.seconds)
    Thread.sleep(200)
    supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))
  }

  // Mostly, onInitialized and onRun hooks of components gets invoked by the time we subscribe for ComponentState
  // Hence by just subscribing to ComponentState does not always guarantees that
  // subscriber receives onInitialized and onRun hooks CurrentState like other tests from this file.
  // Hence this particular test mocks componentBehaviorFactory to just provide pubSubRef test probe,
  // so that we can expect CurrentState message from onInitialized and onRun hooks
  test("onInitialized and onRun hooks of comp handlers should be invoked when supervisor creates comp") {
    compStateProbe = TestProbe[CurrentState]
    lifecycleStateProbe = TestProbe[LifecycleStateChanged]
    containerIdleMessageProbe = TestProbe[ContainerIdleMessage]
    val pubSubRefProbe = TestProbe[PubSub[CurrentState]]

    // This code is here because of the reason explained above test.
    val componentTLA = new Answer[Behavior[Nothing]] {
      override def answer(invocation: InvocationOnMock): Behavior[Nothing] = {
        val compInfo   = invocation.getArgument[ComponentInfo](0)
        val supervisor = invocation.getArgument[ActorRef[FromComponentLifecycleMessage]](1)

        Actor
          .mutable[ComponentMessage](
            ctx ⇒
              new ComponentBehavior[ComponentDomainMessage](
                ctx,
                supervisor,
                new SampleComponentHandlers(ctx, compInfo, pubSubRefProbe.ref)
            )
          )
          .narrow
      }
    }
    val compWring = mock[ComponentBehaviorFactory[_]]
    import ArgumentMatchers._
    Mockito
      .when(
        compWring.make(any[ComponentInfo],
                       any[ActorRef[FromComponentLifecycleMessage]],
                       any[ActorRef[PublisherMessage[CurrentState]]])
      )
      .thenAnswer(componentTLA)

    supervisorBehavior = Actor
      .withTimers[SupervisorMessage](
        timerScheduler ⇒
          Actor.mutable[SupervisorMessage](
            ctx =>
              new SupervisorBehavior(
                ctx,
                Some(containerIdleMessageProbe.testActor),
                timerScheduler,
                hcdInfo,
                compWring,
                registrationFactory,
                locationService
            )
        )
      )
      .narrow

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = Await.result(system.systemActorOf(supervisorBehavior, "hcd-supervisor"), 5.seconds)
    supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

    pubSubRefProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    pubSubRefProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    lifecycleStateProbe.expectMsg(LifecycleStateChanged(supervisorRef, SupervisorMode.Running))
  }

  // DEOPSCSW-179: Unique Action for a component
  test("onDomainMsg hook of comp handlers should be invoked when supervisor receives Domain message") {
    createSupervisorAndStartTLA()

    supervisorRef ! ComponentStatistics(1)

    val domainCurrentState = compStateProbe.expectMsgType[CurrentState]
    val domainDemandState  = DemandState(prefix, Set(choiceKey.set(domainChoice)))
    DemandMatcher(domainDemandState).check(domainCurrentState) shouldBe true
  }

  test("onControlCommand hook of comp handlers should be invoked when supervisor receives Control command") {
    createSupervisorAndStartTLA()

    val commandInfo: CommandInfo = "Obs001"
    val param: Parameter[Int]    = KeyType.IntKey.make("encoder").set(22)
    val setup: Setup             = Setup(commandInfo, prefix, Set(param))

    supervisorRef ! Oneway(setup, TestProbe[CommandResponse].ref)

    val commandCurrentState = compStateProbe.expectMsgType[CurrentState]
    val commandDemandState  = DemandState(prefix, Set(choiceKey.set(commandChoice)))
    DemandMatcher(commandDemandState).check(commandCurrentState) shouldBe true
  }

  test("onGoOffline and goOnline hooks of comp handlers should be invoked when supervisor receives Lifecycle messages") {
    createSupervisorAndStartTLA()

    supervisorRef ! Lifecycle(GoOffline)

    val offlineCurrentState = compStateProbe.expectMsgType[CurrentState]
    val offlineDemandState  = DemandState(prefix, Set(choiceKey.set(offlineChoice)))
    DemandMatcher(offlineDemandState).check(offlineCurrentState) shouldBe true

    supervisorRef ! Lifecycle(GoOnline)

    val onlineCurrentState = compStateProbe.expectMsgType[CurrentState]
    val onlineDemandState  = DemandState(prefix, Set(choiceKey.set(onlineChoice)))
    DemandMatcher(onlineDemandState).check(onlineCurrentState) shouldBe true
  }

  test("running component should ignore RunOnline lifecycle message when it is already online") {
    createSupervisorAndStartTLA()

    supervisorRef ! Lifecycle(GoOnline)
    compStateProbe.expectNoMsg(1.seconds)
  }

  test("running component should ignore RunOffline lifecycle message when it is already offline") {
    createSupervisorAndStartTLA()

    supervisorRef ! Lifecycle(GoOffline)
    compStateProbe.expectMsgType[CurrentState]

    supervisorRef ! Lifecycle(GoOffline)
    compStateProbe.expectNoMsg(1.seconds)

    supervisorRef ! Lifecycle(GoOnline)
    compStateProbe.expectMsgType[CurrentState]
  }

  ignore("onShutdown hook of comp handlers should be invoked when supervisor receives Shutdown message") {
    createSupervisorAndStartTLA()

    untypedSystem.terminate()

    val shutdownCurrentState = compStateProbe.expectMsgType[CurrentState]
    val shutdownDemandState  = DemandState(prefix, Set(choiceKey.set(shutdownChoice)))
    DemandMatcher(shutdownDemandState).check(shutdownCurrentState) shouldBe true

  }
}
