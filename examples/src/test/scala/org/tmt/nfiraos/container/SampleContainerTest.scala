package org.tmt.nfiraos.container

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.ContainerMessage
import csw.command.client.messages.SupervisorLockMessage.Unlock
import csw.command.client.models.framework.LockingResponse.{LockAcquired, LockReleased}
import csw.command.client.models.framework.{LockingResponse, LockingResponseTest}
import csw.common.utils.LockCommandFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType, Location}
import csw.params.commands.CommandResponse.{Cancelled, Completed, Locked, Started}
import csw.params.commands.Setup
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.WordSpecLike

import scala.concurrent.Await
import scala.concurrent.duration._

//#intro
class SampleContainerTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with WordSpecLike {
  import frameworkTestKit.frameworkWiring._

  private val containerConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.Container, "SampleContainer"), ComponentType.Container)
  )
  private var containerLocation: AkkaLocation = _
  private val assemblyConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.NFIRAOS, "sampleassembly"), ComponentType.Assembly)
  )
  private var assemblyLocation: AkkaLocation = _
  private val hcdConnection                  = AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "samplehcd"), ComponentType.HCD))
  private var hcdLocation: AkkaLocation      = _

  private var containerRef: ActorRef[ContainerMessage] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    containerRef = spawnContainer(com.typesafe.config.ConfigFactory.load("SampleContainer.conf"))
  }

  private implicit val actorSystem      = frameworkTestKit.actorSystem
  private implicit val timeout: Timeout = 12.seconds

  //#locate
  import scala.concurrent.duration._
  import org.tmt.nfiraos.shared.SampleInfo._

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
    /*
    "Accept a sleepWithWorker command and then cancel it" in {
      val setup:Setup = Setup(testPrefix, sleepToHcd, None)

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)

      val r1 = Await.result(assemblyCS.submit(setup.add(setSleepTime(4500))), 10.seconds)
      println("SampleContainerTest submit response: " + r1)
      r1 shouldBe a[Started]
      Thread.sleep(2000)
      val cancelSetup = Setup(testPrefix, cancelWorker, None).add(cancelKey.set(r1.runId.id))
      val r2 = Await.result(assemblyCS.submitAndWait(cancelSetup), 10.seconds)
      println("SampleContainerTest cancel response: " + r2)
      r2 shouldBe a[Completed]

      val r3 = Await.result(assemblyCS.queryFinal(r1.runId), 10.seconds)
      println("SampleContainerTest queryFinal response: " + r3)
      r3 shouldBe a[Cancelled]
    }
     */
    "Accept a longCommand and then cancel it" in {
      val setup: Setup = Setup(testPrefix, longCommand, None)

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)

      val r1 = Await.result(assemblyCS.submit(setup), 10.seconds)
      println("SampleContainerTest submit response: " + r1)
      r1 shouldBe a[Started]
      Thread.sleep(2000)
      val cancelSetup = Setup(testPrefix, cancelLongCommand, None).add(cancelKey.set(r1.runId.id))
      val r2          = Await.result(assemblyCS.submitAndWait(cancelSetup), 10.seconds)
      println("SampleContainerTest cancel response: " + r2)
      r2 shouldBe a[Completed]

      val r3 = Await.result(assemblyCS.queryFinal(r1.runId), 10.seconds)
      println("SampleContainerTest queryFinal response: " + r3)
      r3 shouldBe a[Cancelled]
    }
    /*
    "Accept a short, medium and long command" in {
      val shortSetup: Setup = Setup(testPrefix, shortCommand, None)
      val mediumSetup: Setup = Setup(testPrefix, mediumCommand, None)
      val longSetup: Setup = Setup(testPrefix, longCommand, None)

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)

      Await.result(assemblyCS.submitAndWait(shortSetup), 10.seconds) shouldBe a[Completed]
      Await.result(assemblyCS.submitAndWait(mediumSetup), 10.seconds) shouldBe a[Completed]
      Await.result(assemblyCS.submitAndWait(longSetup), 10.seconds) shouldBe a[Completed]

    }
     */
    import csw.location.server.commons.TestFutureExtension.RichFuture
    import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation

    "Accept a complex command and wait for all to finish" in {
      val complexSetup: Setup = Setup(testPrefix, complexCommand, None)

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)

      Await.result(assemblyCS.submitAndWait(complexSetup), 10.seconds) shouldBe a[Completed]
      println("Got final completed")
    }

    "Lock HCD and send command and wait" in {
      val lockingStateProbe = TestProbe[LockingResponse]

      val hcdLocation2: AkkaLocation = locationService.resolve(hcdConnection, 5.seconds).await.get
      hcdLocation2.componentRef ! LockCommandFactory.make(testPrefix, lockingStateProbe.ref)
      lockingStateProbe.expectMessage(LockAcquired)

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)
      val setup: Setup = Setup(testPrefix, sleep, None).add(setSleepTime(1500))

      Await.result(assemblyCS.submitAndWait(setup), 10.seconds) shouldBe a[Locked]
      println("Got final locked")

      hcdLocation2.componentRef ! Unlock(testPrefix, lockingStateProbe.ref)
      lockingStateProbe.expectMessage(LockReleased)

      Await.result(assemblyCS.submitAndWait(setup), 10.seconds) shouldBe a[Completed]
    }
  }

}
