package csw.common.framework

import java.util.Collections

import csw.common.framework.javadsl.JComponentInfo
import csw.common.framework.javadsl.commons.JComponentInfos
import csw.common.framework.models.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.common.framework.models.{ComponentInfo, ContainerInfo, LocationServiceUsage}
import csw.services.location.javadsl.JComponentType
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

  val jHcdInfo: ComponentInfo = JComponentInfos.jHcdInfo

  val containerInfo: ContainerInfo = ContainerInfo("container", RegisterOnly, Set(hcdInfo, assemblyInfo))
}
