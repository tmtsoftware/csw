package csw.framework

import csw.command.client.models.framework.ComponentInfo
import csw.command.client.models.framework.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.framework.models.ContainerInfo
import csw.location.models.ComponentType.{Assembly, HCD}
import csw.prefix.Subsystem

import scala.concurrent.duration.DurationDouble

object ComponentInfos {
  val assemblyInfo: ComponentInfo = ComponentInfo(
    "SampleAssembly",
    Subsystem.WFOS,
    Assembly,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    DoNotRegister,
    Set.empty
  )

  val assemblyInfoToSimulateFailure: ComponentInfo = ComponentInfo(
    "trombone",
    Subsystem.WFOS,
    Assembly,
    "csw.common.components.framework.ComponentBehaviorFactoryToSimulateFailure",
    DoNotRegister,
    Set.empty
  )

  val hcdInfo: ComponentInfo = ComponentInfo(
    "SampleHcd",
    Subsystem.WFOS,
    HCD,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty
  )

  val hcdInfoWithInitializeTimeout: ComponentInfo = ComponentInfo(
    "SampleHcd",
    Subsystem.WFOS,
    HCD,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    50.millis
  )

  val hcdInfoWithRunTimeout: ComponentInfo = ComponentInfo(
    "SampleHcd",
    Subsystem.WFOS,
    HCD,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty,
    5.seconds
  )

  val dummyInfo: ComponentInfo = ComponentInfo(
    "DummyHcd",
    Subsystem.WFOS,
    HCD,
    "dummy",
    DoNotRegister
  )

  val containerInfo: ContainerInfo = ContainerInfo("container", Set(hcdInfo, assemblyInfo))
}
