package csw.apps.containercmd

import java.io.FileWriter
import java.nio.file.{Files, Path, Paths}

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
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

// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-169: Creation of Multiple Components
// DEOPSCSW-182: Control Life Cycle of Components
// DEOPSCSW-216: Locate and connect components to send AKKA commands
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

  def createStandaloneTmpFile(): Path = {
    val hcdConfiguration       = scala.io.Source.fromResource("eaton_hcd_standalone.conf").mkString
    val standaloneConfFilePath = Files.createTempFile("eaton_hcd_standalone", ".conf")
    val fileWriter             = new FileWriter(standaloneConfFilePath.toFile, true)
    fileWriter.write(hcdConfiguration)
    fileWriter.close()
    standaloneConfFilePath
  }

  test("should able to start components in container mode and in standalone mode through configuration service") {

    // start config server and upload laser_container.conf file
    runOn(seed) {
      val serverWiring = ServerWiring.make(locationService)
      serverWiring.svnRepo.initSvnRepo()
      serverWiring.httpService.registeredLazyBinding.await

      val configService       = ConfigClientFactory.adminApi(system, locationService)
      val containerConfigData = ConfigData.fromString(Source.fromResource("laser_container.conf").mkString)

      Await.result(
        configService.create(Paths.get("/laser_container.conf"), containerConfigData, comment = "container"),
        5.seconds
      )

      enterBarrier("config-file-uploaded")
      enterBarrier("running")
      enterBarrier("offline")
    }

    runOn(member1) {
      enterBarrier("config-file-uploaded")

      val testProbe = TestProbe[ContainerMode]

      // withEntries required for multi-node test where seed node is picked up from environment variable
      val clusterSettings = ClusterAwareSettings.joinLocal(3552).withEntries(sys.env)
      val containerCmd    = new ContainerCmd(clusterSettings)

      // only file path is provided, by default - file will be fetched from configuration service
      // and will be considered as container configuration.
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
      Await.result(containerCmd.shutdown, 5.seconds)
    }

    runOn(member2) {
      enterBarrier("config-file-uploaded")

      val testProbe = TestProbe[SupervisorMode]

      val containerCmd = new ContainerCmd(ClusterAwareSettings.joinLocal(3552))

      // this step is required for multi-node, as eaton_hcd_standalone.conf file is not directly available
      // when sbt-assembly creates fat jar
      val standaloneConfFilePath = createStandaloneTmpFile()

      val args          = Array("--standalone", "--local", standaloneConfFilePath.toString)
      val supervisorRef = containerCmd.start(args).get.asInstanceOf[ActorRef[SupervisorExternalMessage]]

      waitForSupervisorToMoveIntoRunningMode(supervisorRef, testProbe, 5.seconds) shouldBe true
      enterBarrier("running")

      enterBarrier("offline")
      Thread.sleep(50)
      supervisorRef ! GetSupervisorMode(testProbe.ref)
      testProbe.expectMsg(SupervisorMode.RunningOffline)

      Files.delete(standaloneConfFilePath)
      Await.result(containerCmd.shutdown, 5.seconds)
    }
    enterBarrier("end")
  }

}
