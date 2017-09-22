package csw.framework.internal.supervisor

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.SampleComponentState._
import csw.common.components.{ComponentDomainMessage, SampleComponentHandlers}
import csw.framework.ComponentInfos._
import csw.exceptions.{FailureRestart, FailureStop}
import csw.framework.models.PubSub.{Publish, PublisherMessage}
import csw.framework.models.SupervisorCommonMessage.GetSupervisorLifecycleState
import csw.framework.models._
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.param.states.CurrentState
import csw.services.location.scaladsl.LocationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.Answer

import scala.concurrent.Future

// DEOPSCSW-178: Lifecycle success/failure notification
class SupervisorLifecycleFailureTest extends FrameworkTestSuite {

  val supervisorLifecycleStateProbe: TestProbe[SupervisorLifecycleState] = TestProbe[SupervisorLifecycleState]
  var supervisorRef: ActorRef[SupervisorExternalMessage]                 = _
  var initializeAnswer: Answer[Future[Unit]]                             = _
  var shutdownAnswer: Answer[Future[Unit]]                               = _
  var runAnswer: Answer[Future[Unit]]                                    = _

  test("handle TLA failure with FailureStop exception in initialize with Restart message") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers = createComponentHandlers(testMocks)

    // Throw a `FailureStop` on the first attempt to initialize but initialize successfully on the next attempt
    doThrow(FailureStop()).doAnswer(initializeAnswer).when(componentHandlers).initialize()

    createSupervisorAndStartTLA(testMocks, componentHandlers)

    // Component fails to initialize with `FailureStop`. The default akka supervision strategy kills the TLA
    // and triggers the PostStop signal of TLA. The post stop signal has `onShutdown` handler of the component which is
    // mocked in the test to publish a `shutdownChoice`.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

    supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)

    // Supervisor is still in the Idle lifecycle state
    supervisorLifecycleStateProbe.expectMsg(SupervisorLifecycleState.Idle)

    // External entity sends restart to supervisor with the intent to restart TLA
    supervisorRef ! Restart

    // TLA initialises successfully the second time due to the defined mock behavior. The `initialize` handler of the
    // component is mocked in the test to publish a `initChoice` in the second attempt.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    // After successful TLA initialization, TLA receives `Run` message from supervisor which triggers `onRun` handler.
    // The `onRun` handler of the component is mocked in the test to publish a `runChoice`.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    // Supervisor registers itself only after successful initialization of TLA. In this test the TLA successfully
    // initializes after Restart message, after which Supervisor registers itself.
    verify(locationService).register(akkaRegistration)

    // On receiving Restart message the supervisor first unregisters itself. But in the case of initialization failures
    // it never registers itself and thus never unregisters as well on receiving Restart message.
    verify(registrationResult, never()).unregister()
  }

  test("handle TLA failure with FailureRestart exception in initialize") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers = createComponentHandlers(testMocks)

    // Throw a `FailureRestart` on the first attempt to initialize but initialize successfully on the next attempt
    doThrow(FailureRestart()).doAnswer(initializeAnswer).when(componentHandlers).initialize()
    createSupervisorAndStartTLA(testMocks, componentHandlers)

    // component fails to initialize with `FailureRestart`. The akka supervision strategy specified in SupervisorBehavior
    // restarts the TLA. The `initialize` handler of the component is mocked in the test to publish a `initChoice` in the second attempt.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    // After successful TLA initialization, TLA receives `Run` message from supervisor which triggers `onRun` handler.
    // The `onRun` handler of the component is mocked in the test to publish a `runChoice`.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    // Supervisor registers itself only after successful initialization of TLA. In this test the TLA successfully
    // initializes after Restart message after which Supervisor registers itself
    verify(locationService).register(akkaRegistration)
  }

  test("handle external restart when TLA throws FailureStop exception in onRun") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers = createComponentHandlers(testMocks)

    //Throw a `FailureStop` on the first attempt to call `onRun` handler on TLA but publish `runChoice` successfully on the next attempt
    doThrow(FailureStop()).doAnswer(runAnswer).when(componentHandlers).onRun()

    createSupervisorAndStartTLA(testMocks, componentHandlers)

    // TLA initialises successfully by calling the `initialize` component handler. The `initialize` handler of the
    // component is mocked in the test to publish a `initChoice`.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    // TLA fails on the `onRun` component handler on the first attempt. The default akka supervision strategy kills the TLA
    // and triggers the PostStop signal of TLA. The post stop signal has `onShutdown` handler of the component which is
    // mocked in the test to publish a `shutdownChoice`.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

    // External entity sends restart to supervisor with the intent to restart TLA
    supervisorRef ! Restart

    // The `initialize` handler of the component is mocked in the test to publish a `initChoice`.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    // After successful TLA initialization, TLA receives `Run` message from supervisor which triggers `onRun` handler.
    // The `onRun` handler of the component is mocked in the test to publish a `runChoice` in the second attempt.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    // On receiving Restart message the supervisor first unregisters itself.
    verify(registrationResult).unregister()

    // Supervisor registers itself after successful TLA initialization. The TLA initialized successfully twice during this test.
    verify(locationService, times(2)).register(akkaRegistration)
  }

  private def createSupervisorAndStartTLA(
      testMocks: FrameworkTestMocks,
      componentHandlers: ComponentHandlers[ComponentDomainMessage]
  ): Unit = {
    import testMocks._

    val componentBehaviorFactory = mock[ComponentBehaviorFactory[ComponentDomainMessage]]
    when(
      componentBehaviorFactory.make(
        any[ComponentInfo],
        any[ActorRef[FromComponentLifecycleMessage]],
        any[ActorRef[PublisherMessage[CurrentState]]],
        any[LocationService]
      )
    ).thenCallRealMethod()

    when(
      componentBehaviorFactory.handlers(
        any[ActorContext[ComponentMessage]],
        any[ComponentInfo],
        any[ActorRef[PublisherMessage[CurrentState]]],
        any[LocationService]
      )
    ).thenReturn(componentHandlers)

    val supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(mock[ActorRef[ContainerIdleMessage]]),
      hcdInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory,
      componentBehaviorFactory
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = untypedSystem.spawnAnonymous(supervisorBehavior)
  }

  private def createComponentHandlers(testMocks: FrameworkTestMocks) = {
    import testMocks._

    createAnswers(compStateProbe)

    val componentHandlers = mock[SampleComponentHandlers]
    when(componentHandlers.initialize()).thenAnswer(initializeAnswer)
    when(componentHandlers.onShutdown()).thenAnswer(shutdownAnswer)
    when(componentHandlers.onRun()).thenAnswer(runAnswer)
    componentHandlers
  }

  private def createAnswers(compStateProbe: TestProbe[PubSub[CurrentState]]): Unit = {
    initializeAnswer = (_) ⇒
      Future.successful(compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    shutdownAnswer = (_) ⇒
      Future.successful(compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

    runAnswer = (_) ⇒
      Future.successful(compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
  }
}
