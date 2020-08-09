import java.net.URI

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import csw.admin.api.UnresolvedAkkaLocationException
import csw.admin.impl.AdminServiceImpl
import csw.command.client.messages.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.location.api.models
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, Metadata}
import csw.location.api.scaladsl.LocationService
import csw.logging.models.{Level, LogMetadata}
import csw.prefix.models.{Prefix, Subsystem}
import org.mockito.MockitoSugar._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

class AdminServiceImplTest extends AnyFunSuite with Matchers with BeforeAndAfterEach {

  var actorTestKit: ActorTestKit = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    actorTestKit = ActorTestKit()
  }

  override def afterEach(): Unit = {
    super.afterEach()
    actorTestKit.shutdownTestKit()
  }

  test("getLogMetadata should get log metadata when component is discovered and it responds with metadata") {
    implicit val system: ActorSystem[_]  = actorTestKit.system
    val locationService: LocationService = mock[LocationService]
    val expectedLogMetadata              = LogMetadata(Level.FATAL, Level.ERROR, Level.WARN, Level.DEBUG)
    val probe = actorTestKit.spawn(Behaviors.receiveMessage[GetComponentLogMetadata] {
      case GetComponentLogMetadata(replyTo) =>
        replyTo ! expectedLogMetadata
        Behaviors.same
    })
    val adminService: AdminServiceImpl = new AdminServiceImpl(locationService)
    val componentId                    = ComponentId(Prefix(Subsystem.AOESW, "test_component"), ComponentType.Assembly)
    when(locationService.find(AkkaConnection(componentId))).thenReturn(
      Future.successful(Some(AkkaLocation(AkkaConnection(componentId), new URI(probe.path.toString), Metadata.empty)))
    )
    val actualLogMetadata = adminService.getLogMetadata(componentId).futureValue
    actualLogMetadata shouldBe expectedLogMetadata
  }

  test("getLogMetadata should get log metadata when sequencer is discovered and it responds with metadata") {
    implicit val system: ActorSystem[_]  = actorTestKit.system
    val locationService: LocationService = mock[LocationService]
    val expectedLogMetadata              = LogMetadata(Level.FATAL, Level.ERROR, Level.WARN, Level.DEBUG)
    val probe = actorTestKit.spawn(Behaviors.receiveMessage[GetComponentLogMetadata] {
      case GetComponentLogMetadata(replyTo) =>
        replyTo ! expectedLogMetadata
        Behaviors.same
    })
    val adminService: AdminServiceImpl = new AdminServiceImpl(locationService)
    val componentId                    = models.ComponentId(Prefix(Subsystem.AOESW, "test_sequencer"), ComponentType.Sequencer)
    when(locationService.find(AkkaConnection(componentId)))
      .thenReturn(
        Future.successful(Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.path.toString), Metadata.empty)))
      )
    val actualLogMetadata = adminService.getLogMetadata(componentId).futureValue
    actualLogMetadata shouldBe expectedLogMetadata
  }

  test("getLogMetadata should fail with UnresolvedAkkaLocationException when componentId is not not resolved") {
    implicit val system: ActorSystem[_]  = actorTestKit.system
    val locationService: LocationService = mock[LocationService]
    val adminService: AdminServiceImpl   = new AdminServiceImpl(locationService)
    val componentId                      = models.ComponentId(Prefix(Subsystem.AOESW, "test_sequencer"), ComponentType.Sequencer)
    when(locationService.find(AkkaConnection(componentId))).thenReturn(Future.successful(None))
    intercept[UnresolvedAkkaLocationException] {
      Await.result(adminService.getLogMetadata(componentId), 100.millis)
    }
  }

  test("setLogLevel should get log metadata when component is discovered") {
    implicit val system: ActorSystem[_]  = actorTestKit.system
    val locationService: LocationService = mock[LocationService]
    val probe                            = actorTestKit.createTestProbe[SetComponentLogLevel]()
    val adminService: AdminServiceImpl   = new AdminServiceImpl(locationService)
    val componentId                      = models.ComponentId(Prefix(Subsystem.AOESW, "test_component"), ComponentType.Assembly)
    when(locationService.find(AkkaConnection(componentId))).thenReturn(
      Future.successful(Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.ref.path.toString), Metadata.empty)))
    )
    adminService.setLogLevel(componentId, Level.FATAL)
    probe.expectMessage(500.millis, SetComponentLogLevel(Level.FATAL))
  }

  test("setLogLevel should get log metadata when sequencer is discovered") {
    implicit val system: ActorSystem[_]  = actorTestKit.system
    val locationService: LocationService = mock[LocationService]
    val probe                            = actorTestKit.createTestProbe[SetComponentLogLevel]()
    val adminService: AdminServiceImpl   = new AdminServiceImpl(locationService)
    val componentId                      = models.ComponentId(Prefix(Subsystem.AOESW, "test_component"), ComponentType.Sequencer)
    when(locationService.find(AkkaConnection(componentId))).thenReturn(
      Future.successful(Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.ref.path.toString), Metadata.empty)))
    )
    adminService.setLogLevel(componentId, Level.FATAL)
    probe.expectMessage(500.millis, SetComponentLogLevel(Level.FATAL))
  }

  test("setLogLevel should fail with UnresolvedAkkaLocationException when componentId is not not resolved") {
    implicit val system: ActorSystem[_]  = actorTestKit.system
    val locationService: LocationService = mock[LocationService]
    val adminService: AdminServiceImpl   = new AdminServiceImpl(locationService)
    val componentId                      = models.ComponentId(Prefix(Subsystem.AOESW, "test_component"), ComponentType.Sequencer)
    when(locationService.find(AkkaConnection(componentId))).thenReturn(Future.successful(None))
    intercept[UnresolvedAkkaLocationException] {
      Await.result(adminService.setLogLevel(componentId, Level.FATAL), 100.millis)
    }
  }
}
