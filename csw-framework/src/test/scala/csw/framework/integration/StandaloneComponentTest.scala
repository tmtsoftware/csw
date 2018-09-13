package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import com.persist.JsonOps
import com.persist.JsonOps.JsonObject
import com.typesafe.config.ConfigFactory
import csw.common.FrameworkAssertions._
import csw.common.components.framework.SampleComponentHandlers
import csw.common.components.framework.SampleComponentState._
import csw.common.utils.TestAppender
import csw.commons.tags.LoggingSystemSensitive
import csw.framework.FrameworkTestWiring
import csw.framework.internal.component.ComponentBehavior
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.command.messages.SupervisorContainerCommonMessages.Shutdown
import csw.command.models.framework.SupervisorLifecycleState
import csw.services.location.api.models.ComponentType.HCD
import csw.services.location.api.models.Connection.AkkaConnection
import csw.services.location.api.models.{ComponentId, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.params.states.{CurrentState, StateName}
import csw.command.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.scaladsl.CommandService
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.logging.internal.LoggingLevels.INFO
import csw.services.logging.internal.LoggingSystem
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-177: Hooks for lifecycle management
// DEOPSCSW-216: Locate and connect components to send AKKA commands
@LoggingSystemSensitive
class StandaloneComponentTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {

  private val testWiring = new FrameworkTestWiring()
  import testWiring._

  // all log messages will be captured in log buffer
  private val logBuffer                    = mutable.Buffer.empty[JsonObject]
  private val testAppender                 = new TestAppender(x ⇒ logBuffer += JsonOps.Json(x.toString).asInstanceOf[JsonObject])
  private var loggingSystem: LoggingSystem = _

  override protected def beforeAll(): Unit = {
    loggingSystem = new LoggingSystem("standalone", "1.0", "localhost", seedActorSystem)
    loggingSystem.setAppenders(List(testAppender))
  }

  override protected def afterAll(): Unit = shutdown()

  test("should start a component in standalone mode and register with location service") {

    // start component in standalone mode
    val wiring: FrameworkWiring = FrameworkWiring.make(testActorSystem, mock[RedisClient])
    Standalone.spawn(ConfigFactory.load("standalone.conf"), wiring)

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]("supervisor-lifecycle-state-probe")
    val supervisorStateProbe          = TestProbe[CurrentState]("supervisor-state-probe")
    val akkaConnection                = AkkaConnection(ComponentId("IFS_Detector", HCD))

    // verify component gets registered with location service
    val maybeLocation = seedLocationService.resolve(akkaConnection, 5.seconds).await

    maybeLocation.isDefined shouldBe true
    val resolvedAkkaLocation = maybeLocation.get
    resolvedAkkaLocation.connection shouldBe akkaConnection

    val supervisorRef = resolvedAkkaLocation.componentRef
    assertThatSupervisorIsRunning(supervisorRef, supervisorLifecycleStateProbe, 5.seconds)

    val supervisorCommandService = new CommandService(resolvedAkkaLocation)

    val (_, akkaProbe) = seedLocationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
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
    Await.result(testActorSystem.whenTerminated, 5.seconds)

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
