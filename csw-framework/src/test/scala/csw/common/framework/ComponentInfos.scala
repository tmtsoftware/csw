package csw.common.framework

import csw.common.framework.models.LocationServiceUsage.DoNotRegister
import csw.common.framework.models.{ComponentInfo, ContainerInfo}
import csw.services.location.models.ComponentType.{Assembly, HCD}

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
      DoNotRegister,
      Set.empty
    )

  val hcdInfoWithInitializeTimeout = ComponentInfo(
    "SampleHcd",
    HCD,
    "wfos",
    "csw.common.components.SampleComponentBehaviorFactory",
    DoNotRegister,
    Set.empty,
    0,
    5
  )

  val hcdInfoWithRunTimeout = ComponentInfo(
    "SampleHcd",
    HCD,
    "wfos",
    "csw.common.components.SampleComponentBehaviorFactory",
    DoNotRegister,
    Set.empty,
    5,
    0
  )

  val containerInfo: ContainerInfo = ContainerInfo("container", Set(hcdInfo, assemblyInfo))
}
