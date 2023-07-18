/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.containercmd

import java.io.FileWriter
import java.nio.file.{Files, Path, Paths}

import org.apache.pekko.actor.testkit.typed.TestKitSettings
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.PekkoLocationExt.RichPekkoLocation
import csw.command.client.messages.ComponentCommonMessage.{ComponentStateSubscription, GetSupervisorLifecycleState}
import csw.command.client.messages.ContainerCommonMessage.GetComponents
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.Shutdown
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
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.location.server.http.MultiNodeHTTPLocationService
import csw.params.commands.CommandResponse.Invalid
import csw.params.commands.{CommandName, Setup}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.ObsId
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.Subsystem.{CSW, Container}
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.Source
import scala.util.Try

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
// DEOPSCSW-216: Locate and connect components to send PEKKO commands
class ContainerCmdTest(ignore: Int)
    extends LSNodeSpec(config = new TwoMembersAndSeed, mode = "http")
    with MultiNodeHTTPLocationService
    with MockedAuthentication
    with ScalaFutures
    with OptionValues {

  import config._

  implicit val ec: ExecutionContextExecutor = typedSystem.executionContext
  implicit val testKit: TestKitSettings     = TestKitSettings(typedSystem)
  private val timeoutDuration               = 10.seconds
  implicit val timeout: Timeout             = timeoutDuration

  private val testFileUtils                            = new TestFileUtils(new Settings(ConfigFactory.load()))
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeoutDuration, 50.milli)

  override def beforeAll(): Unit = {
    super.beforeAll()
    testFileUtils.deleteServerFiles()
  }

  override def afterAll(): Unit = {
    runOn(seed) {
      Try(testFileUtils.deleteServerFiles()).recover(_.printStackTrace())
    }
    super.afterAll()
  }

  private def resolve(prefix: Prefix, compType: ComponentType) =
    locationService.resolve(PekkoConnection(ComponentId(prefix, compType)), timeoutDuration)

  private def resolveLgsfContainer() = resolve(Prefix(Container, "LGSF_Container"), ComponentType.Container)
  private def resolveEtonHcd()       = resolve(Prefix(Subsystem.IRIS, "Eton"), ComponentType.HCD)

  def createStandaloneTmpFile(): Path = {
    val hcdConfiguration       = scala.io.Source.fromResource("eaton_hcd_standalone.conf").mkString
    val standaloneConfFilePath = Files.createTempFile("eaton_hcd_standalone", ".conf")
    val fileWriter             = new FileWriter(standaloneConfFilePath.toFile, true)
    fileWriter.write(hcdConfiguration)
    fileWriter.close()
    standaloneConfFilePath
  }

  test(
    s"${testPrefix} should able to start components in container mode and in standalone mode through configuration service" +
      " | DEOPSCSW-171, DEOPSCSW-168, DEOPSCSW-182, DEOPSCSW-167, DEOPSCSW-43, DEOPSCSW-172, DEOPSCSW-216, DEOPSCSW-203, DEOPSCSW-169, DEOPSCSW-430, CSW-177"
  ) {

    // start config server and upload laser_container.conf file
    runOn(seed) {
      val serverWiring = ServerWiring.make(locationService, securityDirectives)
      serverWiring.svnRepo.initSvnRepo()
      serverWiring.httpService.registeredLazyBinding.futureValue

      val configService       = ConfigClientFactory.adminApi(typedSystem, locationService, factory)
      val containerConfigData = ConfigData.fromString(Source.fromResource("laser_container.conf").mkString)

      configService.create(Paths.get("/laser_container.conf"), containerConfigData, comment = "container").futureValue

      enterBarrier("config-file-uploaded")
      enterBarrier("container-started")

      val maybeContainerLoc = resolveLgsfContainer().futureValue

      // DEOPSCSW-430: Update PekkoLocation model to take Prefix model instead of Option[String]
      maybeContainerLoc.map(_.prefix).value shouldBe Prefix(s"${Container.entryName}.LGSF_Container")
      val containerRef = maybeContainerLoc.value.containerRef

      val componentsProbe               = TestProbe[Components]()
      val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]()

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

      val testProbe    = TestProbe[ContainerLifecycleState]()
      val containerCmd = new ContainerCmd("laser_container_app", CSW, false)

      // only file path is provided, by default - file will be fetched from configuration service
      // and will be considered as container configuration.
      val args = Array("/laser_container.conf")
      containerCmd.start(args)
      val containerRef = resolveLgsfContainer().futureValue.value.containerRef
      assertThatContainerIsRunning(containerRef, testProbe, 5.seconds)
      enterBarrier("container-started")
      enterBarrier("running")

      val laserContainerComponentsF: Future[Components] = containerRef ? (x => GetComponents(x))
      val laserContainerComponents                      = laserContainerComponentsF.futureValue.components

      // resolve and send message to component running in different jvm or on different physical machine
      val etonSupervisorLocation = resolveEtonHcd().futureValue.value

      val etonSupervisorTypedRef = etonSupervisorLocation.componentRef
      val eatonCompStateProbe    = TestProbe[CurrentState]()
      val etonCommandService     = CommandServiceFactory.make(etonSupervisorLocation)

      // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
      etonCommandService.subscribeCurrentState(eatonCompStateProbe.ref ! _)

      import csw.common.components.framework.SampleComponentState._

      val obsId: ObsId          = ObsId("2020A-001-123")
      val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
      // setup to receive Success in validation result
      val setupSuccess: Setup = Setup(successPrefix, CommandName("move.success"), Some(obsId), Set(param))
      val setupFailure: Setup = Setup(failedPrefix, CommandName("move.failure"), Some(obsId), Set(param))

      val laserAssemblySupervisor = laserContainerComponents.head.supervisor
      val laserCompStateProbe     = TestProbe[CurrentState]()

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

      val supervisorRef = resolveEtonHcd().futureValue.value.componentRef
      Thread.sleep(500)

      val lifecycleProbe = TestProbe[SupervisorLifecycleState]()
      supervisorRef ! GetSupervisorLifecycleState(lifecycleProbe.ref)
      lifecycleProbe.expectMessage(SupervisorLifecycleState.RunningOffline)

      enterBarrier("before-shutdown")
      laserAssemblySupervisor ! ComponentStateSubscription(Subscribe(laserCompStateProbe.ref))
      enterBarrier("eton-shutdown")

      // DEOPSCSW-218: Discover component connection information using Pekko protocol
      // Laser assembly is tracking Eton Hcd which is running on member2 (different jvm than this)
      // When Eton Hcd shutdowns, laser assembly receives LocationRemoved event
      laserCompStateProbe.expectMessage(
        10.seconds,
        CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(pekkoLocationRemovedChoice)))
      )
    }

    runOn(member2) {
      enterBarrier("config-file-uploaded")

      val testProbe = TestProbe[SupervisorLifecycleState]()

      val containerCmd = new ContainerCmd("eaton_hcd_standalone_app", CSW, false)

      // this step is required for multi-node, as eaton_hcd_standalone.conf file is not directly available
      // when sbt-assembly creates fat jar
      val standaloneConfFilePath = createStandaloneTmpFile()

      val args = Array("--local", standaloneConfFilePath.toString)
      containerCmd.start(args)
      val supervisorRef = resolveEtonHcd().futureValue.value.componentRef
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
