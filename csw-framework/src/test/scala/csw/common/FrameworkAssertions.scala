package csw.common

import akka.typed.ActorRef
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.internal.container.ContainerLifecycleState
import csw.common.framework.internal.supervisor.SupervisorLifecycleState
import csw.common.framework.models.ContainerCommonMessage.GetContainerLifecycleState
import csw.common.framework.models.SupervisorCommonMessage.GetSupervisorLifecycleState
import csw.common.framework.models.{ContainerExternalMessage, SupervisorExternalMessage}
import csw.services.location.commons.BlockingUtils

import scala.concurrent.duration.Duration

object FrameworkAssertions {

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
      actorRef: ActorRef[SupervisorExternalMessage],
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
