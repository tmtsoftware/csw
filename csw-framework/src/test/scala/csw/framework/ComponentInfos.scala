package csw.framework

import csw.command.client.models.framework.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.framework.models.ContainerInfo
import csw.command.client.models.framework.ComponentInfo
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.params.core.models.Prefix

import scala.concurrent.duration.DurationDouble

object ComponentInfos {
  val assemblyInfo: ComponentInfo = ComponentInfo(
    "SampleAssembly",
    Assembly,
    Prefix("wfos"),
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    DoNotRegister,
    Set.empty
  )

  val assemblyInfoToSimulateFailure: ComponentInfo = ComponentInfo(
    "trombone",
    Assembly,
    Prefix("wfos"),
    "csw.common.components.framework.ComponentBehaviorFactoryToSimulateFailure",
    DoNotRegister,
    Set.empty
  )

  val hcdInfo: ComponentInfo = ComponentInfo(
    "SampleHcd",
    HCD,
    Prefix("wfos"),
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty
  )

  val hcdInfoWithInitializeTimeout: ComponentInfo = ComponentInfo(
    "SampleHcd",
    HCD,
    Prefix("wfos"),
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    50.millis
  )

  val hcdInfoWithRunTimeout: ComponentInfo = ComponentInfo(
    "SampleHcd",
    HCD,
    Prefix("wfos"),
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    5.seconds
  )

  val dummyInfo: ComponentInfo = ComponentInfo(
    "DummyHcd",
    HCD,
    Prefix("wfos"),
    "dummy",
    DoNotRegister
  )

  val containerInfo: ContainerInfo = ContainerInfo("container", Set(hcdInfo, assemblyInfo))
}
