package csw.common.framework.internal.supervisor

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.SampleComponentState._
import csw.common.components.{ComponentDomainMessage, SampleComponentHandlers}
import csw.common.framework.ComponentInfos._
import csw.common.framework.exceptions.{FailureRestart, FailureStop}
import csw.common.framework.models.PubSub.{Publish, PublisherMessage}
import csw.common.framework.models.SupervisorCommonMessage.GetSupervisorLifecycleState
import csw.common.framework.models._
import csw.common.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.common.framework.{FrameworkTestMocks, FrameworkTestSuite}
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
    doThrow(FailureStop()).doAnswer(initializeAnswer).when(componentHandlers).initialize()
    createSupervisorAndStartTLA(testMocks, componentHandlers)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

    supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)
    supervisorLifecycleStateProbe.expectMsg(SupervisorLifecycleState.Idle)

    supervisorRef ! Restart

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    verify(locationService).register(akkaRegistration)
    verify(registrationResult, never()).unregister()
  }

  test("handle TLA failure with FailureRestart exception in initialize") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers = createComponentHandlers(testMocks)
    doThrow(FailureRestart()).doAnswer(initializeAnswer).when(componentHandlers).initialize()
    createSupervisorAndStartTLA(testMocks, componentHandlers)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    verify(locationService).register(akkaRegistration)
    verify(registrationResult, never()).unregister()

  }

  test("handle external restart when TLA throws FailureStop exception in onRun") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers = createComponentHandlers(testMocks)
    doThrow(FailureStop()).doAnswer(runAnswer).when(componentHandlers).onRun()

    createSupervisorAndStartTLA(testMocks, componentHandlers)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

    supervisorRef ! Restart

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    verify(locationService, times(2)).register(akkaRegistration)
    verify(registrationResult).unregister()
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
    when(componentHandlers.componentName).thenReturn(hcdInfo.name)
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
