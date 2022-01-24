package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.{TestInbox, TestProbe}
import akka.stream.scaladsl.{Keep, Sink}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.models.framework.ContainerLifecycleState
import csw.common.FrameworkAssertions.assertThatContainerIsRunning
import csw.common.components.framework.SampleComponentState._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.internal.commons.EventServiceConnection
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.api
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection.AkkaConnection
import csw.params.commands
import csw.params.commands.CommandName
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{Event, ObserveEvent}
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.concurrent.Eventually
import redis.embedded.{RedisSentinel, RedisServer}

import scala.collection.mutable
import scala.concurrent.duration.DurationLong

//DEOPSCSW-395: Provide EventService handle to component developers
//CSW-82: ComponentInfo should take prefix
class EventServiceIntegrationTest extends FrameworkIntegrationSuite with Eventually {
  import testWiring._

  private val masterId: String        = ConfigFactory.load().getString("csw-event.redis.masterId")
  private var sentinel: RedisSentinel = _
  private var server: RedisServer     = _

  private val filterAssemblyConnection = AkkaConnection(api.models.ComponentId(Prefix(Subsystem.TCS, "Filter"), Assembly))
  private val disperserHcdConnection   = AkkaConnection(api.models.ComponentId(Prefix(Subsystem.TCS, "Disperser"), HCD))
  private val wiring                   = FrameworkWiring.make(seedActorSystem)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val tuple = startSentinelAndRegisterService(EventServiceConnection.value, masterId)
    sentinel = tuple._2
    server = tuple._3
  }

  override def afterAll(): Unit = {
    stopSentinel(sentinel, server)
    super.afterAll()
  }

  test("should be able to publish and subscribe to events | DEOPSCSW-395") {
    val containerRef = Container.spawn(ConfigFactory.load("container_tracking_connections.conf"), wiring).await

    val assemblyProbe                = TestInbox[CurrentState]()
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    val filterAssemblyLocation = wiring.locationService.find(filterAssemblyConnection).await
    val disperserHcdLocation   = wiring.locationService.find(disperserHcdConnection).await

    val assemblyCommandService  = CommandServiceFactory.make(filterAssemblyLocation.get)(wiring.actorSystem)
    val disperserCommandService = CommandServiceFactory.make(disperserHcdLocation.get)(wiring.actorSystem)

    assemblyCommandService.subscribeCurrentState(assemblyProbe.ref ! _)

    implicit val timeout: Timeout = Timeout(100.millis)
    assemblyCommandService.submitAndWait(commands.Setup(prefix, CommandName("subscribe.event.success"), None))
    Thread.sleep(1000)
    disperserCommandService.submitAndWait(commands.Setup(prefix, CommandName("publish.event.success"), None))
    Thread.sleep(1000)

    val states = assemblyProbe.receiveAll().filter(_.paramSet.contains(choiceKey.set(eventReceivedChoice)))

    states.size shouldBe 2 // inclusive of latest event
    states should contain(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(eventReceivedChoice))))
  }

  test(
    "should be able to publish and subscribe to observe events coming from IR, Optical & WFS detector (Both java and scala) | CSW-118, CSW-119"
  ) {
    Container.spawn(ConfigFactory.load("observe_event_container.conf"), wiring).await

    val eventService = wiring.eventServiceFactory.make(wiring.locationService)(wiring.actorSystem)
    val subscriber   = eventService.defaultSubscriber

    val subscriptionEventList = mutable.ListBuffer[Event]()
    val value1 = subscriber
      .subscribeObserveEvents()
      .wireTap(e => subscriptionEventList.append(e))
      .toMat(Sink.ignore)(Keep.left)
      .run()
    value1.ready().await
    // events count should be equal to published events count
    eventually(timeout = timeout(5.seconds), interval = interval(100.millis)) {
      val events = subscriptionEventList.toList.filter {
        case _: ObserveEvent => true
        case _               => false
      }

      events.size shouldBe 6
      events.count(_.eventName.name === "ObserveEvent.ObserveStart") shouldBe 2
      events.count(_.eventName.name === "ObserveEvent.ExposureStart") shouldBe 2
      events.count(_.eventName.name === "ObserveEvent.PublishSuccess") shouldBe 2
    }
    value1.unsubscribe().await
  }
}
