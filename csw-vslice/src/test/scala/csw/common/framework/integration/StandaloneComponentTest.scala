package csw.common.framework.integration

import akka.actor
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.common.framework.models.SupervisorCommonMessage.GetSupervisorMode
import csw.common.framework.models.SupervisorExternalMessage
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models.ComponentId
import csw.services.location.models.ComponentType.HCD
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class StandaloneComponentTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private val untypedSystem: actor.ActorSystem  = ClusterAwareSettings.system
  implicit val typedSystem: ActorSystem[_]      = untypedSystem.toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(typedSystem)

  private val locationService: LocationService = LocationServiceFactory.withSystem(untypedSystem)
  override protected def afterAll(): Unit      = Await.result(untypedSystem.terminate(), 5.seconds)

  test("should start multiple components withing a single container and able to accept lifecycle messages") {

    val supervisorModeProbe = TestProbe[SupervisorMode]
    val wiring              = FrameworkWiring.make(untypedSystem, locationService)
    // start a container and verify it moves to running mode
    val hcdRef = Standalone.spawn(ConfigFactory.load("standalone.conf"), wiring)

    hcdRef ! GetSupervisorMode(supervisorModeProbe.ref)
    supervisorModeProbe.expectMsg(SupervisorMode.Idle)

    val eventualLocation = locationService.resolve(AkkaConnection(ComponentId("IFS_Detector", HCD)), 5.seconds)
    val maybeLocation    = Await.result(eventualLocation, 5.seconds)

    maybeLocation.isDefined shouldBe true
    val supervisorRef = maybeLocation.get.typedRef[SupervisorExternalMessage]

    supervisorRef ! GetSupervisorMode(supervisorModeProbe.ref)
    supervisorModeProbe.expectMsg(SupervisorMode.Running)
  }
}
