package csw.common.framework.internal

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import akka.typed.{ActorRef, ActorSystem}
import akka.{actor, Done}
import csw.common.framework.ComponentInfos._
import csw.common.framework.internal.container.{ContainerBehavior, ContainerMode}
import csw.common.framework.internal.supervisor.{SupervisorBehaviorFactory, SupervisorInfoFactory, SupervisorMode}
import csw.common.framework.models.ContainerCommonExternalMessage.GetComponents
import csw.common.framework.models.ContainerIdleMessage.{RegistrationComplete, RegistrationFailed}
import csw.common.framework.models.FromSupervisorMessage.SupervisorModeChanged
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.common.framework.models._
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
class ContainerBehaviorTest extends FunSuite with Matchers with MockitoSugar {
  implicit val untypedSystem: actor.ActorSystem      = ActorSystemFactory.remote()
  implicit val typedSystem: ActorSystem[Nothing]     = untypedSystem.toTyped
  implicit val settings: TestKitSettings             = TestKitSettings(typedSystem)
  private val akkaRegistration                       = AkkaRegistration(mock[AkkaConnection], TestProbe("test-probe").testActor)
  private val locationService: LocationService       = mock[LocationService]
  private val registrationResult: RegistrationResult = mock[RegistrationResult]

  class IdleContainer() {
    val ctx                                      = new StubbedActorContext[ContainerMessage]("test-container", 100, typedSystem)
    val supervisorFactory: SupervisorInfoFactory = mock[SupervisorInfoFactory]
    val answer = new Answer[SupervisorInfo] {
      override def answer(invocation: InvocationOnMock): SupervisorInfo = {
        val componentInfo = invocation.getArgument[ComponentInfo](1)
        SupervisorInfo(
          untypedSystem,
          Component(
            ctx.spawn(
              SupervisorBehaviorFactory.make(Some(ctx.self), componentInfo, locationService, registrationFactory),
              componentInfo.name
            ),
            componentInfo
          )
        )
      }
    }

    when(
      supervisorFactory
        .make(ArgumentMatchers.any[ActorRef[ContainerIdleMessage]],
              ArgumentMatchers.any[ComponentInfo],
              ArgumentMatchers.any[LocationService])
    ).thenAnswer(answer)

    private val registrationFactory: RegistrationFactory = mock[RegistrationFactory]
    when(registrationFactory.akkaTyped(ArgumentMatchers.any[AkkaConnection], ArgumentMatchers.any[ActorRef[_]]))
      .thenReturn(akkaRegistration)

    private val eventualRegistrationResult: Future[RegistrationResult] =
      Promise[RegistrationResult].complete(Success(registrationResult)).future
    private val eventualDone: Future[Done] = Promise[Done].complete(Success(Done)).future

    when(locationService.register(akkaRegistration)).thenReturn(eventualRegistrationResult)
    when(registrationResult.unregister()).thenReturn(eventualDone)

    val containerBehavior =
      new ContainerBehavior(ctx, containerInfo, supervisorFactory, registrationFactory, locationService)
  }

  class RunningContainer() extends IdleContainer {
    ctx.children.map(child ⇒ containerBehavior.onMessage(SupervisorModeChanged(child.upcast, SupervisorMode.Running)))

    containerBehavior.onMessage(RegistrationComplete(registrationResult))

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveAll())
  }

  test("should start in initialize mode and should not accept any outside messages") {
    val idleContainer = new IdleContainer
    import idleContainer._

    containerBehavior.mode shouldBe ContainerMode.Idle
  }

  test("should change its mode to running after all components move to running mode") {
    val idleContainer = new IdleContainer
    import idleContainer._

    // supervisor per component
    ctx.children.size shouldBe containerInfo.components.size
    containerBehavior.supervisors.size shouldBe 2
    containerBehavior.supervisors.map(_.component.info).toSet shouldBe containerInfo.components

    // simulate that container receives LifecycleStateChanged to Running message from all components
    ctx.children.map(
      child ⇒ containerBehavior.onMessage(SupervisorModeChanged(child.upcast, SupervisorMode.Running))
    )

    verify(locationService).register(akkaRegistration)

    containerBehavior.onMessage(RegistrationComplete(registrationResult))

    containerBehavior.mode shouldBe ContainerMode.Running
    containerBehavior.registrationOpt.get shouldBe registrationResult
    containerBehavior.runningComponents shouldBe Set.empty
  }

  test("should handle restart message by changing its mode to initialize") {
    val runningContainer = new RunningContainer
    import runningContainer._

    containerBehavior.runningComponents shouldBe Set.empty
    containerBehavior.onMessage(Restart)
    containerBehavior.mode shouldBe ContainerMode.Idle

    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Restart
  }

  test("should change its mode from restarting to running after all components have restarted") {
    val runningContainer = new RunningContainer
    import runningContainer._

    containerBehavior.onMessage(Restart)

    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox(component.name))
      .map(_.receiveAll())

    ctx.children.map(child ⇒ containerBehavior.onMessage(SupervisorModeChanged(child.upcast, SupervisorMode.Running)))

    verify(locationService, atLeastOnce()).register(akkaRegistration)

    containerBehavior.onMessage(RegistrationComplete(registrationResult))

    containerBehavior.mode shouldBe ContainerMode.Running
  }

  test("should handle GoOnline and GoOffline Lifecycle messages by forwarding to all components") {
    val runningContainer = new RunningContainer
    import runningContainer._

    val initialMode = containerBehavior.mode

    containerBehavior.onMessage(Lifecycle(GoOnline))
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(GoOnline)

    initialMode shouldBe containerBehavior.mode

    containerBehavior.onMessage(Lifecycle(GoOffline))
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(GoOffline)

    initialMode shouldBe containerBehavior.mode
  }

  test("container should be able to handle GetAllComponents message by responding with list of all components") {
    val idleContainer = new IdleContainer
    import idleContainer._

    // Container should handle GetComponents message in Idle mode
    containerBehavior.mode shouldBe ContainerMode.Idle
    val probe = TestProbe[Components]

    containerBehavior.onMessage(GetComponents(probe.ref))

    probe.expectMsg(Components(containerBehavior.supervisors.map(_.component)))

    // Container should handle GetComponents message in Running mode
    ctx.children.map(child ⇒ containerBehavior.onMessage(SupervisorModeChanged(child.upcast, SupervisorMode.Running)))

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveAll())

    verify(locationService, atLeastOnce()).register(akkaRegistration)

    containerBehavior.onMessage(RegistrationComplete(registrationResult))

    containerBehavior.mode shouldBe ContainerMode.Running

    containerBehavior.onMessage(GetComponents(probe.ref))

    probe.expectMsg(Components(containerBehavior.supervisors.map(_.component)))
  }

  test("container should retain mode if registration with location service fails") {
    val idleContainer = new IdleContainer
    import idleContainer._

    val runtimeException = mock[RuntimeException]
    val failedResult     = Promise[RegistrationResult].complete(Failure(runtimeException)).future
    when(locationService.register(akkaRegistration)).thenReturn(failedResult)

    containerBehavior.mode shouldBe ContainerMode.Idle

    // simulate that container receives LifecycleStateChanged to Running message from all components
    ctx.children.map(child ⇒ containerBehavior.onMessage(SupervisorModeChanged(child.upcast, SupervisorMode.Running)))

    verify(locationService, atLeastOnce()).register(akkaRegistration)
    containerBehavior.onMessage(RegistrationFailed(runtimeException))
    containerBehavior.mode shouldBe ContainerMode.Idle
    containerBehavior.registrationOpt shouldBe None
    containerBehavior.runningComponents should not be Set.empty
  }
}
