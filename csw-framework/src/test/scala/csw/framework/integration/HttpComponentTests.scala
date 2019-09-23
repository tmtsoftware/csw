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
import csw.command.client.models.framework.{
  Components,
  ContainerLifecycleState,
  LifecycleStateChanged,
  PubSub,
  SupervisorLifecycleState
}
import csw.common.FrameworkAssertions._
import csw.common.components.command.CommandComponentState._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.models.ComponentType.{Assembly, HCD}
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models.{ComponentId, ComponentType}
import csw.location.client.ActorSystemFactory
import csw.params.commands.CommandResponse.{Accepted, Completed, Error, Invalid, Started}
import csw.params.commands.{ControlCommand, Setup}
import csw.params.core.generics.KeyType.CoordKey
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.Coords.EqFrame.FK5
import csw.params.core.models.Coords.SolarSystemObject.Venus
import csw.params.core.states.{CurrentState, StateName}
import io.lettuce.core.RedisClient
import csw.params.core.models.{Angle, Coords, ObsId, Prefix, ProperMotion, Struct}

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

  private val basePosKey = CoordKey.make("BasePosition")
  private val key1: Key[Float] =
    KeyType.FloatKey.make("assemblyEventValue1")
  private val key1b: Key[Float] =
    KeyType.FloatKey.make("assemblyEventValue1b")
  private val key2b: Key[Struct] =
    KeyType.StructKey.make("assemblyEventStructValue2b")
  private val key3: Key[Int] =
    KeyType.IntKey.make("assemblyEventStructValue3")
  private val key4: Key[Byte] =
    KeyType.ByteKey.make("assemblyEventStructValue4")

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
    //val filterHCDStateProbe          = TestProbe[CurrentState]("hcd-state-probe")

    val assemblyLifecycleStateProbe = TestProbe[LifecycleStateChanged]("assembly-lifecycle-probe")

    val seqPrefix       = Prefix("wfos.seq")
    val filterAsmPrefix = Prefix("wfos.blue.filter")
    //val filterHcdPrefix = Prefix("wfos.blue.filter.hcd")

    // initially container is put in Idle lifecycle state and wait for all the components to move into Running lifecycle state
    // ********** Message: GetContainerLifecycleState **********
    containerRef ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)
    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Idle)

    assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    // resolve container using location service
    val containerLocation = seedLocationService.resolve(irisContainerConnection, 5.seconds).await
    containerLocation.isDefined shouldBe true
    val resolvedContainerRef = containerLocation.get.containerRef

    // ********** Message: GetComponents **********
    resolvedContainerRef ! GetComponents(componentsProbe.ref)
    val components = componentsProbe.expectMessageType[Components].components
    components.size shouldBe 2

    // resolve all the components from container using location service
    val filterAssemblyLocation     = seedLocationService.find(filterAssemblyConnection).await
    val filterHCDLocation          = seedLocationService.find(filterHCDConnection).await
    val httpFilterAssemblyLocation = seedLocationService.find(httpFilterAssembly).await

    filterAssemblyLocation.isDefined shouldBe true
    filterHCDLocation.isDefined shouldBe true
    httpFilterAssemblyLocation.isDefined shouldBe true

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
    var result = Await.result(httpFilterAssemblyCS.submit(short), timeout.duration)
    result shouldBe a[Completed]
    result.commandName shouldEqual immediateCmd

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

    // Wait for completion with queryFinal
    Await.result(httpFilterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe Completed(longRunningCmd, result.runId)

    // Now query should show Completed
    Await.result(httpFilterAssemblyCS.query(result.runId), timeout.duration) shouldBe Completed(longRunningCmd, result.runId)

    // Make sure queryFinal returns immediately and properly for something already completed
    Await.result(httpFilterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe Completed(longRunningCmd, result.runId)

    // Reuse the long running command, but now just wait, no way to use runId so no query or queryFinal is useful
    Await.result(httpFilterAssemblyCS.submitAndWait(longRunning), timeout.duration) shouldBe a[Completed]

    // Make sure invalid works
    Await.result(httpFilterAssemblyCS.submitAndWait(invalid), timeout.duration) shouldBe a[Invalid]

    // Hierarchy going through two layers of queryFinal
    val longRunningToAsm = Setup(seqPrefix, longRunningCmdToAsm, obsId)

    // This executes a command in Assembly that goes to HCD
    result = Await.result(httpFilterAssemblyCS.submitAndWait(longRunningToAsm), timeout.duration)
    result shouldBe a[Completed]
    result.commandName shouldEqual longRunningToAsm.commandName

    // Long running where command completes after Started, uses Submit so returns right away
    result = Await.result(httpFilterAssemblyCS.submit(longRunningToAsm), timeout.duration)
    result shouldBe a[Started]

    // Now wait with queryFinal
    Await.result(httpFilterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe a[Completed]

    // Can also do the whole thing with oneway if necessary (same in old version)
    val onewayResult = Await.result(httpFilterAssemblyCS.oneway(longRunningToAsm), timeout.duration)
    onewayResult shouldBe a[Accepted]
    // Can queryFinal also
    Await.result(httpFilterAssemblyCS.queryFinal(onewayResult.runId), timeout.duration) shouldBe
    Completed(longRunningToAsm.commandName, onewayResult.runId)

    // Try using completer in Assembly
    val longRunningToAsmComp = Setup(seqPrefix, longRunningCmdToAsmComp, obsId)
    result = Await.result(httpFilterAssemblyCS.submit(longRunningToAsmComp), timeout.duration)
    result shouldBe a[Started]
    result.commandName shouldEqual longRunningToAsmComp.commandName
    Await.result(httpFilterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe Completed(
      longRunningToAsmComp.commandName,
      result.runId
    )

    // Try using completer in Assembly with ActorCompleter
    val longRunningToAsmCompActor = Setup(seqPrefix, longRunningCmdToAsmCActor, obsId)
    result = Await.result(httpFilterAssemblyCS.submit(longRunningToAsmCompActor), timeout.duration)
    result shouldBe a[Started]
    result.commandName shouldEqual longRunningToAsmCompActor.commandName
    Await.result(httpFilterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe
    Completed(longRunningToAsmCompActor.commandName, result.runId)

    // Try using completer in Assembly
    val longRunningToAsmInvalid = Setup(seqPrefix, longRunningCmdToAsmInvalid, obsId)
    result = Await.result(httpFilterAssemblyCS.submit(longRunningToAsmInvalid), timeout.duration)
    result shouldBe a[Started]
    result.commandName shouldEqual longRunningToAsmInvalid.commandName
    Await.result(httpFilterAssemblyCS.queryFinal(result.runId), timeout.duration) shouldBe
    Error(longRunningToAsmInvalid.commandName, result.runId, "ERROR")

    val setupForBigParameters = makeTestCommand()
    result = Await.result(httpFilterAssemblyCS.submit(setupForBigParameters), timeout.duration)
    result shouldBe a[Completed]
    result.asInstanceOf[Completed].result.paramSet shouldBe setupForBigParameters.paramSet

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

  // Creates a test command
  private def makeTestCommand(): ControlCommand = {
    import Angle._
    import Coords._

    val pm = ProperMotion(0.5, 2.33)
    val eqCoord = EqCoord(
      ra = "12:13:14.15",
      dec = "-30:31:32.3",
      frame = FK5,
      pmx = pm.pmx,
      pmy = pm.pmy
    )
    val solarSystemCoord = SolarSystemCoord(Tag("BASE"), Venus)
    val minorPlanetCoord = MinorPlanetCoord(
      Tag("GUIDER1"),
      2000,
      90.degree,
      2.degree,
      100.degree,
      1.4,
      0.234,
      220.degree
    )
    val cometCoord = CometCoord(
      Tag("BASE"),
      2000.0,
      90.degree,
      2.degree,
      100.degree,
      1.4,
      0.234
    )
    val altAzCoord = AltAzCoord(Tag("BASE"), 301.degree, 42.5.degree)
    val posParam = basePosKey.set(
      eqCoord,
      solarSystemCoord,
      minorPlanetCoord,
      cometCoord,
      altAzCoord
    )

    Setup(seqPrefix, cmdWithBigParameter, None)
      .add(posParam)
      .add(key1b.set(1.0f, 2.0f, 3.0f))
      .add(
        key2b.set(
          Struct()
            .add(key1.set(1.0f))
            .add(key3.set(1, 2, 3)),
          Struct()
            .add(key1.set(2.0f))
            .add(key3.set(4, 5, 6))
            .add(key4.set(9.toByte, 10.toByte))
        )
      )
  }
}
