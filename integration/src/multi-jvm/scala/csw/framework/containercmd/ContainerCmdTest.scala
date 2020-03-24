package csw.framework.containercmd

import java.io.FileWriter
import java.nio.file.{Files, Path, Paths}

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentCommonMessage.{ComponentStateSubscription, GetSupervisorLifecycleState}
import csw.command.client.messages.ContainerCommonMessage.GetComponents
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.Shutdown
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.command.client.models.framework.PubSub.Subscribe
import csw.command.client.models.framework.ToComponentLifecycleMessage.GoOffline
import csw.command.client.models.framework.{Components, ContainerLifecycleState, SupervisorLifecycleState}
import csw.common.FrameworkAssertions._
import csw.config.api.ConfigData
import csw.config.client.scaladsl.ConfigClientFactory
import csw.config.server.commons.TestFileUtils
import csw.config.server.mocks.MockedAuthentication
import csw.config.server.{ServerWiring, Settings}
import csw.framework.deploy.containercmd.ContainerCmd
import csw.location.api.models
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.location.server.http.MultiNodeHTTPLocationService
import csw.params.commands.CommandResponse.Invalid
import csw.params.commands.{CommandName, Setup}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.ObsId
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.Subsystem.{CSW, Container}
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.Source
import scala.util.control.NonFatal

class ContainerCmdTestMultiJvm1 extends ContainerCmdTest(0)
class ContainerCmdTestMultiJvm2 extends ContainerCmdTest(0)
class ContainerCmdTestMultiJvm3 extends ContainerCmdTest(0)

// DEOPSCSW-43 :  Access Configuration service from any CSW component
// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-168: Deployment of multiple Assemblies and HCDs
// DEOPSCSW-169: Creation of Multiple Components
// DEOPSCSW-171: Starting component from command line
// DEOPSCSW-172: Starting a container from configuration file
// DEOPSCSW-182: Control Life Cycle of Components
// DEOPSCSW-203: Write component-specific verification code
// DEOPSCSW-216: Locate and connect components to send AKKA commands
class ContainerCmdTest(ignore: Int)
    extends LSNodeSpec(config = new TwoMembersAndSeed, mode = "http")
    with MultiNodeHTTPLocationService
    with MockedAuthentication {

  import config._

  implicit val ec: ExecutionContextExecutor = typedSystem.executionContext
  implicit val testKit: TestKitSettings     = TestKitSettings(typedSystem)
  implicit val timeout: Timeout             = 5.seconds

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  override def beforeAll(): Unit = {
    super.beforeAll()
    testFileUtils.deleteServerFiles()
  }

  override def afterAll(): Unit = {
    runOn(seed) {
      try {
        testFileUtils.deleteServerFiles()
      }
      catch {
        case NonFatal(ex) => println(s"Exception in deleting server files - ${ex.printStackTrace()}")
      }
    }
    super.afterAll()
  }

  private def resolveContainer(name: String): ActorRef[ContainerMessage] = {
    val maybeContainerLocF = locationService.resolve(
      AkkaConnection(ComponentId(Prefix(Container, name), ComponentType.Container)),
      5.seconds
    )

    maybeContainerLocF.await.get.containerRef
  }

  private def resolveEtonHcd(): Future[Option[AkkaLocation]] = {
    locationService.resolve(AkkaConnection(models.ComponentId(Prefix(Subsystem.IRIS, "Eton"), ComponentType.HCD)), 2.seconds)
  }

  def createStandaloneTmpFile(): Path = {
    val hcdConfiguration       = scala.io.Source.fromResource("eaton_hcd_standalone.conf").mkString
    val standaloneConfFilePath = Files.createTempFile("eaton_hcd_standalone", ".conf")
    val fileWriter             = new FileWriter(standaloneConfFilePath.toFile, true)
    fileWriter.write(hcdConfiguration)
    fileWriter.close()
    standaloneConfFilePath
  }

  test("should able to start components in container mode and in standalone mode through configuration service | DEOPSCSW-171, DEOPSCSW-168, DEOPSCSW-182, DEOPSCSW-167, DEOPSCSW-43, DEOPSCSW-172, DEOPSCSW-216, DEOPSCSW-203, DEOPSCSW-169, DEOPSCSW-430") {

    // start config server and upload laser_container.conf file
    runOn(seed) {
      val serverWiring = ServerWiring.make(locationService, securityDirectives)
      serverWiring.svnRepo.initSvnRepo()
      serverWiring.httpService.registeredLazyBinding.await

      val configService       = ConfigClientFactory.adminApi(typedSystem, locationService, factory)
      val containerConfigData = ConfigData.fromString(Source.fromResource("laser_container.conf").mkString)

      Await.result(
        configService.create(Paths.get("/laser_container.conf"), containerConfigData, comment = "container"),
        5.seconds
      )

      enterBarrier("config-file-uploaded")
      enterBarrier("container-started")

      val maybeContainerLoc =
        locationService
          .resolve(AkkaConnection(ComponentId(Prefix(Subsystem.Container, "LGSF_Container"), ComponentType.Container)), 5.seconds)
          .await

      maybeContainerLoc.isDefined shouldBe true

      //DEOPSCSW-430: Update AkkaLocation model to take Prefix model instead of Option[String]
      maybeContainerLoc.get.prefix shouldBe Prefix(s"${Container.entryName}.LGSF_Container")

      val containerRef = maybeContainerLoc.get.containerRef

      val componentsProbe               = TestProbe[Components]
      val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]

      containerRef ! GetComponents(componentsProbe.ref)
      val laserContainerComponents = componentsProbe.expectMessageType[Components].components
      laserContainerComponents.size shouldBe 3

      // check that all the components within supervisor moves to Running lifecycle state
      laserContainerComponents
        .foreach { component =>
          component.supervisor ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)
          supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)
        }

      enterBarrier("running")

      enterBarrier("offline")
      enterBarrier("before-shutdown")
      enterBarrier("eton-shutdown")
    }

    runOn(member1) {
      enterBarrier("config-file-uploaded")

      val testProbe = TestProbe[ContainerLifecycleState]

      // withEntries required for multi-node test where seed node is picked up from environment variable
      val containerCmd = new ContainerCmd("laser_container_app", CSW, false)

      // only file path is provided, by default - file will be fetched from configuration service
      // and will be considered as container configuration.
      val args = Array("/laser_container.conf")
      containerCmd.start(args).asInstanceOf[ActorRef[ContainerMessage]]

      val containerRef = resolveContainer("LGSF_Container")

      assertThatContainerIsRunning(containerRef, testProbe, 5.seconds)

      enterBarrier("container-started")
      enterBarrier("running")

      val laserContainerComponentsF: Future[Components] = containerRef ? (x => GetComponents(x))
      val laserContainerComponents                      = laserContainerComponentsF.await.components

      // resolve and send message to component running in different jvm or on different physical machine
      val etonSupervisorF        = resolveEtonHcd()
      val etonSupervisorLocation = Await.result(etonSupervisorF, 15.seconds).get

      val etonSupervisorTypedRef = etonSupervisorLocation.componentRef
      val eatonCompStateProbe    = TestProbe[CurrentState]
      val etonCommandService     = CommandServiceFactory.make(etonSupervisorLocation)

      // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
      etonCommandService.subscribeCurrentState(eatonCompStateProbe.ref ! _)

      import csw.common.components.framework.SampleComponentState._

      val obsId: ObsId          = ObsId("Obs001")
      val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
      // setup to receive Success in validation result
      val setupSuccess: Setup = Setup(successPrefix, CommandName("move.success"), Some(obsId), Set(param))
      val setupFailure: Setup = Setup(failedPrefix, CommandName("move.failure"), Some(obsId), Set(param))

      val laserAssemblySupervisor = laserContainerComponents.head.supervisor
      val laserCompStateProbe     = TestProbe[CurrentState]

      etonCommandService.submitAndWait(setupFailure).map { commandResponse =>
        commandResponse shouldBe Invalid
        eatonCompStateProbe
          .expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(commandValidationChoice))))
      }

      etonCommandService.oneway(setupSuccess).map { _ =>
        eatonCompStateProbe
          .expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(commandValidationChoice))))
        eatonCompStateProbe
          .expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(oneWayCommandChoice))))
        eatonCompStateProbe
          .expectMessage(CurrentState(successPrefix, StateName("testStateName"), Set(choiceKey.set(setupConfigChoice), param)))
      }

      etonSupervisorTypedRef ! Lifecycle(GoOffline)
      Thread.sleep(500)

      enterBarrier("offline")

      val supervisorRef = resolveEtonHcd().await.get.componentRef
      Thread.sleep(500)

      val lifecycleProbe = TestProbe[SupervisorLifecycleState]
      supervisorRef ! GetSupervisorLifecycleState(lifecycleProbe.ref)
      lifecycleProbe.expectMessage(SupervisorLifecycleState.RunningOffline)

      enterBarrier("before-shutdown")
      laserAssemblySupervisor ! ComponentStateSubscription(Subscribe(laserCompStateProbe.ref))
      enterBarrier("eton-shutdown")

      // DEOPSCSW-218: Discover component connection information using Akka protocol
      // Laser assembly is tracking Eton Hcd which is running on member2 (different jvm than this)
      // When Eton Hcd shutdowns, laser assembly receives LocationRemoved event
      laserCompStateProbe.expectMessage(
        10.seconds,
        CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationRemovedChoice)))
      )
    }

    runOn(member2) {
      enterBarrier("config-file-uploaded")

      val testProbe = TestProbe[SupervisorLifecycleState]

      val containerCmd = new ContainerCmd("eaton_hcd_standalone_app", CSW, false)

      // this step is required for multi-node, as eaton_hcd_standalone.conf file is not directly available
      // when sbt-assembly creates fat jar
      val standaloneConfFilePath = createStandaloneTmpFile()

      val args = Array("--standalone", "--local", standaloneConfFilePath.toString)
      containerCmd.start(args).asInstanceOf[ActorRef[ComponentMessage]]

      val supervisorRef = resolveEtonHcd().await.get.componentRef

      assertThatSupervisorIsRunning(supervisorRef, testProbe, 5.seconds)
      enterBarrier("container-started")
      enterBarrier("running")

      enterBarrier("offline")

      enterBarrier("before-shutdown")
      supervisorRef ! Shutdown
      enterBarrier("eton-shutdown")

      Files.delete(standaloneConfFilePath)
    }
    enterBarrier("end")
  }

}
