package csw.common.framework.integration

import akka.actor
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.common.framework.models.SupervisorCommonMessage.GetSupervisorMode
import csw.common.framework.models.{Shutdown, SupervisorExternalMessage}
import csw.services.location.commons.ClusterSettings
import csw.services.location.models.ComponentType.HCD
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, LocationRemoved, TrackingEvent}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-216: Locate and connect components to send AKKA commands
class StandaloneComponentTest extends FunSuite with Matchers with BeforeAndAfterAll {

  // ActorSystem for testing. This acts as a seed node
  implicit val seedActorSystem: actor.ActorSystem = ClusterSettings().onPort(3552).system
  implicit val typedSystem: ActorSystem[_]        = seedActorSystem.toTyped
  implicit val testKitSettings: TestKitSettings   = TestKitSettings(typedSystem)
  implicit val mat: Materializer                  = ActorMaterializer()
  private val locationService: LocationService    = LocationServiceFactory.withSystem(seedActorSystem)

  // ActorSystem for standalone component. Component will join seed node created above.
  private val hcdActorSystem: actor.ActorSystem = ClusterSettings().joinLocal(3552).system

  override protected def afterAll(): Unit = Await.result(seedActorSystem.terminate(), 5.seconds)

  test("should start a component in standalone mode and register with location service") {

    // start component in standalone mode
    val wiring: FrameworkWiring = FrameworkWiring.make(hcdActorSystem)
    Standalone.spawn(ConfigFactory.load("standalone.conf"), wiring)

    val supervisorModeProbe = TestProbe[SupervisorMode]("supervisor-probe")
    val akkaConnection      = AkkaConnection(ComponentId("IFS_Detector", HCD))

    // verify component gets registered with location service
    val eventualLocation = locationService.resolve(akkaConnection, 5.seconds)
    val maybeLocation    = Await.result(eventualLocation, 5.seconds)

    maybeLocation.isDefined shouldBe true
    val resolvedAkkaLocation = maybeLocation.get
    resolvedAkkaLocation.connection shouldBe akkaConnection

    val supervisorRef = resolvedAkkaLocation.typedRef[SupervisorExternalMessage]

    Thread.sleep(500)
    supervisorRef ! GetSupervisorMode(supervisorModeProbe.ref)
    supervisorModeProbe.expectMsg(SupervisorMode.Running)

    val (_, akkaProbe) =
      locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()

    // on shutdown, component unregisters from location service
    supervisorRef ! Shutdown
    akkaProbe.requestNext(LocationRemoved(akkaConnection))
  }
}
