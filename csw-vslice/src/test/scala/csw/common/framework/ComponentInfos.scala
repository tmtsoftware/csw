package csw.common.framework

import csw.common.framework.models.LocationServiceUsage.{DoNotRegister, RegisterOnly}
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

  val containerInfo: ContainerInfo = ContainerInfo("container", RegisterOnly, Set(hcdInfo, assemblyInfo))
}
