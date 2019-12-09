package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.DiagnosticDataMessage.{DiagnosticMode, OperationsMode}
import csw.command.client.models.framework.SupervisorLifecycleState
import csw.common.FrameworkAssertions.assertThatSupervisorIsRunning
import csw.common.components.framework.SampleComponentState
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.internal.commons.EventServiceConnection
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.location.models.ComponentId
import csw.location.models.ComponentType.HCD
import csw.location.models.Connection.AkkaConnection
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.prefix.models.Subsystem
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import redis.embedded.{RedisSentinel, RedisServer}

import scala.concurrent.duration.DurationLong

// DEOPSCSW-37: Add diagnosticMode handler to component handlers
class DiagnosticDataIntegrationTest extends FrameworkIntegrationSuite {
  import testWiring._

  private val masterId: String        = ConfigFactory.load().getString("csw-event.redis.masterId")
  private var sentinel: RedisSentinel = _
  private var server: RedisServer     = _

  private val wiring: FrameworkWiring = FrameworkWiring.make(seedActorSystem)

  override def beforeAll(): Unit = {
    super.beforeAll()
    val tuple = startSentinelAndRegisterService(EventServiceConnection.value, masterId)
    sentinel = tuple._2
    server = tuple._3
  }

  override def afterAll(): Unit = {
    stopSentinel(sentinel, server)
    super.afterAll()
  }

  test("component should be able to handle diagnostic data request") {
    import SampleComponentState._
    import wiring._
    LoggingSystemFactory.start("", "", "", actorSystem)
    Standalone.spawn(ConfigFactory.load("standalone.conf"), wiring)

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]("supervisor-lifecycle-state-probe")
    val akkaConnection                = AkkaConnection(ComponentId(Prefix(Subsystem.IRIS, "IFS_Detector"), HCD))
    val location                      = locationService.resolve(akkaConnection, 5.seconds).await

    LoggingSystemFactory.start("", "", "", actorSystem)

    val supervisorRef = location.get.componentRef
    assertThatSupervisorIsRunning(supervisorRef, supervisorLifecycleStateProbe, 5.seconds)

    val eventService  = eventServiceFactory.make(locationService)
    val eventProbe    = TestProbe[Event]
    val diagnosticKey = EventKey(prefix, diagnosticDataEventName)
    val subscription  = eventService.defaultSubscriber.subscribeActorRef(Set(diagnosticKey), eventProbe.ref)
    subscription.ready().await

    supervisorRef ! DiagnosticMode(UTCTime.now(), "engineering")

    eventProbe.expectMessageType[SystemEvent] //discard invalid event
    val diagnosticModeEvent = eventProbe.expectMessageType[SystemEvent]
    diagnosticModeEvent.eventKey shouldBe diagnosticKey
    diagnosticModeEvent.paramSet.head shouldBe diagnosticModeParam

    supervisorRef ! DiagnosticMode(UTCTime.after(2.second), "engineering")
    eventProbe.expectNoMessage(1.second)
    eventProbe.expectMessageType[SystemEvent].eventKey shouldEqual diagnosticKey

    // unsupportedHint is ignored by the component, it continues to publish in previous diagMode
    supervisorRef ! DiagnosticMode(UTCTime.now(), "unsupportedHint")
    eventProbe.expectMessageType[SystemEvent].eventKey shouldEqual diagnosticKey

    supervisorRef ! OperationsMode
    eventProbe.expectNoMessage()

  }
}
