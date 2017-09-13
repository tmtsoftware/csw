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
import csw.common.framework.models.SupervisorCommonMessage.GetSupervisorMode
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

  val supervisorModeProbe: TestProbe[SupervisorMode]     = TestProbe[SupervisorMode]
  var supervisorRef: ActorRef[SupervisorExternalMessage] = _
  var initializeAnswer: Answer[Future[Unit]]             = _
  var shutdownAnswer: Answer[Future[Unit]]               = _
  var runAnswer: Answer[Future[Unit]]                    = _

  test("handle when TLA throws FailureStop exception in initialize") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers = createComponentHandlers(testMocks)
    doThrow(FailureStop()).doAnswer(initializeAnswer).when(componentHandlers).initialize()
    createSupervisorAndStartTLA(testMocks, componentHandlers)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

    supervisorRef ! GetSupervisorMode(supervisorModeProbe.ref)
    supervisorModeProbe.expectMsg(SupervisorMode.Idle)

    supervisorRef ! Restart

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))

    verify(registrationResult).unregister()
    verify(locationService, times(2)).register(akkaRegistration)
  }

  test("handle TLA failure with FailureRestart exception in initialize") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers = createComponentHandlers(testMocks)
    doThrow(FailureRestart()).doAnswer(initializeAnswer).when(componentHandlers).initialize()
    createSupervisorAndStartTLA(testMocks, componentHandlers)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))

    verify(locationService).register(akkaRegistration)
    verify(registrationResult, never()).unregister()

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
      None,
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
    when(componentHandlers.componentName).thenReturn(hcdInfo.name)
    componentHandlers
  }

  private def createAnswers(compStateProbe: TestProbe[PubSub[CurrentState]]): Unit = {
    initializeAnswer = (_) ⇒
      Future.successful(compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    shutdownAnswer = (_) ⇒
      Future.successful(compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))
  }
}
