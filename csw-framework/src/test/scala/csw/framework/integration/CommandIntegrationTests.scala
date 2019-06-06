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
import csw.command.client.models.framework.PubSub.Subscribe
import csw.command.client.models.framework.{Components, ContainerLifecycleState, LifecycleStateChanged, SupervisorLifecycleState}
import csw.common.components.command.CommandComponentState._
import csw.common.FrameworkAssertions._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.client.ActorSystemFactory
import csw.params.commands.CommandResponse.{Completed, Invalid, Started}
import csw.params.commands.Setup
import csw.params.core.states.{CurrentState, StateName}
import io.lettuce.core.RedisClient
import csw.params.core.models.ObsId

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext}

class CommandIntegrationTests extends FrameworkIntegrationSuite {
  import testWiring._

  private val irisContainerConnection  = AkkaConnection(ComponentId("WFOS_Container", ComponentType.Container))
  private val filterAssemblyConnection = AkkaConnection(ComponentId("Filter", Assembly))
  private val filterHCDConnection      = AkkaConnection(ComponentId("FilterHCD", HCD))
  private val containerActorSystem: ActorSystem[SpawnProtocol] =
    ActorSystemFactory.remote(SpawnProtocol.behavior, "container-system")
  val obsId                         = Some(ObsId("Obs001"))
  implicit val timeout: Timeout     = 15.seconds
  implicit val ec: ExecutionContext = containerActorSystem.executionContext

  override def afterAll(): Unit = {
    containerActorSystem.terminate()
    containerActorSystem.whenTerminated.await
    super.afterAll()
  }

  test("should start multiple components within a single container and able to accept lifecycle messages") {

    val wiring = FrameworkWiring.make(containerActorSystem, mock[RedisClient])
    // start a container and verify it moves to running lifecycle state
    val containerRef =
      Container.spawn(ConfigFactory.load("command_container.conf"), wiring).await

    val componentsProbe              = TestProbe[Components]("comp-probe")
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    val filterAssemblyStateProbe     = TestProbe[CurrentState]("assembly-state-probe")

    val assemblyLifecycleStateProbe = TestProbe[LifecycleStateChanged]("assembly-lifecycle-probe")

    // initially container is put in Idle lifecycle state and wait for all the components to move into Running lifecycle state
    // ********** Message: GetContainerLifecycleState **********
    //containerRef ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)
    //containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Idle)

    //assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    // resolve container using location service
    val containerLocation = seedLocationService.resolve(irisContainerConnection, 5.seconds).await

    containerLocation.isDefined shouldBe true
    val resolvedContainerRef = containerLocation.get.containerRef

    // ********** Message: GetComponents **********
    //   resolvedContainerRef ! GetComponents(componentsProbe.ref)
//    val components = componentsProbe.expectMessageType[Components].components
//    components.size shouldBe 2
//    println("Components:: " + components)

    // resolve all the components from container using location service
    val filterAssemblyLocation = seedLocationService.find(filterAssemblyConnection).await
    val filterHCDLocation      = seedLocationService.find(filterHCDConnection).await

    filterAssemblyLocation.isDefined shouldBe true
    filterHCDLocation.isDefined shouldBe true

    val assemblySupervisor = filterAssemblyLocation.get.componentRef
    val filterAssemblyCS   = CommandServiceFactory.make(filterAssemblyLocation.get)
    val filterHcdCS        = CommandServiceFactory.make(filterHCDLocation.get)

    // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
    // Subscribe to component's current state
    //filterAssemblyCS.subscribeCurrentState(filterAssemblyStateProbe.ref ! _)

    // Subscribe to component's lifecycle state
    //assemblySupervisor ! LifecycleStateSubscription(Subscribe(assemblyLifecycleStateProbe.ref))

    //val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]
    //assemblySupervisor ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)

    // make sure that all the components are in running lifecycle state before sending lifecycle messages
    //supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)

    // Send short command to make sure it works
    /*
    val short = Setup(sourcePrefix, immediateCmd, obsId)
    var result = Await.result(filterAssemblyCS.submit(short), timeout.duration)
    result shouldBe a[Completed]
    result.commandName shouldEqual immediateCmd
     */
    /*
    // Make sure errors are handled in validation
    val invalid = Setup(sourcePrefix, invalidCmd, obsId)
    result = Await.result(filterAssemblyCS.submit(invalid), timeout.duration)
    result shouldBe a[Invalid]
    result.commandName shouldEqual invalidCmd
     */
    // Long running where command completes after Started, uses Submit so returns right away
    // val longRunning = Setup(sourcePrefix, longRunningCmd, obsId)
    //var result = Await.result(filterAssemblyCS.submit(longRunning), timeout.duration)
    //result shouldBe a[Started]

    // Check with query should be Started
    //val qresult = Await.result(filterAssemblyCS.query(result.runId), timeout.duration)
    //qresult shouldBe Started(longRunningCmd, result.runId)

    // Check with query2
    //val qresult2 = Await.result(filterAssemblyCS.query2(result.runId), timeout.duration)
    //qresult2 shouldBe Some(Started(longRunningCmd, result.runId))

    // Wait for completion with queryFinal
    //Await.result(filterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe Completed(longRunningCmd, result.runId)
    /*
    // Now query should show Completed
    Await.result(filterAssemblyCS.query(result.runId), timeout.duration) shouldBe Completed(longRunningCmd, result.runId)

    // Make sure queryFinal returns immediately and properly for something already completed
    Await.result(filterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe Completed(longRunningCmd, result.runId)

    // Reuse the long running command, but now just wait, no way to use runId so no query or queryFinal is useful
    Await.result(filterAssemblyCS.submitAndWait(longRunning), timeout.duration) shouldBe a[Completed]

    // Make sure invalid works
    Await.result(filterAssemblyCS.submitAndWait(invalid), timeout.duration) shouldBe a[Invalid]
     */

    val longRunningToHcd = Setup(sourcePrefix, longRunningCmdToHcd, obsId)
    /*
    var result2 = Await.result(filterHcdCS.submitAndWait(longRunningToHcd), timeout.duration)
    result2 shouldBe a[Completed]
    println("Got this far HCD")
     */
    // Check through HCD
    // Long running where command completes after Started, uses Submit so returns right away
    //  val longRunningToHcd = Setup(sourcePrefix, longRunningCmdToHcd, obsId)

    var result2 = Await.result(filterAssemblyCS.submitAndWait(longRunningToHcd), timeout.duration)
    result2 shouldBe a[Started]
    println("Got this far locally")
    //Await.result(filterAssemblyCS.queryFinal(result2.runId), timeout.duration) shouldBe a[Completed]

    println("Did it")
    // ********** Message: Shutdown **********
    Http(containerActorSystem.toUntyped).shutdownAllConnectionPools().await
    resolvedContainerRef ! Shutdown

    // this proves that ComponentBehaviors postStop signal gets invoked for all components
    // as onShutdownHook of all TLA gets invoked from postStop signal
    //filterAssemblyStateProbe.expectMessage(CurrentState(filterPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    //filterHCDProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))

    // this proves that on shutdown message, container's actor system gets terminated
    // if it does not get terminated in 5 seconds, future will fail which in turn fail this test
    containerActorSystem.whenTerminated.await
  }
}
