package org.tmt.esw.basic

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.ContainerMessage
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.Setup
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.WordSpecLike

import scala.concurrent.Await
import scala.concurrent.duration._

//noinspection ScalaStyle
//#intro
class SampleIntegrationTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with WordSpecLike {

  import frameworkTestKit.frameworkWiring._

  private val containerConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.Container, "SampleContainer"), ComponentType.Container)
  )
  private var containerLocation: AkkaLocation = _
  private val assemblyConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.ESW, "SampleAssembly"), ComponentType.Assembly)
  )
  private var assemblyLocation: AkkaLocation = _
  private val hcdConnection                  = AkkaConnection(ComponentId(Prefix(Subsystem.ESW, "SampleHcd"), ComponentType.HCD))
  private var hcdLocation: AkkaLocation      = _

  private var containerRef: ActorRef[ContainerMessage] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    containerRef = spawnContainer(com.typesafe.config.ConfigFactory.load("BasicSampleContainer.conf"))
  }

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  private implicit val timeout: Timeout                                = 12.seconds

  //#locate

  import org.tmt.esw.basic.shared.SampleInfo._

  import scala.concurrent.duration._

  "startupContainer" must {
    "Ensure container is locatable using Location Service" in {
      containerLocation = Await.result(locationService.resolve(containerConnection, 10.seconds), 10.seconds).get
      containerLocation.connection shouldBe containerConnection
    }

    "Ensure Assembly is locatable using Location Service" in {
      assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get
      assemblyLocation.connection shouldBe assemblyConnection
    }

    "Ensure HCD is locatable using Location Service" in {
      hcdLocation = Await.result(locationService.resolve(hcdConnection, 10.seconds), 10.seconds).get
      hcdLocation.connection shouldBe hcdConnection
    }
  }

  "sending sleep" must {
    "Accept a valid sleep command" in {
      val setup: Setup = Setup(testPrefix, sleep, None).add(setSleepTime(1500))

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)

      val x = Await.result(assemblyCS.submitAndWait(setup), 10.seconds)
      println("X: " + x)
    }

    "Accept an immediate command" in {
      val setup: Setup = Setup(testPrefix, immediateCommand, None)

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)

      val x = Await.result(assemblyCS.submitAndWait(setup), 10.seconds)
      println("X: " + x)
    }

    "Accept a short, medium and long command" in {
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

    "Accept a complex command and wait for all to finish" in {
      val complexSetup: Setup = Setup(testPrefix, complexCommand, None)

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)

      Await.result(assemblyCS.submitAndWait(complexSetup), 10.seconds) shouldBe a[Completed]
      println("Got final completed")
    }
  }

}
