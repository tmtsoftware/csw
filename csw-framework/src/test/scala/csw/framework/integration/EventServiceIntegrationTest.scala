package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.{TestInbox, TestProbe}
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.models.framework.ContainerLifecycleState
import csw.common.FrameworkAssertions.assertThatContainerIsRunning
import csw.common.components.framework.SampleComponentState._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.internal.commons.EventServiceConnection
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection.AkkaConnection
import csw.params.commands
import csw.params.commands.CommandName
import csw.params.core.states.{CurrentState, StateName}
import redis.embedded.{RedisSentinel, RedisServer}

import scala.concurrent.duration.DurationLong

//DEOPSCSW-395: Provide EventService handle to component developers
class EventServiceIntegrationTest extends FrameworkIntegrationSuite {
  import testWiring._

  private val masterId: String        = ConfigFactory.load().getString("csw-event.redis.masterId")
  private var sentinel: RedisSentinel = _
  private var server: RedisServer     = _

  private val filterAssemblyConnection = AkkaConnection(ComponentId("Filter", Assembly))
  private val disperserHcdConnection   = AkkaConnection(ComponentId("Disperser", HCD))
  private val wiring                   = FrameworkWiring.make(testActorSystem)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val tuple = startSentinelAndRegisterService(EventServiceConnection.value, masterId)
    sentinel = tuple._2
    server = tuple._3
  }

  override def afterAll(): Unit = {
    Http(testActorSystem).shutdownAllConnectionPools().await
    stopSentinel(sentinel, server)
    super.afterAll()
  }

  test("should be able to publish and subscribe to events") {
    val containerRef = Container.spawn(ConfigFactory.load("container_tracking_connections.conf"), wiring).await

    val assemblyProbe                = TestInbox[CurrentState]()
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    val filterAssemblyLocation = testWiring.seedLocationService.find(filterAssemblyConnection).await
    val disperserHcdLocation   = testWiring.seedLocationService.find(disperserHcdConnection).await

    val assemblyCommandService  = CommandServiceFactory.make(filterAssemblyLocation.get)
    val disperserCommandService = CommandServiceFactory.make(disperserHcdLocation.get)

    assemblyCommandService.subscribeCurrentState(assemblyProbe.ref ! _)

    implicit val timeout: Timeout = Timeout(100.millis)
    assemblyCommandService.submit(commands.Setup(prefix, CommandName("subscribe.event.success"), None))
    Thread.sleep(1000)
    disperserCommandService.submit(commands.Setup(prefix, CommandName("publish.event.success"), None))
    Thread.sleep(1000)

    val states = assemblyProbe.receiveAll().filter(_.paramSet.contains(choiceKey.set(eventReceivedChoice)))

    states.size shouldBe 2 //inclusive of latest event
    states should contain(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(eventReceivedChoice))))
  }
}
