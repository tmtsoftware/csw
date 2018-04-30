package csw.framework

import csw.messages.framework.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.framework.models.ContainerInfo
import csw.messages.framework.ComponentInfo
import csw.messages.location.ComponentType.{Assembly, HCD}

import scala.concurrent.duration.DurationDouble

object ComponentInfos {
  val assemblyInfo =
    ComponentInfo(
      "SampleAssembly",
      Assembly,
      "wfos",
      "csw.common.components.framework.SampleComponentHandlers",
      DoNotRegister,
      Set.empty
    )

  val assemblyInfoToSimulateFailure =
    ComponentInfo(
      "trombone",
      Assembly,
      "wfos",
      "csw.common.components.framework.ComponentHandlerToSimulateFailure",
      DoNotRegister,
      Set.empty
    )

  val hcdInfo =
    ComponentInfo(
      "SampleHcd",
      HCD,
      "wfos",
      "csw.common.components.framework.SampleComponentHandlers",
      RegisterOnly,
      Set.empty
    )

  val hcdInfoWithInitializeTimeout = ComponentInfo(
    "SampleHcd",
    HCD,
    "wfos",
    "csw.common.components.framework.SampleComponentHandlers",
    RegisterOnly,
    Set.empty,
    50.millis
  )

  val hcdInfoWithRunTimeout = ComponentInfo(
    "SampleHcd",
    HCD,
    "wfos",
    "csw.common.components.framework.SampleComponentHandlers",
    RegisterOnly,
    Set.empty,
    5.seconds
  )

  val initFailureHcdInfo =
    ComponentInfo(
      "SampleHcd",
      HCD,
      "wfos",
      "csw.common.components.framework.InitFailureComponentHandlers",
      RegisterOnly,
      Set.empty
    )

  val initFailureRestartHcdInfo =
    ComponentInfo(
      "SampleHcd",
      HCD,
      "wfos",
      "csw.common.components.framework.InitFailureRestartComponentHandlers",
      RegisterOnly,
      Set.empty
    )

  val validateFailureRestartHcdInfo =
    ComponentInfo(
      "SampleHcd",
      HCD,
      "wfos",
      "csw.common.components.framework.ValidateFailureRestartComponentHandlers",
      RegisterOnly,
      Set.empty
    )
  val containerInfo: ContainerInfo = ContainerInfo("container", Set(hcdInfo, assemblyInfo))
}
