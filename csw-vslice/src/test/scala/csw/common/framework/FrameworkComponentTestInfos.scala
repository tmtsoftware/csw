package csw.common.framework

import csw.common.framework.models.{ComponentInfo, LocationServiceUsages}
import csw.common.framework.models.LocationServiceUsages.DoNotRegister
import csw.services.location.models.ComponentType
import csw.services.location.models.ComponentType.{Assembly, HCD}

object FrameworkComponentTestInfos {
  val assemblyInfo =
    ComponentInfo("trombone",
                  Assembly,
                  "wfos",
                  "csw.common.components.SampleComponentWiring",
                  DoNotRegister,
                  Some(Set.empty))

  val assemblyInfoToSimulateFailure =
    ComponentInfo("trombone",
                  Assembly,
                  "wfos",
                  "csw.common.components.ComponentWiringToSimulateFailure",
                  DoNotRegister,
                  Some(Set.empty))

  val hcdInfo =
    ComponentInfo("SampleHcd", HCD, "wfos", "csw.common.components.SampleComponentWiring", DoNotRegister)

  val containerInfo: ComponentInfo =
    ComponentInfo("container",
                  ComponentType.Container,
                  "tcs.mobie.blue.filter",
                  "",
                  LocationServiceUsages.RegisterOnly,
                  None,
                  Some(Set(hcdInfo, assemblyInfo)))
}
