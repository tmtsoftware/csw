package example.tutorial.full

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.ContainerMessage
import csw.command.client.messages.SupervisorLockMessage.Unlock
import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.LockingResponse.{LockAcquired, LockReleased}
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.api.models.Connection.AkkaConnection
import csw.params.commands.CommandResponse.{Cancelled, Completed, Locked, Started}
import csw.params.commands.Setup
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import example.tutorial.moderate.LockCommandFactory

import scala.concurrent.Await
import scala.concurrent.duration._

//noinspection ScalaStyle
//#intro
class FullSampleIntegrationTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with AnyWordSpecLike {

  import frameworkTestKit.frameworkWiring._

  private val containerConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.Container, "SampleContainer"), ComponentType.Container)
  )
  private val assemblyConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.CSW, "sample"), ComponentType.Assembly)
  )
  private val hcdConnection = AkkaConnection(ComponentId(Prefix(Subsystem.CSW, "samplehcd"), ComponentType.HCD))

  private var containerRef: ActorRef[ContainerMessage] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    containerRef = spawnContainer(com.typesafe.config.ConfigFactory.load("FullSampleContainer.conf"))
  }

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  private implicit val timeout: Timeout                                = 12.seconds

  //#locate

  import example.tutorial.moderate.shared.SampleInfo._

  import scala.concurrent.duration._

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

  "sending sleep" must {
    "Accept a valid sleep command" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get

      val setup: Setup = Setup(testPrefix, sleep, None).add(setSleepTime(1500))
      val assemblyCS   = CommandServiceFactory.make(assemblyLocation)

      Await.result(assemblyCS.submitAndWait(setup), 10.seconds) shouldBe a[Completed]
    }

    "Accept an immediate command" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get

      val setup: Setup = Setup(testPrefix, immediateCommand, None)
      val assemblyCS   = CommandServiceFactory.make(assemblyLocation)

      Await.result(assemblyCS.submitAndWait(setup), 10.seconds) shouldBe a[Completed]
    }

    "Accept a longCommand and then cancel it" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get

      val setup: Setup = Setup(testPrefix, sleep, None).madd(setSleepTime(4500))
      val assemblyCS   = CommandServiceFactory.make(assemblyLocation)

      val r1 = Await.result(assemblyCS.submit(setup), 10.seconds)
      r1 shouldBe a[Started]
      // Wait for 2 seconds
      Thread.sleep(2000)
      // Cancel command should complete
      val cancelSetup = Setup(testPrefix, cancelLongCommand, None).add(cancelKey.set(r1.runId.id))
      val r2          = Await.result(assemblyCS.submitAndWait(cancelSetup), 10.seconds)
      r2 shouldBe a[Completed]

      // Initial long command returns Cancelled
      val r3 = Await.result(assemblyCS.queryFinal(r1.runId), 10.seconds)
      r3 shouldBe a[Cancelled]
    }

    "Accept a short, medium and long command" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get

      val shortSetup: Setup  = Setup(testPrefix, shortCommand, None)
      val mediumSetup: Setup = Setup(testPrefix, mediumCommand, None)
      val longSetup: Setup   = Setup(testPrefix, longCommand, None)

      val assemblyCS = CommandServiceFactory.make(assemblyLocation)

      Await.result(assemblyCS.submitAndWait(shortSetup), 10.seconds) shouldBe a[Completed]
      Await.result(assemblyCS.submitAndWait(mediumSetup), 10.seconds) shouldBe a[Completed]
      Await.result(assemblyCS.submitAndWait(longSetup), 10.seconds) shouldBe a[Completed]
    }

    "Accept a complex command and wait for all to finish" in {
      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get

      val complexSetup: Setup = Setup(testPrefix, complexCommand, None)
      val assemblyCS          = CommandServiceFactory.make(assemblyLocation)

      Await.result(assemblyCS.submitAndWait(complexSetup), 10.seconds) shouldBe a[Completed]
    }

    import csw.command.client.extensions.AkkaLocationExt._

    import scala.concurrent.Await

    "Lock HCD and send command then unlock and send again" in {
      val lockingStateProbe = TestProbe[LockingResponse]()

      val hcdLocation2: AkkaLocation = Await.result(locationService.resolve(hcdConnection, 5.seconds), 5.seconds).get
      hcdLocation2.componentRef ! LockCommandFactory.make(testPrefix, lockingStateProbe.ref)
      lockingStateProbe.expectMessage(LockAcquired)

      val assemblyLocation = Await.result(locationService.resolve(assemblyConnection, 10.seconds), 10.seconds).get
      val assemblyCS       = CommandServiceFactory.make(assemblyLocation)
      val setup: Setup     = Setup(testPrefix, sleep, None).add(setSleepTime(1500))

      Await.result(assemblyCS.submitAndWait(setup), 10.seconds) shouldBe a[Locked]

      // Now unlock and command
      hcdLocation2.componentRef ! Unlock(testPrefix, lockingStateProbe.ref)
      lockingStateProbe.expectMessage(LockReleased)

      Await.result(assemblyCS.submitAndWait(setup), 10.seconds) shouldBe a[Completed]
    }
  }

}
