/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.supervisor

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.ActorRef
import csw.command.client.messages.CommandMessage.Submit
import csw.command.client.messages.ComponentCommonMessage.{
  ComponentStateSubscription,
  GetSupervisorLifecycleState,
  LifecycleStateSubscription
}
import csw.command.client.messages.SupervisorContainerCommonMessages.Restart
import csw.command.client.messages.{ComponentMessage, ContainerIdleMessage}
import csw.command.client.models.framework.{LifecycleStateChanged, PubSub, SupervisorLifecycleState}
import csw.common.FrameworkAssertions.*
import csw.common.components.framework.SampleComponentState.*
import csw.common.utils.TestAppender
import csw.framework.ComponentInfos.*
import csw.framework.exceptions.{FailureRestart, FailureStop}
import csw.framework.internal.component.ComponentBehavior
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.logging.client.commons.PekkoTypedExtension.UserActorFactory
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggerFactory
import csw.logging.models.Level.ERROR
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, ControlCommand, Setup}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.{Id, ObsId}
import csw.params.core.states.{CurrentState, StateName}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{doThrow, verify, when}
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

// DEOPSCSW-178: Lifecycle success/failure notification
// DEOPSCSW-181: Multiple Examples for Lifecycle Support
// CSW-86: Subsystem should be case-insensitive
class SupervisorLifecycleFailureTest extends FrameworkTestSuite with BeforeAndAfterEach {

  val supervisorLifecycleStateProbe: TestProbe[SupervisorLifecycleState] = TestProbe[SupervisorLifecycleState]()
  var supervisorRef: ActorRef[ComponentMessage]                          = scala.compiletime.uninitialized
  var initializeAnswer: Answer[Unit]                                     = scala.compiletime.uninitialized
  var submitAnswer: Answer[Future[Unit]]                                 = scala.compiletime.uninitialized
  var shutdownAnswer: Answer[Unit]                                       = scala.compiletime.uninitialized
  var runAnswer: Answer[Future[Unit]]                                    = scala.compiletime.uninitialized

  implicit val ec: ExecutionContext = typedSystem.executionContext

  // all log messages will be captured in log buffer
  private val logBuffer                    = mutable.Buffer.empty[JsObject]
  private val testAppender                 = new TestAppender(x => logBuffer += Json.parse(x.toString).as[JsObject])
  private var loggingSystem: LoggingSystem = scala.compiletime.uninitialized

  override protected def beforeAll(): Unit = {
    loggingSystem = new LoggingSystem("sup-failure", "1.0", "localhost", typedSystem)
    loggingSystem.setAppenders(List(testAppender))
  }

  override protected def afterEach(): Unit = logBuffer.clear()

  test(
    "handle TLA failure with FailureStop exception in initialize with Restart message | DEOPSCSW-178, DEOPSCSW-181, DEOPSCSW-180"
  ) {
    val testMocks = frameworkTestMocks()
    import testMocks.*

    val componentHandlers = createComponentHandlers(testMocks)

    val failureStopExMsg = "testing FailureStop"
    // Throw a `FailureStop` on the first attempt to initialize but initialize successfully on the next attempt
    doThrow(TestFailureStop(failureStopExMsg)).doAnswer(initializeAnswer).when(componentHandlers).initialize()

    createSupervisorAndStartTLA(testMocks, componentHandlers)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // Component fails to initialize with `FailureStop`. The default pekko supervision strategy kills the TLA
    // and triggers the PostStop signal of TLA. The post stop signal has `onShutdown` handler of the component which is
    // mocked in the test to publish a `shutdownChoice`.
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))

    // DEOPSCSW-180: Generic and Specific Log messages
    // component handlers initialize block throws FailureStop exception which we expect pekko logs it
    Thread.sleep(100)
    assertThatExceptionIsLogged(
      logBuffer,
      "WFOS",
      "SampleHcd",
      failureStopExMsg,
      ERROR,
      ComponentBehavior.getClass.getName,
      TestFailureStop.getClass.getName,
      failureStopExMsg
    )

    supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)

    // Supervisor is still in the Idle lifecycle state
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Idle)

    // External entity sends restart to supervisor with the intent to restart TLA
    supervisorRef ! Restart

    // TLA initialises successfully the second time due to the defined mock behavior. The `initialize` handler of the
    // component is mocked in the test to publish a `initChoice` in the second attempt.
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))

    eventually(lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Restart)))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    eventually(lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    // Supervisor registers itself only after successful initialization of TLA. In this test the TLA successfully
    // initializes after Restart message, after which Supervisor registers itself.
    verify(locationService).register(pekkoRegistration)

    // On receiving Restart message the supervisor first unregisters itself. But in the case of initialization failures
    // it never registers itself and thus never unregisters as well on receiving Restart message.
    verify(registrationResult, Mockito.never()).unregister()
  }

  test("handle TLA failure with FailureRestart exception in initialize | DEOPSCSW-178, DEOPSCSW-181, DEOPSCSW-180") {
    val testMocks = frameworkTestMocks()
    import testMocks.*

    val componentHandlers   = createComponentHandlers(testMocks)
    val failureRestartExMsg = "testing FailureRestart"

    // Throw a `FailureRestart` on the first attempt to initialize but initialize successfully on the next attempt
    doThrow(TestFailureRestart(failureRestartExMsg)).doAnswer(initializeAnswer).when(componentHandlers).initialize()
    createSupervisorAndStartTLA(testMocks, componentHandlers)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // component fails to initialize with `FailureRestart`. The pekko supervision strategy specified in SupervisorBehavior
    // restarts the TLA. The `initialize` handler of the component is mocked in the test to publish a `initChoice` in the second attempt.
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMessage(5.seconds, LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    Thread.sleep(100)
    // DEOPSCSW-180: Generic and Specific Log messages
    // component handlers initialize block throws FailureRestart exception which we expect pekko logs it
    assertThatExceptionIsLogged(
      logBuffer,
      "WFOS",
      "SampleHcd",
      failureRestartExMsg,
      ERROR,
      ComponentBehavior.getClass.getName,
      TestFailureRestart.getClass.getName,
      failureRestartExMsg
    )

    // Supervisor registers itself only after successful initialization of TLA. In this test the TLA successfully
    // initializes after Restart message after which Supervisor registers itself
    verify(locationService).register(pekkoRegistration)
  }

  // DEOPSCSW-294 : FailureRestart exception from onDomainMsg, onSetup or onObserve component handlers results into unexpected message to supervisor
  test("handle TLA failure with FailureRestart exception in Running | DEOPSCSW-178, DEOPSCSW-181, DEOPSCSW-294") {
    val testMocks = frameworkTestMocks()
    import testMocks.*

    val componentHandlers   = createComponentHandlers(testMocks)
    val failureRestartExMsg = "testing FailureRestart"
    val unexpectedMessage   = "Unexpected message :[Running"

    val obsId: ObsId          = ObsId("2020A-001-123")
    val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
    val setup: Setup          = Setup(prefix, CommandName("move"), Some(obsId), Set(param))

    doThrow(TestFailureRestart(failureRestartExMsg))
      .when(componentHandlers)
      .validateCommand(any[Id], any[ControlCommand])

    createSupervisorAndStartTLA(testMocks, componentHandlers)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // component Initializes successfully
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMessage(5.seconds, LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    // Supervisor sends component a submit command which will fail with FailureRestart exception on calling onSubmit Handler
    supervisorRef ! Submit(setup, TestProbe[SubmitResponse]().ref)

    // Component initializes again by the pekko framework without termination
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))

    supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)

    // Supervisor is still in the Running lifecycle state
    supervisorLifecycleStateProbe.expectMessage(5.seconds, SupervisorLifecycleState.Running)

    Thread.sleep(100)

    // Assert that the error log statement of type "Unexpected message received in Running lifecycle state" is not generated
    assertThatExceptionIsNotLogged(logBuffer, unexpectedMessage)
  }

  private def createSupervisorAndStartTLA(
      testMocks: FrameworkTestMocks,
      componentHandlers: ComponentHandlers
  ): Unit = {
    import testMocks.*

    val supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(mock[ActorRef[ContainerIdleMessage]]),
      registrationFactory,
      (_, _) => componentHandlers,
      new CswContext(
        cswCtx.locationService,
        cswCtx.eventService,
        cswCtx.alarmService,
        cswCtx.timeServiceScheduler,
        new LoggerFactory(hcdInfo.prefix),
        cswCtx.configClientService,
        currentStatePublisher,
        commandResponseManager,
        hcdInfo
      )
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = typedSystem.spawn(supervisorBehavior, "")
  }

  private def createComponentHandlers(testMocks: FrameworkTestMocks) = {
    import testMocks.*

    createAnswers(compStateProbe)

    val componentHandlers = mock[ComponentHandlers]
    when(componentHandlers.initialize()).thenAnswer(initializeAnswer.answer(_))
    when(componentHandlers.onShutdown()).thenAnswer(shutdownAnswer.answer(_))
    componentHandlers
  }

  private def createAnswers(compStateProbe: TestProbe[CurrentState]): Unit = {
    initializeAnswer = _ => {
      // small sleep is required in order for test probe to subscribe for component state and lifecycle state
      // before component actually gets initialized
      Thread.sleep(200)
      compStateProbe.ref ! CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice)))
    }

    shutdownAnswer = _ =>
      compStateProbe.ref ! CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice)))
  }
}

case class TestFailureStop(msg: String)    extends FailureStop(msg)
case class TestFailureRestart(msg: String) extends FailureRestart(msg)
