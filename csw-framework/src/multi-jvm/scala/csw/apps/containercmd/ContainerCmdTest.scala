package csw.apps.containercmd

import java.io.FileWriter
import java.nio.file.{Files, Path, Paths}

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
import csw.common.FrameworkAssertions._
import csw.common.components.{ComponentStatistics, SampleComponentState}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.ContainerCommonMessage.GetComponents
import csw.messages.PubSub.Subscribe
import csw.messages.RunningMessage.Lifecycle
import csw.messages.SupervisorCommonMessage.{ComponentStateSubscription, GetSupervisorLifecycleState}
import csw.messages.ToComponentLifecycleMessage.GoOffline
import csw.messages.ccs.commands.{CommandResponse, Setup}
import csw.messages.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.ObsId
import csw.messages.params.states.CurrentState
import csw.messages.{Components, ContainerExternalMessage, Shutdown, SupervisorExternalMessage}
import csw.services.config.api.models.ConfigData
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.{ServerWiring, Settings}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.Source

class ContainerCmdTestMultiJvm1 extends ContainerCmdTest(0)
class ContainerCmdTestMultiJvm2 extends ContainerCmdTest(0)
class ContainerCmdTestMultiJvm3 extends ContainerCmdTest(0)

// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-168: Deployment of multiple Assemblies and HCDs
// DEOPSCSW-169: Creation of Multiple Components
// DEOPSCSW-171: Starting component from command line
// DEOPSCSW-182: Control Life Cycle of Components
// DEOPSCSW-216: Locate and connect components to send AKKA commands
class ContainerCmdTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._

  implicit val actorSystem: ActorSystem[_]  = system.toTyped
  implicit val ec: ExecutionContextExecutor = actorSystem.executionContext
  implicit val testkit: TestKitSettings     = TestKitSettings(actorSystem)

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  override def beforeAll(): Unit = {
    super.beforeAll()
    testFileUtils.deleteServerFiles()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testFileUtils.deleteServerFiles()
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
      enterBarrier("before-shutdown")
      enterBarrier("eton-shutdown")
    }

    runOn(member1) {
      enterBarrier("config-file-uploaded")

      val testProbe = TestProbe[ContainerLifecycleState]

      // withEntries required for multi-node test where seed node is picked up from environment variable
      val clusterSettings = ClusterAwareSettings.joinLocal(3552).withEntries(sys.env)
      val containerCmd    = new ContainerCmd("laser_container_app", clusterSettings, false)

      // only file path is provided, by default - file will be fetched from configuration service
      // and will be considered as container configuration.
      val args = Array("/laser_container.conf")
      val containerRef =
        containerCmd.start(args).asInstanceOf[ActorRef[ContainerExternalMessage]]

      assertThatContainerIsRunning(containerRef, testProbe, 5.seconds)

      val componentsProbe               = TestProbe[Components]
      val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]

      containerRef ! GetComponents(componentsProbe.ref)
      val laserContainerComponents = componentsProbe.expectMsgType[Components].components
      laserContainerComponents.size shouldBe 3

      // check that all the components within supervisor moves to Running lifecycle state
      laserContainerComponents
        .foreach { component ⇒
          component.supervisor ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)
          supervisorLifecycleStateProbe.expectMsg(SupervisorLifecycleState.Running)
        }
      enterBarrier("running")

      // resolve and send message to component running in different jvm or on different physical machine
      val etonSupervisorF =
        locationService.resolve(AkkaConnection(ComponentId("Eton", ComponentType.HCD)), 2.seconds)
      val etonSupervisorLocation = Await.result(etonSupervisorF, 15.seconds).get

      val etonSupervisorTypedRef = etonSupervisorLocation.componentRef()
      val eatonCompStateProbe    = TestProbe[CurrentState]

      etonSupervisorTypedRef ! ComponentStateSubscription(Subscribe(eatonCompStateProbe.ref))
      etonSupervisorTypedRef ! ComponentStatistics(1)

      import SampleComponentState._
      eatonCompStateProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(domainChoice))))

      val obsId: ObsId          = ObsId("Obs001")
      val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
      // setup to receive Success in validation result
      val setupSuccess: Setup = Setup(obsId, successPrefix, Set(param))
      val setupFailure: Setup = Setup(obsId, failedPrefix, Set(param))

      val laserAssemblySupervisor = laserContainerComponents.head.supervisor
      val laserCompStateProbe     = TestProbe[CurrentState]
      val commandResponseProbe    = TestProbe[CommandResponse]
      etonSupervisorTypedRef ! Submit(setupFailure, commandResponseProbe.ref)
      eatonCompStateProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(submitCommandChoice))))
      eatonCompStateProbe.expectMsg(CurrentState(failedPrefix, Set(choiceKey.set(setupConfigChoice), param)))

      etonSupervisorTypedRef ! Oneway(setupSuccess, commandResponseProbe.ref)
      eatonCompStateProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(oneWayCommandChoice))))
      eatonCompStateProbe.expectMsg(CurrentState(successPrefix, Set(choiceKey.set(setupConfigChoice), param)))

      etonSupervisorTypedRef ! Lifecycle(GoOffline)
      enterBarrier("offline")

      enterBarrier("before-shutdown")
      laserAssemblySupervisor ! ComponentStateSubscription(Subscribe(laserCompStateProbe.ref))
      enterBarrier("eton-shutdown")

      // DEOPSCSW-218: Discover component connection information using Akka protocol
      // Laser assembly is tracking Eton Hcd which is running on member2 (different jvm than this)
      // When Eton Hcd shutdowns, laser assembly receives LocationRemoved event
      laserCompStateProbe.expectMsg(10.seconds, CurrentState(prefix, Set(choiceKey.set(akkaLocationRemovedChoice))))
    }

    runOn(member2) {
      enterBarrier("config-file-uploaded")

      val testProbe = TestProbe[SupervisorLifecycleState]

      val containerCmd = new ContainerCmd("eaton_hcd_standalone_app", ClusterAwareSettings.joinLocal(3552), false)

      // this step is required for multi-node, as eaton_hcd_standalone.conf file is not directly available
      // when sbt-assembly creates fat jar
      val standaloneConfFilePath = createStandaloneTmpFile()

      val args = Array("--standalone", "--local", standaloneConfFilePath.toString)
      val supervisorRef =
        containerCmd.start(args).asInstanceOf[ActorRef[SupervisorExternalMessage]]

      assertThatSupervisorIsRunning(supervisorRef, testProbe, 5.seconds)
      enterBarrier("running")

      enterBarrier("offline")
      Thread.sleep(50)
      supervisorRef ! GetSupervisorLifecycleState(testProbe.ref)
      testProbe.expectMsg(SupervisorLifecycleState.RunningOffline)

      enterBarrier("before-shutdown")
      supervisorRef ! Shutdown
      enterBarrier("eton-shutdown")

      Files.delete(standaloneConfFilePath)
    }
    enterBarrier("end")
  }

}
