package csw.framework

import csw.command.client.models.framework.ComponentInfo
import csw.command.client.models.framework.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.framework.models.ContainerInfo
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.ConnectionType.{AkkaType, HttpType}
import csw.prefix.models.Prefix

import scala.concurrent.duration.DurationDouble

object ComponentInfos {
  val assemblyInfo: ComponentInfo = ComponentInfo(
    Prefix("WFOS.SampleAssembly"),
    Assembly,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    DoNotRegister,
    Set.empty,
    Set.empty
  )

  val assemblyInfoToSimulateFailure: ComponentInfo = ComponentInfo(
    Prefix("WFOS.Trombone"),
    Assembly,
    "csw.common.components.framework.ComponentBehaviorFactoryToSimulateFailure",
    DoNotRegister,
    Set.empty,
    Set.empty
  )

  val hcdInfo: ComponentInfo = ComponentInfo(
    Prefix("WFOS.SampleHcd"),
    HCD,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set(AkkaType, HttpType),
    Set.empty
  )

  val hcdInfoWithInitializeTimeout: ComponentInfo = ComponentInfo(
    Prefix("WFOS.SampleHcd"),
    HCD,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set(AkkaType, HttpType),
    Set.empty,
    50.millis
  )

  val hcdInfoWithRunTimeout: ComponentInfo = ComponentInfo(
    Prefix("WFOS.SampleHcd"),
    HCD,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set(AkkaType, HttpType),
    Set.empty,
    5.seconds
  )

  val dummyInfo: ComponentInfo = ComponentInfo(Prefix("WFOS.dummyhcd"), HCD, "dummy", DoNotRegister)

  val containerInfo: ContainerInfo = ContainerInfo("container", Set(hcdInfo, assemblyInfo))
}
