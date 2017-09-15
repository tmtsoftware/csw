package csw.common

import akka.typed.ActorRef
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.models.ContainerCommonMessage.GetContainerMode
import csw.common.framework.models.SupervisorCommonMessage.GetSupervisorMode
import csw.common.framework.models.{ContainerExternalMessage, SupervisorExternalMessage}
import csw.services.location.commons.BlockingUtils

import scala.concurrent.duration.Duration

object FrameworkAssertions {

  def assertThatContainerIsInRunningMode(
      containerRef: ActorRef[ContainerExternalMessage],
      probe: TestProbe[ContainerMode],
      duration: Duration
  ): Unit = {
    def getContainerMode: ContainerMode = {
      containerRef ! GetContainerMode(probe.ref)
      probe.expectMsgType[ContainerMode]
    }

    assert(BlockingUtils.poll(getContainerMode == ContainerMode.Running, duration),
           s"expected :${ContainerMode.Running}, found :$getContainerMode")
  }

  def assertThatSupervisorIsInRunningMode(
      actorRef: ActorRef[SupervisorExternalMessage],
      probe: TestProbe[SupervisorMode],
      duration: Duration
  ): Unit = {
    def getSupervisorMode: SupervisorMode = {
      actorRef ! GetSupervisorMode(probe.ref)
      probe.expectMsgType[SupervisorMode]
    }

    assert(BlockingUtils.poll(getSupervisorMode == SupervisorMode.Running, duration),
           s"expected :${SupervisorMode.Running}, found :$getSupervisorMode")
  }
}
