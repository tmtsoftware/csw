package csw.common.framework

import csw.common.framework.models.{ComponentInfo, ContainerInfo}
import csw.services.location.models.ComponentType.{Assembly, HCD}

object ComponentInfos {
  val assemblyInfo =
    ComponentInfo("trombone", Assembly, "wfos", "csw.common.components.SampleComponentWiring", Set.empty)

  val assemblyInfoToSimulateFailure =
    ComponentInfo("trombone", Assembly, "wfos", "csw.common.components.ComponentWiringToSimulateFailure", Set.empty)

  val hcdInfo =
    ComponentInfo("SampleHcd", HCD, "wfos", "csw.common.components.SampleComponentWiring", Set.empty)

  val containerInfo: ContainerInfo = ContainerInfo("container", Set(hcdInfo, assemblyInfo))
}
