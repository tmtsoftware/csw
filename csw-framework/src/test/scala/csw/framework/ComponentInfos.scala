package csw.framework

import csw.messages.framework.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.framework.models.ContainerInfo
import csw.messages.framework.ComponentInfo
import csw.messages.location.ComponentType.{Assembly, HCD}

import scala.concurrent.duration.DurationDouble

object ComponentInfos {
  val assemblyInfo =
    ComponentInfo(
      "trombone",
      Assembly,
      "wfos",
      "csw.common.components.SampleComponentBehaviorFactory",
      DoNotRegister,
      Set.empty
    )

  val assemblyInfoToSimulateFailure =
    ComponentInfo(
      "trombone",
      Assembly,
      "wfos",
      "csw.common.components.ComponentBehaviorFactoryToSimulateFailure",
      DoNotRegister,
      Set.empty
    )

  val hcdInfo =
    ComponentInfo(
      "SampleHcd",
      HCD,
      "wfos",
      "csw.common.components.SampleComponentBehaviorFactory",
      RegisterOnly,
      Set.empty
    )

  val hcdInfoWithInitializeTimeout = ComponentInfo(
    "SampleHcd",
    HCD,
    "wfos",
    "csw.common.components.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    0.seconds
  )

  val hcdInfoWithRunTimeout = ComponentInfo(
    "SampleHcd",
    HCD,
    "wfos",
    "csw.common.components.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    5.seconds
  )

  val containerInfo: ContainerInfo = ContainerInfo("container", Set(hcdInfo, assemblyInfo))
}
