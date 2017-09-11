package csw.common.framework

import java.nio.file.Paths

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
import csw.apps.containercmd.ContainerCmd
import csw.common.components.{ComponentStatistics, SampleComponentState}
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.models.ContainerCommonMessage.{GetComponents, GetContainerMode}
import csw.common.framework.models.PubSub.Subscribe
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{ComponentStateSubscription, GetSupervisorMode}
import csw.common.framework.models.ToComponentLifecycleMessage.GoOffline
import csw.common.framework.models.{Components, ContainerExternalMessage, SupervisorExternalMessage}
import csw.param.states.CurrentState
import csw.services.config.api.models.ConfigData
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.{ServerWiring, Settings}
import csw.services.location.commons.{BlockingUtils, ClusterAwareSettings}
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationLong}
import scala.io.Source

class ContainerCmdTestMultiJvm1 extends ContainerCmdTest(0)
class ContainerCmdTestMultiJvm2 extends ContainerCmdTest(0)
class ContainerCmdTestMultiJvm3 extends ContainerCmdTest(0)

class ContainerCmdTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._

  implicit val actorSystem: ActorSystem[_] = system.toTyped
  implicit val testkit: TestKitSettings    = TestKitSettings(actorSystem)

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  override def beforeAll(): Unit = {
    super.beforeAll()
    testFileUtils.deleteServerFiles()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testFileUtils.deleteServerFiles()
  }

  test("should able to start container and component in standalone mode through configuration service") {

    runOn(seed) {
      val serverWiring = ServerWiring.make(locationService)
      serverWiring.svnRepo.initSvnRepo()
      serverWiring.httpService.registeredLazyBinding.await

      val configService = ConfigClientFactory.adminApi(system, locationService)

      val containerConfigData  = ConfigData.fromString(Source.fromResource("laser_container.conf").mkString)
      val standaloneConfigData = ConfigData.fromString(Source.fromResource("eaton_hcd_standalone.conf").mkString)

      Await.result(configService.create(Paths.get("/laser_container.conf"), containerConfigData, comment = "container"),
                   5.seconds)

      Await.result(
        configService.create(Paths.get("/eaton_hcd_standalone.conf"), standaloneConfigData, comment = "standalone"),
        5.seconds
      )

      enterBarrier("config-files-created")
      enterBarrier("running")
      enterBarrier("offline")
    }

    runOn(member1) {
      enterBarrier("config-files-created")

      val testProbe = TestProbe[ContainerMode]

      val containerCmd = new ContainerCmd(ClusterAwareSettings.joinLocal(3552))
      val args         = Array("/laser_container.conf")
      val containerRef = containerCmd.start(args).get.asInstanceOf[ActorRef[ContainerExternalMessage]]

      waitForContainerToMoveIntoRunningMode(containerRef, testProbe, 5.seconds) shouldBe true

      val componentsProbe     = TestProbe[Components]
      val supervisorModeProbe = TestProbe[SupervisorMode]

      containerRef ! GetComponents(componentsProbe.ref)
      val laserContainerComponents = componentsProbe.expectMsgType[Components].components
      laserContainerComponents.size shouldBe 3

      // check that all the components within supervisor moves to Running mode
      laserContainerComponents
        .foreach { component ⇒
          component.supervisor ! GetSupervisorMode(supervisorModeProbe.ref)
          supervisorModeProbe.expectMsg(SupervisorMode.Running)
        }
      enterBarrier("running")

      // resolve and send message to component running in different jvm or on different physical machine
      val etonSupervisorF =
        locationService.resolve(AkkaConnection(ComponentId("Eton", ComponentType.HCD)), 2.seconds)
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
      enterBarrier("config-files-created")

      val testProbe = TestProbe[SupervisorMode]

      val containerCmd  = new ContainerCmd(ClusterAwareSettings.joinLocal(3552))
      val args          = Array("--standalone", "/eaton_hcd_standalone.conf")
      val supervisorRef = containerCmd.start(args).get.asInstanceOf[ActorRef[SupervisorExternalMessage]]

      waitForSupervisorToMoveIntoRunningMode(supervisorRef, testProbe, 5.seconds) shouldBe true
      enterBarrier("running")

      enterBarrier("offline")
      Thread.sleep(50)
      supervisorRef ! GetSupervisorMode(testProbe.ref)
      testProbe.expectMsg(SupervisorMode.RunningOffline)
    }
    enterBarrier("end")
  }

  def waitForContainerToMoveIntoRunningMode(
      containerRef: ActorRef[ContainerExternalMessage],
      probe: TestProbe[ContainerMode],
      duration: Duration
  ): Boolean = {

    def getContainerMode: ContainerMode = {
      containerRef ! GetContainerMode(probe.ref)
      probe.expectMsgType[ContainerMode]
    }

    BlockingUtils.poll(getContainerMode == ContainerMode.Running, duration)
  }

  def waitForSupervisorToMoveIntoRunningMode(
      actorRef: ActorRef[SupervisorExternalMessage],
      probe: TestProbe[SupervisorMode],
      duration: Duration
  ): Boolean = {

    def getSupervisorMode: SupervisorMode = {
      actorRef ! GetSupervisorMode(probe.ref)
      probe.expectMsgType[SupervisorMode]
    }

    BlockingUtils.poll(getSupervisorMode == SupervisorMode.Running, duration)
  }
}
