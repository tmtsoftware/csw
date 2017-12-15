package csw.services.ccs.jmh

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
import csw.messages.ccs.commands.ComponentRef
import csw.messages.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.messages.{ComponentMessage, ContainerExternalMessage}
import csw.services.location.commons.BlockingUtils

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationDouble}

object BenchmarkHelpers {

  def spawnStandaloneComponent(actorSystem: ActorSystem, config: Config): ComponentRef = {
    val wiring: FrameworkWiring                          = FrameworkWiring.make(actorSystem)
    implicit val typedSystem: typed.ActorSystem[Nothing] = actorSystem.toTyped
    implicit val settings: TestKitSettings               = TestKitSettings(typedSystem)

    val probe = TestProbe[SupervisorLifecycleState]

    val actorRef = Await.result(Standalone.spawn(config, wiring), 5.seconds)
    assertThatSupervisorIsRunning(actorRef, probe, 5.seconds)
    ComponentRef(actorRef)
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
