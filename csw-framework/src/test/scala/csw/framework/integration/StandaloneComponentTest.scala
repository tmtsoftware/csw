package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.SupervisorContainerCommonMessages.Shutdown
import csw.command.client.models.framework.SupervisorLifecycleState
import csw.common.FrameworkAssertions._
import csw.common.components.framework.SampleComponentHandlers
import csw.common.components.framework.SampleComponentState._
import csw.common.utils.TestAppender
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.component.ComponentBehavior
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.location.client.ActorSystemFactory
import csw.location.models.ComponentType.HCD
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.logging.api.models.Level.INFO
import csw.logging.client.internal.LoggingSystem
import csw.params.core.states.{CurrentState, StateName}
import io.lettuce.core.RedisClient
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-177: Hooks for lifecycle management
// DEOPSCSW-216: Locate and connect components to send AKKA commands
class StandaloneComponentTest extends FrameworkIntegrationSuite {
  import testWiring._
  // all log messages will be captured in log buffer
  private val logBuffer                    = mutable.Buffer.empty[JsObject]
  private val testAppender                 = new TestAppender(x => logBuffer += Json.parse(x.toString).as[JsObject])
  private var loggingSystem: LoggingSystem = _
  // using standaloneActorSystem to start component instead of seedActorSystem,
  // to assert shutdown of the component(which will also shutdown standaloneActorSystem)
  private val standaloneComponentActorSystem: ActorSystem[SpawnProtocol] =
    ActorSystemFactory.remote(SpawnProtocol.behavior, "test")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    loggingSystem = new LoggingSystem("standalone", "1.0", "localhost", seedActorSystem)
    loggingSystem.setAppenders(List(testAppender))
  }

  override def afterAll(): Unit = {
    standaloneComponentActorSystem.terminate()
    standaloneComponentActorSystem.whenTerminated.await
    super.afterAll()
  }

  test("should start a component in standalone mode and register with location service") {
    // start component in standalone mode
    val wiring: FrameworkWiring = FrameworkWiring.make(standaloneComponentActorSystem, mock[RedisClient])
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

    val supervisorCommandService = CommandServiceFactory.make(resolvedAkkaLocation)

    val (_, akkaProbe) =
      seedLocationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent](seedActorSystem.toUntyped))(Keep.both).run()
    akkaProbe.requestNext() shouldBe a[LocationUpdated]

    // on shutdown, component unregisters from location service
    supervisorCommandService.subscribeCurrentState(supervisorStateProbe.ref ! _)
    Http(standaloneComponentActorSystem.toUntyped).shutdownAllConnectionPools().await
    supervisorRef ! Shutdown

    // this proves that ComponentBehaviors postStop signal gets invoked
    // as onShutdownHook of TLA gets invoked from postStop signal
    supervisorStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))

    // this proves that postStop signal of supervisor gets invoked
    // as supervisor gets unregistered in postStop signal
    akkaProbe.requestNext(10.seconds) shouldBe LocationRemoved(akkaConnection)

    // this proves that on shutdown message, supervisor's actor system gets terminated
    // if it does not get terminated in 5 seconds, future will fail which in turn fail this test
    Await.result(standaloneComponentActorSystem.whenTerminated, 5.seconds)

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
      ComponentBehavior.getClass.getName
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
