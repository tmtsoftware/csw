package csw.common.framework

import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.wiring.{Container, FrameworkWiring}
import csw.common.framework.models.ContainerCommonMessage.GetContainerMode
import csw.common.framework.models.ContainerExternalMessage.GetComponents
import csw.common.framework.models.{Components, ContainerMessage}
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class FrameworkTestMultiJvmNode1 extends FrameworkTest(0)
class FrameworkTestMultiJvmNode2 extends FrameworkTest(0)

class FrameworkTest(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) {

  import config._

  implicit val actorSystem: ActorSystem[_] = system.toTyped
  implicit val testkit: TestKitSettings    = TestKitSettings(actorSystem)

//  LoggingSystemFactory.start("framework", "1.0", "localhost", system)

  test("should able to create multiple containers across jvm's") {

    runOn(seed) {
      val wiring       = FrameworkWiring.make(system, locationService)
      val containerRef = Container.spawn(ConfigFactory.load("laser_container.conf"), wiring)

      val containerModeProbe = TestProbe[ContainerMode]
      val componentsProbe    = TestProbe[Components]

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Idle)

      Thread.sleep(500)

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Running)
      enterBarrier("container-running")

      val wfsContainerLocationF =
        locationService.find(AkkaConnection(ComponentId("WFS_Container", ComponentType.Container)))
      val wfsContainerLocation = Await.result(wfsContainerLocationF, 5.seconds).get

      val efsContainerTypedRef = wfsContainerLocation.typedRef[ContainerMessage]

      efsContainerTypedRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Running)

      efsContainerTypedRef ! GetComponents(componentsProbe.ref)
      val components = componentsProbe.expectMsgType[Components].components
      components.size shouldBe 3
    }

    runOn(member) {
      val wiring       = FrameworkWiring.make(system, locationService)
      val containerRef = Container.spawn(ConfigFactory.load("wfs_container.conf"), wiring)

      val containerModeProbe = TestProbe[ContainerMode]
      val componentsProbe    = TestProbe[Components]

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Idle)

      Thread.sleep(500)

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Running)
      enterBarrier("container-running")

      val laserContainerLocationF =
        locationService.find(AkkaConnection(ComponentId("LGSF_Container", ComponentType.Container)))
      val laserContainerLocation = Await.result(laserContainerLocationF, 5.seconds).get

      val laserContainerTypedRef = laserContainerLocation.typedRef[ContainerMessage]

      laserContainerTypedRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Running)

      laserContainerTypedRef ! GetComponents(componentsProbe.ref)
      val components = componentsProbe.expectMsgType[Components].components
      components.size shouldBe 3
    }
    enterBarrier("end")
  }
}
