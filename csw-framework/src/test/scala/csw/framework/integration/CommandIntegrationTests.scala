package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentCommonMessage.{GetSupervisorLifecycleState, LifecycleStateSubscription}
import csw.command.client.messages.ContainerCommonMessage.{GetComponents, GetContainerLifecycleState}
import csw.command.client.messages.SupervisorContainerCommonMessages.Shutdown
import csw.command.client.models.framework._
import csw.common.FrameworkAssertions._
import csw.common.components.command.CommandComponentState._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.api.models
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.client.ActorSystemFactory
import csw.params.commands.CommandResponse._
import csw.params.commands.Setup
import csw.params.core.generics.KeyType
import csw.params.core.models.{ObsId, Units}
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.{Prefix, Subsystem}
import io.lettuce.core.RedisClient

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext}

// CSW-82: ComponentInfo should take prefix
class CommandIntegrationTests extends FrameworkIntegrationSuite {

  import testWiring.seedLocationService

  private val wfosContainerConnection = AkkaConnection(
    ComponentId(Prefix(Subsystem.Container, "WFOS_Container"), ComponentType.Container)
  )
  private val filterAssemblyConnection = AkkaConnection(models.ComponentId(Prefix(Subsystem.WFOS, "FilterASS"), Assembly))
  private val filterHCDConnection      = AkkaConnection(models.ComponentId(Prefix(Subsystem.WFOS, "FilterHCD"), HCD))
  private implicit val containerActorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "container-system")
  val obsId                         = Some(ObsId("Obs001"))
  implicit val timeout: Timeout     = 12.seconds
  implicit val ec: ExecutionContext = containerActorSystem.executionContext

  override def afterAll(): Unit = {
    containerActorSystem.terminate()
    containerActorSystem.whenTerminated.await
    super.afterAll()
  }

  test("should start multiple components within a single container and exercise features of Command Service | DEOPSCSW-372") {

    val wiring = FrameworkWiring.make(containerActorSystem, mock[RedisClient])
    // start a container and verify it moves to running lifecycle state
    val containerRef =
      Container.spawn(ConfigFactory.load("command_container.conf"), wiring).await

    val componentsProbe              = TestProbe[Components]("comp-probe")
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    val filterAssemblyStateProbe     = TestProbe[CurrentState]("assembly-state-probe")
    val filterHCDStateProbe          = TestProbe[CurrentState]("hcd-state-probe")

    val assemblyLifecycleStateProbe = TestProbe[LifecycleStateChanged]("assembly-lifecycle-probe")

    // initially container is put in Idle lifecycle state and wait for all the components to move into Running lifecycle state
    // ********** Message: GetContainerLifecycleState **********
    containerRef ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Idle)

    assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    // resolve container using location service
    val containerLocation = seedLocationService.resolve(wfosContainerConnection, 5.seconds).await

    containerLocation.isDefined shouldBe true
    val resolvedContainerRef = containerLocation.get.containerRef

    // ********** Message: GetComponents **********
    resolvedContainerRef ! GetComponents(componentsProbe.ref)
    val components = componentsProbe.expectMessageType[Components].components
    components.size shouldBe 2

    // resolve all the components from container using location service
    val filterAssemblyLocation = seedLocationService.find(filterAssemblyConnection).await
    val filterHCDLocation      = seedLocationService.find(filterHCDConnection).await

    filterAssemblyLocation.isDefined shouldBe true
    filterHCDLocation.isDefined shouldBe true

    val filterAssemblyCS = CommandServiceFactory.make(filterAssemblyLocation.get)
    val filterHcdCS      = CommandServiceFactory.make(filterHCDLocation.get)

    // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
    // Subscribe to component's current state
    filterAssemblyCS.subscribeCurrentState(filterAssemblyStateProbe.ref ! _)
    filterHcdCS.subscribeCurrentState(filterHCDStateProbe.ref ! _)

    // Subscribe to component's lifecycle state
    filterAssemblyLocation.foreach(l =>
      l.componentRef ! LifecycleStateSubscription(PubSub.Subscribe(assemblyLifecycleStateProbe.ref))
    )

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]
    filterAssemblyLocation.foreach(l => l.componentRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref))

    // make sure that all the components are in running lifecycle state before sending lifecycle messages
    supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Running)

    // Make sure errors are handled in validation
    val invalid = Setup(seqPrefix, invalidCmd, obsId)
    var result  = Await.result(filterAssemblyCS.submit(invalid), timeout.duration)
    result shouldBe a[Invalid]

    // Send short command to make sure basic functionality works
    val short = Setup(seqPrefix, immediateCmd, obsId)
    result = Await.result(filterAssemblyCS.submit(short), timeout.duration)
    result shouldBe a[Completed]

    // Long running where command in Assembly completes after Started, uses Submit so returns right away
    val longRunning = Setup(seqPrefix, longRunningCmd, obsId)
    result = Await.result(filterAssemblyCS.submit(longRunning), timeout.duration)
    result shouldBe a[Started]

    // Check with query should be Started if quick
    val qresult = Await.result(filterAssemblyCS.query(result.runId), timeout.duration)
    qresult shouldBe Started(result.runId)

    // Wait for completion with queryFinal
    Await.result(filterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe Completed(result.runId)

    // Now query should show Completed
    Await.result(filterAssemblyCS.query(result.runId), timeout.duration) shouldBe Completed(result.runId)

    // Make sure queryFinal returns immediately and properly for something already completed
    Await.result(filterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe Completed(result.runId)

    // Reuse the long running command, but now just wait, no way to use runId so no query or queryFinal is useful
    Await.result(filterAssemblyCS.submitAndWait(longRunning), timeout.duration) shouldBe a[Completed]

    // Make sure invalid works
    Await.result(filterAssemblyCS.submitAndWait(invalid), timeout.duration) shouldBe a[Invalid]

    // Hierarchy going through two layers of queryFinal
    val longRunningToAsm = Setup(seqPrefix, longRunningCmdToAsm, obsId)

    // This executes a command in Assembly that goes to HCD
    result = Await.result(filterAssemblyCS.submitAndWait(longRunningToAsm), timeout.duration)
    result shouldBe a[Completed]

    // Long running where command completes after Started, uses Submit so returns right away
    result = Await.result(filterAssemblyCS.submit(longRunningToAsm), timeout.duration)
    result shouldBe a[Started]

    // Now wait with queryFinal
    Await.result(filterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe a[Completed]

    // Can also do the whole thing with oneway if necessary (same in old version)
    val onewayResult = Await.result(filterAssemblyCS.oneway(longRunningToAsm), timeout.duration)
    onewayResult shouldBe a[Accepted]

    // Can queryFinal also
    Await.result(filterAssemblyCS.queryFinal(onewayResult.runId), timeout.duration) shouldBe Completed(onewayResult.runId)

    // Try using completer like behavior in Assembly
    val longRunningToAsmComp = Setup(seqPrefix, longRunningCmdToAsmComp, obsId)
    result = Await.result(filterAssemblyCS.submit(longRunningToAsmComp), timeout.duration)
    result shouldBe a[Started]

    Await.result(filterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe Completed(result.runId)

    // Long running that returns an error from HCD
    val longRunningToAsmInvalid = Setup(seqPrefix, longRunningCmdToAsmInvalid, obsId)
    result = Await.result(filterAssemblyCS.submit(longRunningToAsmInvalid), timeout.duration)
    result shouldBe a[Started]
    Await.result(filterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe Error(result.runId, "ERROR")

    val submitAllSetup1       = Setup(seqPrefix, immediateCmd, obsId)
    val submitAllSetup2       = Setup(seqPrefix, longRunningCmd, obsId)
    val submitAllinvalidSetup = Setup(seqPrefix, invalidCmd, obsId)

    //#submitAll
    val submitAllF        = filterAssemblyCS.submitAllAndWait(List(submitAllSetup1, submitAllSetup2, submitAllinvalidSetup))
    val submitAllResponse = Await.result(submitAllF, timeout.duration)
    submitAllResponse.length shouldBe 3
    submitAllResponse(0) shouldBe a[Completed]
    submitAllResponse(1) shouldBe a[Completed]
    submitAllResponse(2) shouldBe a[Invalid]
    //#submitAll

    // Make sure we can return a result
    val k1 = KeyType.IntKey.make("encoder")
    val k2 = KeyType.StringKey.make("stringThing")
    val rsetup = Setup(seqPrefix, cmdWithBigParameter, obsId)
      .madd(k1.set(545).withUnits(Units.millimeter), k2.set("This", "is", "good"))
    result = Await.result(filterAssemblyCS.submitAndWait(rsetup), timeout.duration)
    result shouldBe a[Completed]
    val completedResult = result.asInstanceOf[Completed]
    completedResult.result.nonEmpty shouldBe true
    completedResult.result.paramSet shouldEqual rsetup.paramSet

    // ********** Message: Shutdown **********
    Http().shutdownAllConnectionPools().await
    resolvedContainerRef ! Shutdown

    // this proves that ComponentBehaviors postStop signal gets invoked for all components
    // as onShutdownHook of all TLA gets invoked from postStop signal

    filterAssemblyStateProbe.expectMessage(
      CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice)))
    )
    filterHCDStateProbe.expectMessage(
      CurrentState(filterHcdPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice)))
    )

    // this proves that on shutdown message, container's actor system gets terminated
    // if it does not get terminated in 5 seconds, future will fail which in turn fail this test
    containerActorSystem.whenTerminated.await
  }
}
