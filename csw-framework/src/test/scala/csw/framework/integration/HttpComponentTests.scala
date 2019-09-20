package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentCommonMessage.{GetSupervisorLifecycleState, LifecycleStateSubscription}
import csw.command.client.messages.ContainerCommonMessage.{GetComponents, GetContainerLifecycleState}
import csw.command.client.messages.SupervisorContainerCommonMessages.Shutdown
import csw.command.client.models.framework.{Components, ContainerLifecycleState, LifecycleStateChanged, PubSub, SupervisorLifecycleState}
import csw.common.FrameworkAssertions._
import csw.common.components.command.CommandComponentState.{seqPrefix, _}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.models.ComponentType.{Assembly, HCD}
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models.{ComponentId, ComponentType}
import csw.location.client.ActorSystemFactory
import csw.params.commands.CommandResponse.{Accepted, Completed, Error, Invalid, Started}
import csw.params.commands.{CommandName, Setup}
import csw.params.core.states.{CurrentState, StateName}
import io.lettuce.core.RedisClient
import csw.params.core.models.{ObsId, Prefix}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext}

class HttpComponentTests extends FrameworkIntegrationSuite {
  import testWiring._

  private val irisContainerConnection  = AkkaConnection(ComponentId("WFOS_Container", ComponentType.Container))
  private val filterAssemblyConnection = AkkaConnection(ComponentId("FilterASS", Assembly))
  private val filterHCDConnection      = AkkaConnection(ComponentId("FilterHCD", HCD))
  private val httpFilterAssembly       = HttpConnection(ComponentId("FilterASS", Assembly))

  private val containerActorSystem: ActorSystem[SpawnProtocol] =
    ActorSystemFactory.remote(SpawnProtocol.behavior, "container-system")
  val obsId                         = Some(ObsId("Obs001"))
  implicit val timeout: Timeout     = 12.seconds
  implicit val ec: ExecutionContext = containerActorSystem.executionContext

  override def afterAll(): Unit = {
    containerActorSystem.terminate()
    containerActorSystem.whenTerminated.await
    super.afterAll()
  }

  test("should start multiple components within a single container and exercise features of Command Service") {

    val wiring = FrameworkWiring.make(containerActorSystem, mock[RedisClient])
    // start a container and verify it moves to running lifecycle state
    val containerRef =
      Container.spawn(ConfigFactory.load("command_container.conf"), wiring).await

    val componentsProbe              = TestProbe[Components]("comp-probe")
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    val filterAssemblyStateProbe     = TestProbe[CurrentState]("assembly-state-probe")
    val filterHCDStateProbe          = TestProbe[CurrentState]("hcd-state-probe")
    println("1")
    val assemblyLifecycleStateProbe = TestProbe[LifecycleStateChanged]("assembly-lifecycle-probe")

    val seqPrefix       = Prefix("wfos.seq")
    val filterAsmPrefix = Prefix("wfos.blue.filter")
    //val filterHcdPrefix = Prefix("wfos.blue.filter.hcd")
    println("2")
    val immediateCmd = CommandName("move.immediate")

    // initially container is put in Idle lifecycle state and wait for all the components to move into Running lifecycle state
    // ********** Message: GetContainerLifecycleState **********
    containerRef ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Idle)
    println("3")
    assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)
    println("4")
    // resolve container using location service
    val containerLocation = seedLocationService.resolve(irisContainerConnection, 5.seconds).await

    containerLocation.isDefined shouldBe true
    val resolvedContainerRef = containerLocation.get.containerRef

    // ********** Message: GetComponents **********
    resolvedContainerRef ! GetComponents(componentsProbe.ref)
    val components = componentsProbe.expectMessageType[Components].components
    components.size shouldBe 2

    // resolve all the components from container using location service
    val filterAssemblyLocation = seedLocationService.find(filterAssemblyConnection).await
//    val filterHCDLocation      = seedLocationService.find(filterHCDConnection).await

    val httpFilterAssemblyLocation = seedLocationService.find(httpFilterAssembly).await

    filterAssemblyLocation.isDefined shouldBe true
  //  filterHCDLocation.isDefined shouldBe true
    httpFilterAssemblyLocation.isDefined shouldBe true
//println("Yes Yes: " + httpFilterAssemblyLocation.get)
    val filterAssemblyCS = CommandServiceFactory.make(filterAssemblyLocation.get)
    //val filterHcdCS      = CommandServiceFactory.make(filterHCDLocation.get)

    val httpFilterAssemblyCS = CommandServiceFactory.make(httpFilterAssemblyLocation.get)

    // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
    // Subscribe to component's current state
    filterAssemblyCS.subscribeCurrentState(filterAssemblyStateProbe.ref ! _)
    //filterHcdCS.subscribeCurrentState(filterHCDStateProbe.ref ! _)

    // Subscribe to component's lifecycle state
    filterAssemblyLocation.foreach(
      l => l.componentRef ! LifecycleStateSubscription(PubSub.Subscribe(assemblyLifecycleStateProbe.ref))
    )

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]
    filterAssemblyLocation.foreach(l => l.componentRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))

    // make sure that all the components are in running lifecycle state before sending lifecycle messages
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)

    // Send short command to make sure it works
    val short  = Setup(seqPrefix, immediateCmd, obsId)
    var result = Await.result(filterAssemblyCS.submit(short), timeout.duration)
    result shouldBe a[Completed]
    result.commandName shouldEqual immediateCmd

    println("Now Http")
    result = Await.result(httpFilterAssemblyCS.submit(short), timeout.duration)
    result shouldBe a[Completed]
    result.commandName shouldEqual immediateCmd
    println("Result is: " + result)

    val validateResponse = Await.result(httpFilterAssemblyCS.validate(short), timeout.duration)
    validateResponse shouldBe a[Accepted]

    // Make sure errors are handled in validation
    val invalid = Setup(seqPrefix, invalidCmd, obsId)
    result = Await.result(httpFilterAssemblyCS.submit(invalid), timeout.duration)
    result shouldBe a[Invalid]
    result.commandName shouldEqual invalidCmd

    // Long running where command completes after Started, uses Submit so returns right away
    val longRunning = Setup(seqPrefix, longRunningCmd, obsId)
    result = Await.result(httpFilterAssemblyCS.submit(longRunning), timeout.duration)
    result shouldBe a[Started]

    // Check with query should be Started
    val qresult = Await.result(httpFilterAssemblyCS.query(result.runId), timeout.duration)
    qresult shouldBe Started(longRunningCmd, result.runId)

    println("Sleeping")
    Thread.sleep(2000)
    println("Done")

    // ********** Message: Shutdown **********
    Http(containerActorSystem.toUntyped).shutdownAllConnectionPools().await
    resolvedContainerRef ! Shutdown

    // this proves that ComponentBehaviors postStop signal gets invoked for all components
    // as onShutdownHook of all TLA gets invoked from postStop signal
    filterAssemblyStateProbe.expectMessage(
      CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice)))
    )

    /*
    filterHCDStateProbe.expectMessage(
      CurrentState(filterHcdPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice)))
    )
*/
    // this proves that on shutdown message, container's actor system gets terminated
    // if it does not get terminated in 5 seconds, future will fail which in turn fail this test
    containerActorSystem.whenTerminated.await
  }

}
