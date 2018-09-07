package csw.framework

import csw.messages.framework.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.framework.models.ContainerInfo
import csw.messages.framework.ComponentInfo
import csw.messages.location.ComponentType.{Assembly, HCD}
import csw.messages.params.models.Prefix

import scala.concurrent.duration.DurationDouble

object ComponentInfos {
  val assemblyInfo =
    ComponentInfo(
      "SampleAssembly",
      Assembly,
      Prefix("wfos"),
      "csw.common.components.framework.SampleComponentBehaviorFactory",
      DoNotRegister,
      Set.empty
    )

  val assemblyInfoToSimulateFailure =
    ComponentInfo(
      "trombone",
      Assembly,
      Prefix("wfos"),
      "csw.common.components.framework.ComponentBehaviorFactoryToSimulateFailure",
      DoNotRegister,
      Set.empty
    )

  val hcdInfo =
    ComponentInfo(
      "SampleHcd",
      HCD,
      Prefix("wfos"),
      "csw.common.components.framework.SampleComponentBehaviorFactory",
      RegisterOnly,
      Set.empty
    )

  val hcdInfoWithInitializeTimeout = ComponentInfo(
    "SampleHcd",
    HCD,
    Prefix("wfos"),
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    50.millis
  )

  val hcdInfoWithRunTimeout = ComponentInfo(
    "SampleHcd",
    HCD,
    Prefix("wfos"),
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    5.seconds
  )

  val dummyInfo =
    ComponentInfo(
      "DummyHcd",
      HCD,
      Prefix("wfos"),
      "dummy",
      DoNotRegister,
      Set.empty
    )

  val containerInfo: ContainerInfo = ContainerInfo("container", Set(hcdInfo, assemblyInfo))
}
