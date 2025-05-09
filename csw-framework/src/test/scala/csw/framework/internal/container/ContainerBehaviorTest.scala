/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.container

import org.apache.pekko.Done
import org.apache.pekko.actor.testkit.typed.Effect.Watched
import org.apache.pekko.actor.testkit.typed.TestKitSettings
import org.apache.pekko.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorRefResolver, ActorSystem, SpawnProtocol}
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.command.client.messages.ContainerIdleMessage.SupervisorsCreated
import csw.command.client.messages.FromSupervisorMessage.SupervisorLifecycleStateChanged
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.Restart
import csw.command.client.messages.{ComponentMessage, ContainerActorMessage, ContainerIdleMessage}
import csw.command.client.models.framework.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.command.client.models.framework.{ComponentInfo, ContainerLifecycleState, SupervisorLifecycleState, _}
import csw.event.client.EventServiceFactory
import csw.framework.ComponentInfos.*
import csw.framework.FrameworkTestMocks
import csw.framework.internal.supervisor.SupervisorInfoFactory
import csw.framework.scaladsl.RegistrationFactory
import csw.location.api.PekkoRegistrationFactory
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoRegistration, Metadata}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{Future, Promise}
import scala.util.Success

//DEOPSCSW-182-Control Life Cycle of Components
//DEOPSCSW-216-Locate and connect components to send PEKKO commands
class ContainerBehaviorTest extends AnyFunSuite with Matchers with MockitoSugar {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "test")
  implicit val settings: TestKitSettings                       = TestKitSettings(typedSystem)
  private val mocks                                            = new FrameworkTestMocks()

  class IdleContainer() {
    private val testActor: ActorRef[Any]       = TestProbe("test-probe").ref
    private val metadata: Metadata             = Metadata(Map("key1" -> "value1"))
    val pekkoRegistration: PekkoRegistration   = PekkoRegistrationFactory.make(mock[PekkoConnection], testActor, metadata)
    val locationService: LocationService       = mock[LocationService]
    val eventService: EventServiceFactory      = mock[EventServiceFactory]
    val alarmService: AlarmServiceFactory      = mock[AlarmServiceFactory]
    val registrationResult: RegistrationResult = mock[RegistrationResult]
    private[container] var supervisorInfos: Set[SupervisorInfo] = Set.empty
    var componentProbes: Set[TestProbe[ComponentMessage]]       = Set.empty
    val supervisorInfoFactory: SupervisorInfoFactory            = mock[SupervisorInfoFactory]
    val actorRefResolver: ActorRefResolver                      = mock[ActorRefResolver]

    private def answer(ci: ComponentInfo): Future[Some[SupervisorInfo]] = {
      val componentProbe: TestProbe[ComponentMessage] = TestProbe(ci.prefix.toString)
      val supervisorInfo                              = SupervisorInfo(typedSystem, Component(componentProbe.ref, ci))

      supervisorInfos += SupervisorInfo(typedSystem, Component(componentProbe.ref, ci))
      componentProbes += componentProbe
      Future.successful(Some(supervisorInfo))
    }

    when(
      supervisorInfoFactory
        .make(
          any[ActorRef[ContainerIdleMessage]],
          any[ComponentInfo],
          any[LocationService],
          any[EventServiceFactory],
          any[AlarmServiceFactory],
          any[RegistrationFactory]
        )
    ).thenAnswer(arg => answer(arg.getArgument(1, classOf[ComponentInfo])))
    when(actorRefResolver.resolveActorRef(any[String])).thenReturn(TestProbe().ref)

    private val registrationFactory: RegistrationFactory = mock[RegistrationFactory]

    when(registrationFactory.pekkoTyped(any[PekkoConnection], any[ActorRef[?]], any[Metadata]))
      .thenReturn(pekkoRegistration)

    private val eventualRegistrationResult: Future[RegistrationResult] =
      Promise[RegistrationResult]().complete(Success(registrationResult)).future
    private val eventualDone: Future[Done] = Promise[Done]().complete(Success(Done)).future

    when(locationService.register(pekkoRegistration)).thenReturn(eventualRegistrationResult)
    when(registrationResult.unregister()).thenReturn(eventualDone)

    val containerBehaviorTestkit: BehaviorTestKit[ContainerActorMessage] = BehaviorTestKit(
      Behaviors.setup(ctx =>
        new ContainerBehavior(
          ctx,
          containerInfo,
          supervisorInfoFactory,
          registrationFactory,
          locationService,
          eventService,
          alarmService,
          mocks.loggerFactory,
          actorRefResolver
        )
      )
    )
  }

  class RunningContainer() extends IdleContainer {
    containerBehaviorTestkit.run(SupervisorsCreated(supervisorInfos))
    val components = Components(supervisorInfos.map(_.component))

    components.components.foreach(component =>
      containerBehaviorTestkit.run(SupervisorLifecycleStateChanged(component.supervisor, SupervisorLifecycleState.Running))
    )
  }

  test("should watch the components created in the container | DEOPSCSW-182, DEOPSCSW-216") {
    val runningContainer = new RunningContainer
    import runningContainer._

    containerBehaviorTestkit
      .retrieveAllEffects() shouldBe components.components
      .map(component => Watched(component.supervisor))
      .toList
  }

  test("should handle restart message by changing its lifecycle state to Idle | DEOPSCSW-182, DEOPSCSW-216") {
    val runningContainer = new RunningContainer
    import runningContainer._
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]()

    // verify that Container is in Running state before sending Restart
    containerBehaviorTestkit.run(GetContainerLifecycleState(containerLifecycleStateProbe.ref))
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Running)

    containerBehaviorTestkit.run(Restart)

    // verify that Container changes its state to Idle after Restart
    containerBehaviorTestkit.run(GetContainerLifecycleState(containerLifecycleStateProbe.ref))
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Idle)

    // verify that Container sends Restart message to all component supervisors
    componentProbes.map(_.expectMessage(Restart))
  }

  test(
    "should change its lifecycle state from Idle to Running after all components have restarted | DEOPSCSW-182, DEOPSCSW-216"
  ) {
    val runningContainer = new RunningContainer
    import runningContainer._
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]()

    containerBehaviorTestkit.run(Restart)

    // verify that Container changes its state to Idle after Restart
    containerBehaviorTestkit.run(GetContainerLifecycleState(containerLifecycleStateProbe.ref))
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Idle)

    // simulate that container receives LifecycleStateChanged to Running message from all components
    components.components.foreach(component =>
      containerBehaviorTestkit.run(SupervisorLifecycleStateChanged(component.supervisor, SupervisorLifecycleState.Running))
    )

    // verify that Container changes its state to Running after all component supervisors change their state to Running
    containerBehaviorTestkit.run(GetContainerLifecycleState(containerLifecycleStateProbe.ref))
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Running)

  }

  test("should handle GoOnline and GoOffline Lifecycle messages by forwarding to all components | DEOPSCSW-182, DEOPSCSW-216") {
    val runningContainer = new RunningContainer
    import runningContainer._
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]()

    containerBehaviorTestkit.run(GetContainerLifecycleState(containerLifecycleStateProbe.ref))
    val initialLifecycleState = containerLifecycleStateProbe.expectMessageType[ContainerLifecycleState]

    containerBehaviorTestkit.run(Lifecycle(GoOnline))

    // verify that Container sends GoOnline message to all component supervisors
    componentProbes.map(_.expectMessage(Lifecycle(GoOnline)))

    // verify that Container LifecycleState does not change on receiving GoOnline message
    containerBehaviorTestkit.run(GetContainerLifecycleState(containerLifecycleStateProbe.ref))
    val newLifecycleState = containerLifecycleStateProbe.expectMessage(initialLifecycleState)

    containerBehaviorTestkit.run(Lifecycle(GoOffline))

    // verify that Container sends GoOffline message to all component supervisors
    componentProbes.map(_.expectMessage(Lifecycle(GoOffline)))

    // verify that Container LifecycleState does not change on receiving GoOffline message
    containerBehaviorTestkit.run(GetContainerLifecycleState(containerLifecycleStateProbe.ref))
    containerLifecycleStateProbe.expectMessage(newLifecycleState)
  }
}
