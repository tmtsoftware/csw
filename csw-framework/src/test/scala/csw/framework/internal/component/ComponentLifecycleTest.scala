package csw.framework.internal.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, PostStop}
import csw.framework.FrameworkTestMocks.MutableActorMock
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{ComponentInfos, FrameworkTestSuite}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommandValidationResponses.Accepted
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.IdleMessage.Initialize
import csw.messages.RunningMessage.Lifecycle
import csw.messages._
import csw.messages.ccs.commands.{Observe, Setup}
import csw.messages.params.generics.KeyType
import csw.messages.params.models.{ObsId, Prefix}
import csw.services.location.scaladsl.LocationService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

//DEOPSCSW-177-Hooks for lifecycle management
//DEOPSCSW-179-Unique Action for a component
class ComponentLifecycleTest extends FrameworkTestSuite with MockitoSugar {

  class RunningComponent(supervisorProbe: TestProbe[FromComponentLifecycleMessage]) {
    private val ctx = new StubbedActorContext[ComponentMessage]("test-component", 100, system)

    val locationService: LocationService                            = mock[LocationService]
    val sampleHcdHandler: ComponentHandlers[ComponentDomainMessage] = mock[ComponentHandlers[ComponentDomainMessage]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    when(sampleHcdHandler.onShutdown()).thenReturn(Future.unit)
    val behavior =
      new ComponentBehavior[ComponentDomainMessage](
        ctx,
        ComponentInfos.hcdInfo,
        supervisorProbe.ref,
        sampleHcdHandler,
        locationService
      ) with MutableActorMock[ComponentMessage]

    val runningComponentBehavior: ComponentBehavior[ComponentDomainMessage] = {
      behavior.onMessage(Initialize)
      supervisorProbe.expectMsgType[Running]
      behavior
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

    val obsId: ObsId = ObsId("Obs001")
    val sc1          = Setup(obsId, Prefix("wfos.prog.cloudcover")).add(KeyType.IntKey.make("encoder").set(22))

    when(sampleHcdHandler.onSubmit(ArgumentMatchers.any[Setup](), ArgumentMatchers.any[ActorRef[CommandResponse]]()))
      .thenReturn(Accepted)

    runningComponentBehavior.onMessage(Submit(sc1, commandResponseProbe.ref))

    verify(sampleHcdHandler).onSubmit(sc1, commandResponseProbe.ref)
    commandResponseProbe.expectMsg(Accepted)
  }

  test("A running component should handle Oneway command") {
    val supervisorProbe      = TestProbe[FromComponentLifecycleMessage]
    val commandResponseProbe = TestProbe[CommandResponse]
    val runningComponent     = new RunningComponent(supervisorProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1          = Observe(obsId, Prefix("wfos.prog.cloudcover")).add(KeyType.IntKey.make("encoder").set(22))

    when(sampleHcdHandler.onOneway(ArgumentMatchers.any[Setup]())).thenReturn(Accepted)

    runningComponentBehavior.onMessage(Oneway(sc1, commandResponseProbe.ref))

    verify(sampleHcdHandler).onOneway(sc1)
    commandResponseProbe.expectMsg(Accepted)
  }
}
