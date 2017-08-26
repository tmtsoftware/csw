package csw.common.framework.internal

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import akka.typed.{ActorRef, ActorSystem}
import akka.{actor, testkit, Done}
import csw.common.framework.FrameworkComponentTestInfos._
import csw.common.framework.models.ContainerCommonMsg.GetComponents
import csw.common.framework.models.SupervisorCommonMsg.{GetSupervisorMode, LifecycleStateSubscription}
import csw.common.framework.models.ContainerIdleMsg.{RegistrationComplete, RegistrationFailed, SupervisorModeChanged}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.ContainerRunningMsg.{UnRegistrationComplete, UnRegistrationFailed}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.models._
import csw.common.framework.scaladsl.{SupervisorBehaviorFactory, SupervisorFactory}
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, RegistrationFactory}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

//DEOPSCSW-182-Control Life Cycle of Components
//DEOPSCSW-216-Locate and connect components to send AKKA commands
class ContainerTest extends FunSuite with Matchers with MockitoSugar {
  implicit val untypedSystem: actor.ActorSystem      = ActorSystemFactory.remote()
  implicit val typedSystem: ActorSystem[Nothing]     = untypedSystem.toTyped
  implicit val settings: TestKitSettings             = TestKitSettings(typedSystem)
  private val akkaRegistration                       = AkkaRegistration(mock[AkkaConnection], testkit.TestProbe("test-probe").testActor)
  private val locationService: LocationService       = mock[LocationService]
  private val registrationResult: RegistrationResult = mock[RegistrationResult]

  class IdleContainer() {
    val ctx                                  = new StubbedActorContext[ContainerMsg]("test-container", 100, typedSystem)
    val supervisorFactory: SupervisorFactory = mock[SupervisorFactory]
    val answer = new Answer[SupervisorInfo] {
      override def answer(invocation: InvocationOnMock): SupervisorInfo = {
        val componentInfo = invocation.getArgument(0).asInstanceOf[ComponentInfo]
        SupervisorInfo(
          untypedSystem,
          ctx.spawn(SupervisorBehaviorFactory.behavior(componentInfo, locationService, registrationFactory),
                    componentInfo.name),
          componentInfo
        )
      }
    }

    when(supervisorFactory.make(ArgumentMatchers.any[ComponentInfo]())).thenAnswer(answer)

    private val registrationFactory: RegistrationFactory = mock[RegistrationFactory]
    when(registrationFactory.akkaTyped(ArgumentMatchers.any[AkkaConnection], ArgumentMatchers.any[ActorRef[_]]))
      .thenReturn(akkaRegistration)

    private val eventualRegistrationResult: Future[RegistrationResult] =
      Promise[RegistrationResult].complete(Success(registrationResult)).future
    private val eventualDone: Future[Done] = Promise[Done].complete(Success(Done)).future

    when(locationService.register(akkaRegistration)).thenReturn(eventualRegistrationResult)
    when(registrationResult.unregister()).thenReturn(eventualDone)

    val container = new Container(ctx, containerInfo, supervisorFactory, registrationFactory, locationService)
  }

  class RunningContainer() extends IdleContainer {
    ctx.children.map(
      child ⇒ container.onMessage(SupervisorModeChanged(LifecycleStateChanged(child.upcast, SupervisorMode.Running)))
    )

    container.onMessage(RegistrationComplete(registrationResult))

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveAll())
  }

  test("should start in initialize mode and should not accept any outside messages") {
    val idleContainer = new IdleContainer
    import idleContainer._

    container.mode shouldBe ContainerMode.Idle
  }

  test("should change its mode to running after all components move to running mode") {
    val idleContainer = new IdleContainer
    import idleContainer._

    // supervisor per component + lifecycleStateTrackerRef
    ctx.children.size shouldBe (containerInfo.components.size + 1)
    container.supervisors.size shouldBe 2
    container.supervisors.map(_.componentInfo).toSet shouldBe containerInfo.components

    // check that all components received LifecycleStateSubscription message and GetSupervisorMode message
    val supervisorInboxes =
      containerInfo.components.toList.map(componentInfo ⇒ ctx.childInbox[SupervisorCommonMsg](componentInfo.name))

    supervisorInboxes.map(_.receiveMsg()) should contain only LifecycleStateSubscription(
      Subscribe(container.lifecycleStateTrackerRef)
    )

    supervisorInboxes.map(_.receiveMsg()) should contain only GetSupervisorMode(ctx.self)

    // simulate that container receives LifecycleStateChanged to Running message from all components
    ctx.children.map(
      child ⇒ container.onMessage(SupervisorModeChanged(LifecycleStateChanged(child.upcast, SupervisorMode.Running)))
    )

    // check that lifecycleStateTrackerRef gets un-subscribed from all components
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorCommonMsg](component.name))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(
      Unsubscribe(container.lifecycleStateTrackerRef)
    )

    verify(locationService).register(akkaRegistration)

    container.onMessage(RegistrationComplete(registrationResult))

    container.mode shouldBe ContainerMode.Running
    container.registrationOpt.get shouldBe registrationResult
    container.runningComponents shouldBe Set.empty
  }

  test("should handle Shutdown message by changing it's mode to Idle and forwarding the message to all components") {
    val runningContainer = new RunningContainer
    import runningContainer._

    container.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))
    verify(registrationResult).unregister()
    container.onMessage(UnRegistrationComplete)

    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(ToComponentLifecycleMessage.Shutdown)

    container.mode shouldBe ContainerMode.Idle
  }

  test("should handle restart message by changing its mode to initialize and subscribes to all components") {
    val runningContainer = new RunningContainer
    import runningContainer._

    container.runningComponents shouldBe Set.empty
    container.onMessage(Lifecycle(Restart))
    container.mode shouldBe ContainerMode.Idle

    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorCommonMsg](component.name))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(Subscribe(container.lifecycleStateTrackerRef))

    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(Restart)
  }

  test("should change its mode from restarting to running after all components have restarted") {
    val runningContainer = new RunningContainer
    import runningContainer._

    container.onMessage(Lifecycle(Restart))

    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox(component.name))
      .map(_.receiveAll())

    ctx.children.map(
      child ⇒ container.onMessage(SupervisorModeChanged(LifecycleStateChanged(child.upcast, SupervisorMode.Running)))
    )

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(
      Unsubscribe(container.lifecycleStateTrackerRef)
    )

    verify(locationService, atLeastOnce()).register(akkaRegistration)

    container.onMessage(RegistrationComplete(registrationResult))

    container.mode shouldBe ContainerMode.Running
  }

  test("should handle GoOnline and GoOffline Lifecycle messages by forwarding to all components") {
    val runningContainer = new RunningContainer
    import runningContainer._

    val initialMode = container.mode

    container.onMessage(Lifecycle(GoOnline))
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(GoOnline)

    initialMode shouldBe container.mode

    container.onMessage(Lifecycle(GoOffline))
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(GoOffline)

    initialMode shouldBe container.mode
  }

  test("container should be able to handle GetAllComponents message by responding with list of all components") {
    val idleContainer = new IdleContainer
    import idleContainer._

    // Container should handle GetComponents message in Idle mode
    container.mode shouldBe ContainerMode.Idle
    val probe = TestProbe[Components]

    container.onMessage(GetComponents(probe.ref))

    probe.expectMsg(Components(container.supervisors))

    // Container should handle GetComponents message in Running mode
    ctx.children.map(
      child ⇒ container.onMessage(SupervisorModeChanged(LifecycleStateChanged(child.upcast, SupervisorMode.Running)))
    )

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveAll())

    verify(locationService, atLeastOnce()).register(akkaRegistration)

    container.onMessage(RegistrationComplete(registrationResult))

    container.mode shouldBe ContainerMode.Running

    container.onMessage(GetComponents(probe.ref))

    probe.expectMsg(Components(container.supervisors))
  }

  test("container should retain mode if registration with location service fails") {
    val idleContainer = new IdleContainer
    import idleContainer._

    val runtimeException = mock[RuntimeException]
    val failedResult     = Promise[RegistrationResult].complete(Failure(runtimeException)).future
    when(locationService.register(akkaRegistration)).thenReturn(failedResult)

    container.mode shouldBe ContainerMode.Idle

    // simulate that container receives LifecycleStateChanged to Running message from all components
    ctx.children.map(
      child ⇒ container.onMessage(SupervisorModeChanged(LifecycleStateChanged(child.upcast, SupervisorMode.Running)))
    )

    verify(locationService, atLeastOnce()).register(akkaRegistration)
    container.onMessage(RegistrationFailed(runtimeException))
    container.mode shouldBe ContainerMode.Idle
    container.registrationOpt shouldBe None
    container.runningComponents should not be Set.empty
  }

  test("container should retain mode if un-registration with location service fails") {
    val runningContainer = new RunningContainer
    import runningContainer._

    val runtimeException = mock[RuntimeException]
    val failedResult     = Promise[Done].complete(Failure(runtimeException)).future
    when(registrationResult.unregister()).thenReturn(failedResult)

    container.mode shouldBe ContainerMode.Running

    container.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))

    verify(registrationResult, atLeastOnce()).unregister()
    container.onMessage(UnRegistrationFailed(runtimeException))
    container.mode shouldBe ContainerMode.Running
    container.registrationOpt should not be None
  }
}
