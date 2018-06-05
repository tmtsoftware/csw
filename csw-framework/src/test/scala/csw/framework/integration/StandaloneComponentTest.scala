package csw.framework.integration

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.TestProbe
import com.persist.JsonOps
import com.persist.JsonOps.JsonObject
import com.typesafe.config.ConfigFactory
import csw.common.FrameworkAssertions._
import csw.common.components.framework.SampleComponentHandlers
import csw.common.components.framework.SampleComponentState._
import csw.common.utils.TestAppender
import csw.commons.tags.LoggingSystemSensitive
import csw.framework.internal.component.ComponentBehavior
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.framework.SupervisorLifecycleState
import csw.messages.location.ComponentType.HCD
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.params.states.{CurrentState, StateName}
import csw.messages.scaladsl.SupervisorContainerCommonMessages.Shutdown
import csw.services.command.scaladsl.CommandService
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import csw.services.logging.internal.LoggingLevels.INFO
import csw.services.logging.internal.LoggingSystem
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-177: Hooks for lifecycle management
// DEOPSCSW-216: Locate and connect components to send AKKA commands
@LoggingSystemSensitive
class StandaloneComponentTest extends FunSuite with Matchers with BeforeAndAfterAll {

  // ActorSystem for testing. This acts as a seed node
  implicit val seedActorSystem: actor.ActorSystem = ClusterSettings().onPort(3553).system
  implicit val typedSystem: ActorSystem[_]        = seedActorSystem.toTyped
  implicit val testKitSettings: TestKitSettings   = TestKitSettings(typedSystem)
  implicit val mat: Materializer                  = ActorMaterializer()
  private val locationService: LocationService    = LocationServiceFactory.withSystem(seedActorSystem)

  // ActorSystem for standalone component. Component will join seed node created above.
  private val hcdActorSystem: actor.ActorSystem = ClusterSettings().joinLocal(3553).system

  // all log messages will be captured in log buffer
  private val logBuffer                    = mutable.Buffer.empty[JsonObject]
  private val testAppender                 = new TestAppender(x ⇒ logBuffer += JsonOps.Json(x.toString).asInstanceOf[JsonObject])
  private var loggingSystem: LoggingSystem = _

  override protected def beforeAll(): Unit = {
    loggingSystem = new LoggingSystem("standalone", "1.0", "localhost", seedActorSystem)
    loggingSystem.setAppenders(List(testAppender))
  }

  override protected def afterAll(): Unit = Await.result(seedActorSystem.terminate(), 5.seconds)

  test("should start a component in standalone mode and register with location service") {

    // start component in standalone mode
    val wiring: FrameworkWiring = FrameworkWiring.make(hcdActorSystem)
    Standalone.spawn(ConfigFactory.load("standalone.conf"), wiring)

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]("supervisor-lifecycle-state-probe")
    val supervisorStateProbe          = TestProbe[CurrentState]("supervisor-state-probe")
    val akkaConnection                = AkkaConnection(ComponentId("IFS_Detector", HCD))

    // verify component gets registered with location service
    val eventualLocation = locationService.resolve(akkaConnection, 5.seconds)
    val maybeLocation    = Await.result(eventualLocation, 5.seconds)

    maybeLocation.isDefined shouldBe true
    val resolvedAkkaLocation = maybeLocation.get
    resolvedAkkaLocation.connection shouldBe akkaConnection

    val supervisorRef = resolvedAkkaLocation.componentRef
    assertThatSupervisorIsRunning(supervisorRef, supervisorLifecycleStateProbe, 5.seconds)

    val supervisorCommandService = new CommandService(resolvedAkkaLocation)

    val (_, akkaProbe) = locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
    akkaProbe.requestNext() shouldBe a[LocationUpdated]

    // on shutdown, component unregisters from location service
    supervisorCommandService.subscribeCurrentState(supervisorStateProbe.ref ! _)
    supervisorRef ! Shutdown

    // this proves that ComponentBehaviors postStop signal gets invoked
    // as onShutdownHook of TLA gets invoked from postStop signal
    supervisorStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))

    // this proves that postStop signal of supervisor gets invoked
    // as supervisor gets unregistered in postStop signal
    akkaProbe.requestNext(10.seconds) shouldBe LocationRemoved(akkaConnection)

    // this proves that on shutdown message, supervisor's actor system gets terminated
    // if it does not get terminated in 5 seconds, future will fail which in turn fail this test
    Await.result(hcdActorSystem.whenTerminated, 5.seconds)

    /*
     * This assertion are here just to prove that LoggingSystem is integrated with framework and ComponentHandlers
     * are able to log messages
     */
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    // DEOPSCSW-180: Generic and Specific Log messages
    assertThatMessageIsLogged(
      logBuffer,
      "IFS_Detector",
      "Invoking lifecycle handler's initialize hook",
      INFO,
      classOf[ComponentBehavior].getName
    )
    // log message from Component handler
    assertThatMessageIsLogged(
      logBuffer,
      "IFS_Detector",
      "Initializing Component TLA",
      INFO,
      classOf[SampleComponentHandlers].getName
    )
  }
}
