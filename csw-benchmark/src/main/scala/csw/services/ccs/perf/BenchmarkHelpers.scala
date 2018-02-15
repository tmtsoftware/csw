package csw.services.ccs.perf

import akka.actor.ActorSystem
import akka.typed
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.Config
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.ComponentCommonMessage.GetSupervisorLifecycleState
import csw.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.messages.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.messages.location.{AkkaLocation, ComponentId, ComponentType}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.{ComponentMessage, ContainerExternalMessage}
import csw.services.ccs.scaladsl.CommandService
import csw.services.location.commons.BlockingUtils
import csw.services.location.scaladsl.LocationServiceFactory

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationDouble}

object BenchmarkHelpers {

  def spawnStandaloneComponent(actorSystem: ActorSystem, config: Config): CommandService = {
    val locationService                                  = LocationServiceFactory.withSystem(actorSystem)
    val wiring: FrameworkWiring                          = FrameworkWiring.make(actorSystem, locationService)
    implicit val typedSystem: typed.ActorSystem[Nothing] = actorSystem.toTyped
    implicit val settings: TestKitSettings               = TestKitSettings(typedSystem)

    val probe = TestProbe[SupervisorLifecycleState]

    Standalone.spawn(config, wiring)
    val akkaLocation: AkkaLocation =
      Await.result(locationService.resolve(AkkaConnection(ComponentId("Perf", ComponentType.HCD)), 5.seconds), 5.seconds).get

    assertThatSupervisorIsRunning(akkaLocation.componentRef, probe, 5.seconds)

    new CommandService(akkaLocation)
  }

  def assertThatContainerIsRunning(
      containerRef: ActorRef[ContainerExternalMessage],
      probe: TestProbe[ContainerLifecycleState],
      duration: Duration
  ): Unit = {
    def getContainerLifecycleState: ContainerLifecycleState = {
      containerRef ! GetContainerLifecycleState(probe.ref)
      probe.expectMsgType[ContainerLifecycleState]
    }

    assert(
      BlockingUtils.poll(getContainerLifecycleState == ContainerLifecycleState.Running, duration),
      s"expected :${ContainerLifecycleState.Running}, found :$getContainerLifecycleState"
    )
  }

  def assertThatSupervisorIsRunning(
      actorRef: ActorRef[ComponentMessage],
      probe: TestProbe[SupervisorLifecycleState],
      duration: Duration
  ): Unit = {
    def getSupervisorLifecycleState: SupervisorLifecycleState = {
      actorRef ! GetSupervisorLifecycleState(probe.ref)
      probe.expectMsgType[SupervisorLifecycleState]
    }

    assert(
      BlockingUtils.poll(getSupervisorLifecycleState == SupervisorLifecycleState.Running, duration),
      s"expected :${SupervisorLifecycleState.Running}, found :$getSupervisorLifecycleState"
    )
  }
}
