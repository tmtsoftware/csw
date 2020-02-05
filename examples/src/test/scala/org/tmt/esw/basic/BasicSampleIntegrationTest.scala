package org.tmt.esw.basic

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.ContainerMessage
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error}
import csw.params.commands.Setup
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.WordSpecLike
import org.tmt.esw.basic.shared.SampleInfo._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

//noinspection ScalaStyle
//#intro
class BasicSampleIntegrationTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with WordSpecLike {
  import frameworkTestKit.frameworkWiring._

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  private implicit val ec: ExecutionContext                            = actorSystem.executionContext
  private implicit val timeout: Timeout                                = 12.seconds

  private val containerConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.Container, "SampleContainer"), ComponentType.Container)
  )

  private val assemblyConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.ESW, "SampleAssembly"), ComponentType.Assembly)
  )
  private val hcdConnection = AkkaConnection(ComponentId(Prefix(Subsystem.ESW, "SampleHcd"), ComponentType.HCD))

  private var containerRef: ActorRef[ContainerMessage] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    containerRef = spawnContainer(com.typesafe.config.ConfigFactory.load("BasicSampleContainer.conf"))
  }

  "startupContainer" must {
    "Ensure container is locatable using Location Service" in {
      val containerLocation = Await.result(locationService.resolve(containerConnection, 10.seconds), 10.seconds).get
      containerLocation.connection shouldBe containerConnection
    }

    "Ensure Assembly is locatable using Location Service" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get
      assemblyLocation.connection shouldBe assemblyConnection
    }

    "Ensure HCD is locatable using Location Service" in {
      val hcdLocation = Await.result(locationService.resolve(hcdConnection, 10.seconds), 10.seconds).get
      hcdLocation.connection shouldBe hcdConnection
    }
  }

  "assembly and HCD" must {
    "accept and execute a valid sleep command" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get

      val testSleep    = 1500L
      val setup: Setup = Setup(testPrefix, sleep, None).add(setSleepTime(testSleep))
      val assemblyCS   = CommandServiceFactory.make(assemblyLocation)

      val sr: Completed = (Await.result(assemblyCS.submitAndWait(setup), 10.seconds)).asInstanceOf[Completed]
      sr.result(resultKey).head shouldBe testSleep
    }

    "Accept and execute an immediate command" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get

      val setup: Setup = Setup(testPrefix, immediateCommand, None)
      val assemblyCS   = CommandServiceFactory.make(assemblyLocation)

      Await.result(assemblyCS.submitAndWait(setup), 10.seconds) shouldBe a[Completed]
    }

    "Accept and execute a short, medium and long command" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get

      val shortSetup: Setup  = Setup(testPrefix, shortCommand, None)
      val mediumSetup: Setup = Setup(testPrefix, mediumCommand, None)
      val longSetup: Setup   = Setup(testPrefix, longCommand, None)

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)

      val r1: Completed = Await.result(assemblyCS.submitAndWait(shortSetup), 10.seconds).asInstanceOf[Completed]
      r1.result(resultKey).head shouldBe shortSleepPeriod

      val r2: Completed = Await.result(assemblyCS.submitAndWait(mediumSetup), 10.seconds).asInstanceOf[Completed]
      r2.result(resultKey).head shouldBe mediumSleepPeriod

      val r3: Completed = Await.result(assemblyCS.submitAndWait(longSetup), 10.seconds).asInstanceOf[Completed]
      r3.result(resultKey).head shouldBe longSleepPeriod
    }

    "Accept and execute a complex command and wait for all to finish" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get

      val complexSetup: Setup = Setup(testPrefix, complexCommand, None)
      val assemblyCS          = CommandServiceFactory.make(assemblyLocation)

      Await.result(assemblyCS.submitAndWait(complexSetup), 10.seconds) shouldBe a[Completed]
    }
  }
}
