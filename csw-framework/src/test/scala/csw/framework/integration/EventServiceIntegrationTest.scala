package csw.framework.integration

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.{TestInbox, TestProbe}
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.common.FrameworkAssertions
import csw.common.components.framework.SampleComponentState._
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.messages.commands
import csw.messages.commands.CommandName
import csw.messages.framework.ContainerLifecycleState
import csw.messages.location.ComponentId
import csw.messages.location.ComponentType.{Assembly, HCD}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.params.states.{CurrentState, StateName}
import csw.services.command.scaladsl.CommandService
import csw.services.event.internal.redis.RedisTestProps
import csw.services.location.commons.ClusterSettings
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class EventServiceIntegrationTest extends FunSuite with Matchers with BeforeAndAfterAll {
  val seedPort                      = 3557
  val sentinelPort                  = 26379
  val serverPort                    = 6379
  private val props: RedisTestProps = RedisTestProps.createRedisProperties(seedPort, sentinelPort, serverPort)

  implicit val typedSystem: ActorSystem[_] = props.typedActorSystem
  private val systemToJoinCluster          = ClusterSettings().joinLocal(seedPort).system

  implicit val testKitSettings: TestKitSettings = TestKitSettings(typedSystem)
  private val filterAssemblyConnection          = AkkaConnection(ComponentId("Filter", Assembly))

  private val disperserHcdConnection = AkkaConnection(ComponentId("Disperser", HCD))

  override protected def beforeAll(): Unit = {
    props.start()
  }

  override protected def afterAll(): Unit = {
    props.shutdown()
    Await.result(systemToJoinCluster.terminate(), 5.seconds)
  }

  test("should be able to publish and subscribe to events") {
    val wiring: FrameworkWiring = FrameworkWiring.make(systemToJoinCluster)

    val containerRef = Await.result(Container.spawn(ConfigFactory.load("container_tracking_connections.conf"), wiring), 5.seconds)

    val assemblyProbe                = TestInbox[CurrentState]()
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    FrameworkAssertions.assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    val filterAssemblyLocation = Await.result(wiring.locationService.find(filterAssemblyConnection), 5.seconds)
    val disperserHcdLocation   = Await.result(wiring.locationService.find(disperserHcdConnection), 5.seconds)

    val assemblyCommandService  = new CommandService(filterAssemblyLocation.get)
    val disperserCommandService = new CommandService(disperserHcdLocation.get)

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
