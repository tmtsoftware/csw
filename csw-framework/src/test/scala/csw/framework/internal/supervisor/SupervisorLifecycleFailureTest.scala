package csw.framework.internal.supervisor

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.testkit.typed.scaladsl.TestProbe
import com.persist.JsonOps
import com.persist.JsonOps.JsonObject
import csw.common.FrameworkAssertions._
import csw.common.components.framework.SampleComponentState._
import csw.common.utils.TestAppender
import csw.commons.tags.LoggingSystemSensitive
import csw.framework.ComponentInfos._
import csw.framework.internal.component.ComponentBehavior
import csw.framework.{ComponentInfos, FrameworkTestMocks, FrameworkTestSuite}
import csw.messages.commands.{CommandName, CommandResponse, Setup}
import csw.messages.framework.{ComponentInfo, LifecycleStateChanged, PubSub, SupervisorLifecycleState}
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.ObsId
import csw.messages.params.states.CurrentState
import csw.messages.scaladsl.CommandMessage.Submit
import csw.messages.scaladsl.ComponentCommonMessage.{ComponentStateSubscription, GetSupervisorLifecycleState, LifecycleStateSubscription}
import csw.messages.scaladsl.SupervisorContainerCommonMessages.Restart
import csw.messages.scaladsl.{ComponentMessage, ContainerIdleMessage}
import csw.services.logging.internal.LoggingLevels.ERROR
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.LoggerFactory
import org.mockito.Mockito._
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterEach

import scala.collection.mutable
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

// DEOPSCSW-178: Lifecycle success/failure notification
@LoggingSystemSensitive
class SupervisorLifecycleFailureTest extends FrameworkTestSuite with BeforeAndAfterEach {

  val supervisorLifecycleStateProbe: TestProbe[SupervisorLifecycleState] = TestProbe[SupervisorLifecycleState]
  var supervisorRef: ActorRef[ComponentMessage] = _
  var initializeAnswer: Answer[Future[Unit]] = _
  var submitAnswer: Answer[Future[Unit]] = _
  var shutdownAnswer: Answer[Future[Unit]] = _
  var runAnswer: Answer[Future[Unit]] = _

  implicit val ec: ExecutionContext = typedSystem.executionContext

  // all log messages will be captured in log buffer
  private val logBuffer = mutable.Buffer.empty[JsonObject]
  private val testAppender = new TestAppender(x ⇒ logBuffer += JsonOps.Json(x.toString).asInstanceOf[JsonObject])
  private var loggingSystem: LoggingSystem = _

  override protected def beforeAll(): Unit = {
    loggingSystem = new LoggingSystem("sup-failure", "1.0", "localhost", untypedSystem)
    loggingSystem.setAppenders(List(testAppender))
  }

  override protected def afterEach(): Unit = logBuffer.clear()

  test("handle TLA failure with FailureStop exception in initialize with Restart message") {
    val testMocks = frameworkTestMocks()
    import testMocks._


    val failureStopExMsg = "testing FailureStop"

    createSupervisorAndStartTLA(testMocks, ComponentInfos.initFailureHcdInfo)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // Component fails to initialize with `FailureStop`. The default akka supervision strategy kills the TLA
    // and triggers the PostStop signal of TLA. The post stop signal has `onShutdown` handler of the component which is
    // mocked in the test to publish a `shutdownChoice`.
    compStateProbe.expectMessage(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))

    // DEOPSCSW-180: Generic and Specific Log messages
    // component handlers initialize block throws FailureStop exception which we expect akka logs it
    Thread.sleep(100)
    assertThatExceptionIsLogged(
      logBuffer,
      "SampleHcd",
      failureStopExMsg,
      ERROR,
      classOf[ComponentBehavior].getName,
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
    compStateProbe.expectMessage(CurrentState(prefix, Set(choiceKey.set(initChoice))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMessage(5.seconds, LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

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

    val failureRestartExMsg = "testing FailureRestart"

    // Throw a `FailureRestart` on the first attempt to initialize but initialize successfully on the next attempt
    createSupervisorAndStartTLA(testMocks, ComponentInfos.initFailureRestartHcdInfo)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // component fails to initialize with `FailureRestart`. The akka supervision strategy specified in SupervisorBehavior
    // restarts the TLA. The `initialize` handler of the component is mocked in the test to publish a `initChoice` in the second attempt.
    compStateProbe.expectMessage(CurrentState(prefix, Set(choiceKey.set(initChoice))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMessage(5.seconds, LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    Thread.sleep(100)
    // DEOPSCSW-180: Generic and Specific Log messages
    // component handlers initialize block throws FailureRestart exception which we expect akka logs it
    assertThatExceptionIsLogged(
      logBuffer,
      "SampleHcd",
      failureRestartExMsg,
      ERROR,
      classOf[ComponentBehavior].getName,
      TestFailureRestart.getClass.getName,
      failureRestartExMsg
    )

    // Supervisor registers itself only after successful initialization of TLA. In this test the TLA successfully
    // initializes after Restart message after which Supervisor registers itself
    verify(locationService).register(akkaRegistration)
  }

  // DEOPSCSW-294 : FailureRestart exception from onDomainMsg, onSetup or onObserve component handlers results into unexpected message to supervisor
  test("handle TLA failure with FailureRestart exception in Running") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val unexpectedMessage = "Unexpected message :[Running"

    val obsId: ObsId = ObsId("Obs001")
    val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
    val setup: Setup = Setup(prefix, CommandName("move"), Some(obsId), Set(param))

    createSupervisorAndStartTLA(testMocks, ComponentInfos.validateFailureRestartHcdInfo)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // component Initializes successfully
    compStateProbe.expectMessage(CurrentState(prefix, Set(choiceKey.set(initChoice))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMessage(5.seconds, LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    // Supervisor sends component a submit command which will fail with FailureRestart exception on calling onSubmit Handler
    supervisorRef ! Submit(setup, TestProbe[CommandResponse].ref)

    // Component initializes again by the akka framework without termination
    compStateProbe.expectMessage(CurrentState(prefix, Set(choiceKey.set(initChoice))))

    supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)

    // Supervisor is still in the Running lifecycle state
    supervisorLifecycleStateProbe.expectMessage(5.seconds, SupervisorLifecycleState.Running)

    Thread.sleep(100)

    // Assert that the error log statement of type "Unexpected message received in Running lifecycle state" is not generated
    assertThatExceptionIsNotLogged(logBuffer, unexpectedMessage)
  }

  private def createSupervisorAndStartTLA(testMocks: FrameworkTestMocks,
                                           componentInfo: ComponentInfo
                                         ): Unit = {
    import testMocks._

    val supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(mock[ActorRef[ContainerIdleMessage]]),
      componentInfo,
      locationService,
      registrationFactory,
      commandResponseManagerFactory,
      new LoggerFactory(hcdInfo.name)
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = untypedSystem.spawnAnonymous(supervisorBehavior)
  }
}