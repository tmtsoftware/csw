package csw.framework.internal.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, PostStop}
import csw.framework.FrameworkTestMocks.TypedActorMock
import csw.framework.{ComponentInfos, FrameworkTestSuite}
import csw.framework.scaladsl.ComponentHandlers
import csw.param.commands.{Observe, Setup}
import csw.param.generics.KeyType
import csw.param.messages.CommandMessage.{Oneway, Submit}
import csw.param.messages.FromComponentLifecycleMessage.{Initialized, Running}
import csw.param.messages.IdleMessage.Initialize
import csw.param.messages.InitialMessage.Run
import csw.param.messages._
import csw.param.messages.RunningMessage.Lifecycle
import csw.param.models.{Prefix, Validations}
import csw.services.location.scaladsl.LocationService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

//DEOPSCSW-177-Hooks for lifecycle management
//DEOPSCSW-179-Unique Action for a component
class ComponentLifecycleTest extends FrameworkTestSuite with MockitoSugar {

  class IdleComponent(supervisorProbe: TestProbe[FromComponentLifecycleMessage]) {
    private val ctx = new StubbedActorContext[ComponentMessage]("test-component", 100, system)

    val locationService: LocationService                            = mock[LocationService]
    val sampleHcdHandler: ComponentHandlers[ComponentDomainMessage] = mock[ComponentHandlers[ComponentDomainMessage]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    when(sampleHcdHandler.onRun()).thenReturn(Future.unit)
    when(sampleHcdHandler.onShutdown()).thenReturn(Future.unit)
    val behavior =
      new ComponentBehavior[ComponentDomainMessage](
        ctx,
        ComponentInfos.hcdInfo,
        supervisorProbe.ref,
        sampleHcdHandler,
        locationService
      ) with TypedActorMock[ComponentMessage]

    val idleComponentBehavior: ComponentBehavior[ComponentDomainMessage] = {
      behavior.onMessage(Initialize)
      supervisorProbe.expectMsgType[Initialized]
      behavior
    }
  }

  class RunningComponent(supervisorProbe: TestProbe[FromComponentLifecycleMessage])
      extends IdleComponent(supervisorProbe) {
    val runningComponentBehavior: ComponentBehavior[ComponentDomainMessage] = {
      idleComponentBehavior.onMessage(Run)
      supervisorProbe.expectMsgType[Running]
      idleComponentBehavior
    }
  }

  test("A running component should handle RunOffline lifecycle message") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(true)

    val previousComponentLifecyleState = runningComponentBehavior.lifecycleState
    runningComponentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    verify(sampleHcdHandler).onGoOffline()
    verify(sampleHcdHandler).isOnline
    previousComponentLifecyleState shouldBe runningComponentBehavior.lifecycleState
  }

  test("A running component should not accept RunOffline lifecycle message when it is already offline") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    val previousComponentLifecyleState = runningComponentBehavior.lifecycleState
    runningComponentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    verify(sampleHcdHandler, never).onGoOffline()
    previousComponentLifecyleState shouldBe runningComponentBehavior.lifecycleState
  }

  test("A running component should handle RunOnline lifecycle message when it is Offline") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    val previousComponentLifecyleState = runningComponentBehavior.lifecycleState
    runningComponentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    verify(sampleHcdHandler).onGoOnline()
    previousComponentLifecyleState shouldBe runningComponentBehavior.lifecycleState
  }

  test("A running component should not accept RunOnline lifecycle message when it is already Online") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._

    when(sampleHcdHandler.isOnline).thenReturn(true)

    runningComponentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    verify(sampleHcdHandler, never).onGoOnline()
  }

  test("A running component should clean up using onShutdown handler before stopping") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._

    runningComponentBehavior.onSignal(PostStop)
    verify(sampleHcdHandler).onShutdown()
  }

  test("A running component should handle Submit command") {
    val supervisorProbe      = TestProbe[FromComponentLifecycleMessage]
    val commandResponseProbe = TestProbe[CommandResponse]
    val runningComponent     = new RunningComponent(supervisorProbe)
    import runningComponent._

    val sc1 = Setup("Obs001", Prefix("wfos.prog.cloudcover")).add(KeyType.IntKey.make("encoder").set(22))

    when(sampleHcdHandler.onControlCommand(ArgumentMatchers.any[Submit]())).thenReturn(Validations.Valid)

    runningComponentBehavior.onMessage(Submit(sc1, commandResponseProbe.ref))

    verify(sampleHcdHandler).onControlCommand(Submit(sc1, commandResponseProbe.ref))
    commandResponseProbe.expectMsg(Accepted)
  }

  test("A running component should handle Oneway command") {
    val supervisorProbe      = TestProbe[FromComponentLifecycleMessage]
    val commandResponseProbe = TestProbe[CommandResponse]
    val runningComponent     = new RunningComponent(supervisorProbe)
    import runningComponent._

    val sc1 = Observe("Obs001", Prefix("wfos.prog.cloudcover")).add(KeyType.IntKey.make("encoder").set(22))

    when(sampleHcdHandler.onControlCommand(ArgumentMatchers.any[Submit]())).thenReturn(Validations.Valid)

    runningComponentBehavior.onMessage(Submit(sc1, commandResponseProbe.ref))

    verify(sampleHcdHandler).onControlCommand(Oneway(sc1, ArgumentMatchers.any[ActorRef[AnyRef]]()))
    commandResponseProbe.expectMsg(Accepted)
  }
}
