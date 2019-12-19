package csw.framework

import csw.command.client.models.framework.ComponentInfo
import csw.command.client.models.framework.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.framework.models.ContainerInfo
import csw.location.models.ComponentType.{Assembly, HCD}
import csw.prefix.models.Prefix

import scala.concurrent.duration.DurationDouble

object ComponentInfos {
  val assemblyInfo: ComponentInfo = ComponentInfo(
    Prefix("wfos.sampleassembly"),
    Assembly,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    DoNotRegister,
    Set.empty
  )

  val assemblyInfoToSimulateFailure: ComponentInfo = ComponentInfo(
    Prefix("wfos.trombone"),
    Assembly,
    "csw.common.components.framework.ComponentBehaviorFactoryToSimulateFailure",
    DoNotRegister,
    Set.empty
  )

  val hcdInfo: ComponentInfo = ComponentInfo(
    Prefix("wfos.samplehcd"),
    HCD,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty
  )

  val hcdInfoWithInitializeTimeout: ComponentInfo = ComponentInfo(
    Prefix("wfos.samplehcd"),
    HCD,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    50.millis
  )

  val hcdInfoWithRunTimeout: ComponentInfo = ComponentInfo(
    Prefix("wfos.samplehcd"),
    HCD,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    5.seconds
  )

  val dummyInfo: ComponentInfo = ComponentInfo(Prefix("wfos.dummyhcd"), HCD, "dummy", DoNotRegister)

  val containerInfo: ContainerInfo = ContainerInfo("container", Set(hcdInfo, assemblyInfo))
}
