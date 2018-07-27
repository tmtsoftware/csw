package csw.framework.internal.supervisor

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox, TestProbe}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Terminated}
import csw.common.components.framework.SampleComponentBehaviorFactory
import csw.framework.ComponentInfos._
import csw.framework.exceptions.{FailureStop, InitializationFailed}
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.messages.CommandResponseManagerMessage.Query
import csw.messages.ComponentCommonMessage.{ComponentStateSubscription, GetSupervisorLifecycleState, LifecycleStateSubscription}
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.RunningMessage.Lifecycle
import csw.messages.SupervisorContainerCommonMessages.Restart
import csw.messages.SupervisorIdleMessage.InitializeTimeout
import csw.messages.SupervisorInternalRunningMessage.{RegistrationNotRequired, RegistrationSuccess}
import csw.messages.commands.CommandResponse
import csw.messages.framework.LocationServiceUsage.DoNotRegister
import csw.messages.framework.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.messages.framework.ToComponentLifecycleMessages._
import csw.messages.framework.{ComponentInfo, LifecycleStateChanged, PubSub, SupervisorLifecycleState}
import csw.messages.params.models.Id
import csw.messages.params.states.{CurrentState, StateName}
import csw.messages.{CommandResponseManagerMessage, ContainerIdleMessage, SupervisorMessage, TopLevelActorMessage}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
// DEOPSCSW-177: Hooks for lifecycle management
class SupervisorBehaviorLifecycleTest extends FrameworkTestSuite with BeforeAndAfterEach {

  class TestData(compInfo: ComponentInfo) {
    val testMocks: FrameworkTestMocks = frameworkTestMocks()
    import testMocks._

    val sampleHcdHandler: ComponentHandlers                        = mock[ComponentHandlers]
    val timerScheduler: TimerScheduler[SupervisorMessage]          = mock[TimerScheduler[SupervisorMessage]]
    val containerIdleMessageProbe: TestProbe[ContainerIdleMessage] = TestProbe[ContainerIdleMessage]
    val componentActorName                                         = s"${compInfo.name}-${SupervisorBehavior.ComponentActorNameSuffix}"

    val supervisorBehaviorKit = BehaviorTestKit(
      Behaviors
        .setup[SupervisorMessage](
          ctx =>
            new SupervisorBehavior(
              ctx,
              timerScheduler,
              None,
              compInfo,
              new SampleComponentBehaviorFactory,
              commandResponseManagerFactory,
              registrationFactory,
              locationService,
              eventService,
              loggerFactory
          )
        )
    )

    verify(timerScheduler).startSingleTimer(SupervisorBehavior.InitializeTimerKey, InitializeTimeout, hcdInfo.initializeTimeout)
  }

  test("supervisor should start in Idle lifecycle state and spawn four actors") {
    val testData = new TestData(hcdInfo)
    import testData._

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Idle)

    val spawnedEffects = supervisorBehaviorKit.retrieveAllEffects().map {
      case s: Spawned[_] ⇒ s.childName
      case _             ⇒ ""
    }

    spawnedEffects should contain allOf (
      SupervisorBehavior.PubSubLifecycleActor,
      SupervisorBehavior.PubSubComponentActor,
      SupervisorBehavior.CommandResponseManagerActorName,
      componentActorName
    )

    verify(timerScheduler).startSingleTimer(SupervisorBehavior.InitializeTimerKey, InitializeTimeout, hcdInfo.initializeTimeout)
  }

  // *************** Begin testing of onIdleMessages ***************
  test("supervisor should accept Running message and register with Location service when LocationServiceUsage is RegisterOnly") {
    val testData = new TestData(hcdInfo)
    import testData._
    import testData.testMocks._
    val supervisorLifecycleStateProbe            = TestProbe[SupervisorLifecycleState]
    val childRef: ActorRef[TopLevelActorMessage] = supervisorBehaviorKit.childInbox(componentActorName).ref

    supervisorBehaviorKit.run(Running(childRef))

    verify(timerScheduler).cancel(SupervisorBehavior.InitializeTimerKey)
    verify(locationService).register(akkaRegistration)

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)
  }

  test("supervisor should accept Running message and should not register when LocationServiceUsage is DoNotRegister") {
    val testData = new TestData(hcdInfo.copy(locationServiceUsage = DoNotRegister))
    import testData._
    import testData.testMocks._
    val supervisorLifecycleStateProbe            = TestProbe[SupervisorLifecycleState]
    val childRef: ActorRef[TopLevelActorMessage] = supervisorBehaviorKit.childInbox(componentActorName).ref

    supervisorBehaviorKit.run(Running(childRef))

    verify(locationService, never()).register(akkaRegistration)
    verify(timerScheduler).cancel(SupervisorBehavior.InitializeTimerKey)
    supervisorBehaviorKit.run(RegistrationNotRequired(childRef))

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)
  }
  // *************** End of testing onIdleMessages ***************

  // *************** Begin testing of onInternalMessage ***************
  test("supervisor should publish state change after successful registration with location service") {
    val testData = new TestData(hcdInfo)
    import testData._

    val supervisorLifecycleStateProbe            = TestProbe[SupervisorLifecycleState]
    val childRef: ActorRef[TopLevelActorMessage] = supervisorBehaviorKit.childInbox(componentActorName).ref

    supervisorBehaviorKit.run(Running(childRef))
    supervisorBehaviorKit.run(RegistrationSuccess(childRef))

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)

    val childPubSubLifecycleInbox: TestInbox[PubSub[LifecycleStateChanged, LifecycleStateChanged]] =
      supervisorBehaviorKit.childInbox(SupervisorBehavior.PubSubLifecycleActor)

    childPubSubLifecycleInbox.receiveMessage() shouldBe Publish(
      LifecycleStateChanged(supervisorBehaviorKit.selfInbox().ref, SupervisorLifecycleState.Running)
    )
  }

  test("supervisor should publish state change if locationServiceUsage is DoNotRegister") {
    val testData = new TestData(hcdInfo)
    import testData._
    val supervisorLifecycleStateProbe            = TestProbe[SupervisorLifecycleState]
    val childRef: ActorRef[TopLevelActorMessage] = supervisorBehaviorKit.childInbox(componentActorName).ref

    supervisorBehaviorKit.run(Running(childRef))
    supervisorBehaviorKit.run(RegistrationNotRequired(childRef))

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)

    val childPubSubLifecycleInbox: TestInbox[PubSub[LifecycleStateChanged, LifecycleStateChanged]] =
      supervisorBehaviorKit.childInbox(SupervisorBehavior.PubSubLifecycleActor)

    childPubSubLifecycleInbox.receiveMessage() shouldBe Publish(
      LifecycleStateChanged(supervisorBehaviorKit.selfInbox().ref, SupervisorLifecycleState.Running)
    )
  }
  // *************** End of testing onInternalMessage ***************

  // *************** Begin testing of onCommonMessages ***************
  test("supervisor should handle LifecycleStateSubscription message by coordinating with pub sub actor") {
    val testData = new TestData(hcdInfo)
    import testData._
    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]
    val subscriberProbe               = TestProbe[LifecycleStateChanged]

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    val previousSupervisorLifecycleState = supervisorLifecycleStateProbe.expectMessageType[SupervisorLifecycleState]

    // Subscribe
    supervisorBehaviorKit.run(LifecycleStateSubscription(Subscribe(subscriberProbe.ref)))

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(previousSupervisorLifecycleState)

    val childPubSubLifecycleInbox: TestInbox[PubSub[LifecycleStateChanged, LifecycleStateChanged]] =
      supervisorBehaviorKit.childInbox(SupervisorBehavior.PubSubLifecycleActor)

    val subscribeMessage = childPubSubLifecycleInbox.receiveMessage()
    subscribeMessage shouldBe Subscribe[LifecycleStateChanged, LifecycleStateChanged](subscriberProbe.ref)

    // Unsubscribe
    supervisorBehaviorKit.run(LifecycleStateSubscription(Unsubscribe[LifecycleStateChanged, LifecycleStateChanged](subscriberProbe.ref)))

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(previousSupervisorLifecycleState)

    val unsubscribeMessage = childPubSubLifecycleInbox.receiveMessage()
    unsubscribeMessage shouldBe Unsubscribe[LifecycleStateChanged, LifecycleStateChanged](subscriberProbe.ref)
  }

  test("supervisor should handle ComponentStateSubscription message by coordinating with pub sub actor") {
    val testData = new TestData(hcdInfo)
    import testData._

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]
    val subscriberProbe               = TestProbe[CurrentState]

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    val previousSupervisorLifecycleState = supervisorLifecycleStateProbe.expectMessageType[SupervisorLifecycleState]

    // Subscribe
    supervisorBehaviorKit.run(ComponentStateSubscription(Subscribe[CurrentState, StateName](subscriberProbe.ref)))
    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(previousSupervisorLifecycleState)

    val childPubSubComponentStateInbox: TestInbox[PubSub[CurrentState, StateName]] =
      supervisorBehaviorKit.childInbox(SupervisorBehavior.PubSubComponentActor)

    val subscribeMessage = childPubSubComponentStateInbox.receiveMessage()
    subscribeMessage shouldBe Subscribe[CurrentState, StateName](subscriberProbe.ref)

    // Unsubscribe
    supervisorBehaviorKit.run(ComponentStateSubscription(Unsubscribe[CurrentState, StateName](subscriberProbe.ref)))
    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(previousSupervisorLifecycleState)

    val unsubscribeMessage = childPubSubComponentStateInbox.receiveMessage()
    unsubscribeMessage shouldBe Unsubscribe[CurrentState, StateName](subscriberProbe.ref)
  }

  // *************** End of testing onCommonMessages ***************

  /**
   * Below Tests show that all external messages for the TLA are received by the Supervisor
   * which passes them to TLA (depending on lifecycle)
  **/
  // *************** Begin testing of onRunning Messages ***************

  test("supervisor should handle lifecycle Restart message") {
    val testData = new TestData(hcdInfo)
    import testData._
    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]

    supervisorBehaviorKit.run(Restart)
    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Restart)
  }

  test("supervisor should handle lifecycle GoOffline message") {
    val testData = new TestData(hcdInfo)
    import testData._

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]

    val childComponentInbox: TestInbox[TopLevelActorMessage] = supervisorBehaviorKit.childInbox(componentActorName)

    supervisorBehaviorKit.run(Running(childComponentInbox.ref))
    supervisorBehaviorKit.run(RegistrationSuccess(childComponentInbox.ref))
    supervisorBehaviorKit.run(Lifecycle(GoOffline))

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.RunningOffline)

    childComponentInbox.receiveAll() should contain(Lifecycle(GoOffline))
  }

  test("supervisor should handle lifecycle GoOnline message") {
    val testData = new TestData(hcdInfo)
    import testData._
    val supervisorLifecycleStateProbe                        = TestProbe[SupervisorLifecycleState]
    val childComponentInbox: TestInbox[TopLevelActorMessage] = supervisorBehaviorKit.childInbox(componentActorName)

    supervisorBehaviorKit.run(Running(childComponentInbox.ref))
    supervisorBehaviorKit.run(RegistrationSuccess(childComponentInbox.ref))
    supervisorBehaviorKit.run(Lifecycle(GoOffline))
    supervisorBehaviorKit.run(Lifecycle(GoOnline))

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)
    childComponentInbox.receiveAll() should contain(Lifecycle(GoOnline))
  }

  // *************** End of testing onRunning Messages ***************

  test("supervisor should handle Terminated signal for Idle lifecycle state") {
    val testData = new TestData(hcdInfo)
    import testData._
    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]

    val childComponentInbox: TestInbox[TopLevelActorMessage] = supervisorBehaviorKit.childInbox(componentActorName)

    supervisorBehaviorKit.run(GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Idle)

    intercept[InitializationFailed.type] {
      supervisorBehaviorKit.signal(Terminated(childComponentInbox.ref)(new FailureStop(message = "reason of failing") {}))
    }
  }

  test("supervisor should handle Command Response Manager messages by forwarding it to Command Response Manager") {
    val testData = new TestData(hcdInfo)
    import testData._

    val childRef: ActorRef[TopLevelActorMessage] = supervisorBehaviorKit.childInbox(componentActorName).ref
    val subscriberProbe                          = TestProbe[CommandResponse]
    val testCmdId                                = Id()

    supervisorBehaviorKit.run(Running(childRef))

    supervisorBehaviorKit.run(Query(testCmdId, subscriberProbe.ref))
    testMocks.commandResponseManagerActor.expectMessage(Query(testCmdId, subscriberProbe.ref))

    supervisorBehaviorKit.run(CommandResponseManagerMessage.Subscribe(testCmdId, subscriberProbe.ref))
    testMocks.commandResponseManagerActor.expectMessage(CommandResponseManagerMessage.Subscribe(testCmdId, subscriberProbe.ref))

    supervisorBehaviorKit.run(CommandResponseManagerMessage.Unsubscribe(testCmdId, subscriberProbe.ref))
    testMocks.commandResponseManagerActor.expectMessage(CommandResponseManagerMessage.Unsubscribe(testCmdId, subscriberProbe.ref))
  }
}
