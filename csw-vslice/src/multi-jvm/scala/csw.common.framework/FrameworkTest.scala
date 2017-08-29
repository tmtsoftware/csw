package csw.common.framework

import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.common.components.{ComponentStatistics, SampleComponentState}
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.common.framework.models.ContainerCommonMessage.GetContainerMode
import csw.common.framework.models.ContainerExternalMessage.GetComponents
import csw.common.framework.models.PubSub.Subscribe
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{ComponentStateSubscription, GetSupervisorMode}
import csw.common.framework.models.ToComponentLifecycleMessage.GoOffline
import csw.common.framework.models.{Components, ContainerMessage, SupervisorExternalMessage}
import csw.param.states.CurrentState
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType}
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class FrameworkTestMultiJvmNode1 extends FrameworkTest(0)
class FrameworkTestMultiJvmNode2 extends FrameworkTest(0)
class FrameworkTestMultiJvmNode3 extends FrameworkTest(0)

class FrameworkTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._

  implicit val actorSystem: ActorSystem[_] = system.toTyped
  implicit val testkit: TestKitSettings    = TestKitSettings(actorSystem)

//  LoggingSystemFactory.start("framework", "1.0", "localhost", system)

  test("should able to create multiple containers across jvm's and start component in standalone mode") {

    runOn(seed) {
      val wiring       = FrameworkWiring.make(system, locationService)
      val containerRef = Container.spawn(ConfigFactory.load("laser_container.conf"), wiring)

      val containerModeProbe = TestProbe[ContainerMode]
      val componentsProbe    = TestProbe[Components]

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Idle)

      Thread.sleep(2000)

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Running)
      enterBarrier("running")

      val wfsContainerLocationF =
        locationService.find(AkkaConnection(ComponentId("WFS_Container", ComponentType.Container)))
      val wfsContainerLocation = Await.result(wfsContainerLocationF, 5.seconds).get

      val efsContainerTypedRef = wfsContainerLocation.typedRef[ContainerMessage]

      efsContainerTypedRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Running)

      efsContainerTypedRef ! GetComponents(componentsProbe.ref)
      val components = componentsProbe.expectMsgType[Components].components
      components.size shouldBe 3
      enterBarrier("offline")
    }

    runOn(member1) {
      val wiring       = FrameworkWiring.make(system, locationService)
      val containerRef = Container.spawn(ConfigFactory.load("wfs_container.conf"), wiring)

      val containerModeProbe = TestProbe[ContainerMode]

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Idle)

      Thread.sleep(2000)

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Running)
      enterBarrier("running")

      val etonSupervisorF =
        locationService.find(AkkaConnection(ComponentId("Eton", ComponentType.HCD)))
      val etonSupervisorLocation = Await.result(etonSupervisorF, 5.seconds).get

      val etonSupervisorTypedRef = etonSupervisorLocation.typedRef[SupervisorExternalMessage]
      val compStateProbe         = TestProbe[CurrentState]

      etonSupervisorTypedRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
      etonSupervisorTypedRef ! ComponentStatistics(1)

      import SampleComponentState._
      compStateProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(domainChoice))))

      etonSupervisorTypedRef ! Lifecycle(GoOffline)
      enterBarrier("offline")
    }

    runOn(member2) {
      val wiring        = FrameworkWiring.make(system, locationService)
      val supervisorRef = Standalone.spawn(ConfigFactory.load("eaton_hcd_standalone.conf"), wiring)

      Thread.sleep(2000)
      val supervisorStateProbe = TestProbe[SupervisorMode]

      supervisorRef ! GetSupervisorMode(supervisorStateProbe.ref)
      supervisorStateProbe.expectMsg(SupervisorMode.Running)
      enterBarrier("running")

      enterBarrier("offline")
      Thread.sleep(50)
      supervisorRef ! GetSupervisorMode(supervisorStateProbe.ref)
      supervisorStateProbe.expectMsg(SupervisorMode.RunningOffline)
    }

    enterBarrier("end")
  }
}
