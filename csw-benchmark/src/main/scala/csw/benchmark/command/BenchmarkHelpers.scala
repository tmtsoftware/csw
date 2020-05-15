package csw.benchmark.command

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed
import akka.actor.typed.{ActorRef, SpawnProtocol}
import com.typesafe.config.Config
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentCommonMessage.GetSupervisorLifecycleState
import csw.command.client.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.command.client.models.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.internal.BlockingUtils
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationDouble}

object BenchmarkHelpers {

  def spawnStandaloneComponent(actorSystem: typed.ActorSystem[SpawnProtocol.Command], config: Config): CommandService = {
    val locationService                                                = HttpLocationServiceFactory.makeLocalClient(actorSystem)
    val wiring: FrameworkWiring                                        = FrameworkWiring.make(actorSystem, locationService)
    implicit val typedSystem: typed.ActorSystem[SpawnProtocol.Command] = actorSystem

    val probe = TestProbe[SupervisorLifecycleState]()

    Standalone.spawn(config, wiring)
    val akkaLocation: AkkaLocation =
      Await
        .result(
          locationService.resolve(AkkaConnection(ComponentId(Prefix(Subsystem.CSW, "Perf"), ComponentType.HCD)), 5.seconds),
          5.seconds
        )
        .get

    assertThatSupervisorIsRunning(akkaLocation.componentRef, probe, 5.seconds)

    CommandServiceFactory.make(akkaLocation)
  }

  def assertThatContainerIsRunning(
      containerRef: ActorRef[ContainerMessage],
      probe: TestProbe[ContainerLifecycleState],
      duration: Duration
  ): Unit = {
    def getContainerLifecycleState: ContainerLifecycleState = {
      containerRef ! GetContainerLifecycleState(probe.ref)
      probe.expectMessageType[ContainerLifecycleState]
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
      probe.expectMessageType[SupervisorLifecycleState]
    }

    assert(
      BlockingUtils.poll(getSupervisorLifecycleState == SupervisorLifecycleState.Running, duration),
      s"expected :${SupervisorLifecycleState.Running}, found :$getSupervisorLifecycleState"
    )
  }
}
