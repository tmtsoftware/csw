package csw.common.framework.integration

import akka.actor
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.common.components.SampleComponentState._
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.internal.wiring.{Container, FrameworkWiring}
import csw.common.framework.models.ContainerCommonMessage.{GetComponents, GetContainerMode}
import csw.common.framework.models.PubSub.Subscribe
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.common.framework.models.{Components, LifecycleStateChanged, Restart}
import csw.param.states.CurrentState
import csw.services.location.commons.ClusterAwareSettings
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-169: Creation of Multiple Components
class ContainerIntegrationTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private val untypedSystem: actor.ActorSystem  = ClusterAwareSettings.system
  implicit val typedSystem: ActorSystem[_]      = untypedSystem.toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(typedSystem)

  override protected def afterAll(): Unit = Await.result(untypedSystem.terminate(), 5.seconds)

  test("should start multiple components withing a single container and able to accept lifecycle messages") {

    val wiring = FrameworkWiring.make(untypedSystem)
    // start a container and verify it moves to running mode
    val containerRef = Container.spawn(ConfigFactory.load("container.conf"), wiring)

    val componentsProbe    = TestProbe[Components]
    val containerModeProbe = TestProbe[ContainerMode]
    val assemblyProbe      = TestProbe[CurrentState]
    val filterProbe        = TestProbe[CurrentState]
    val disperserProbe     = TestProbe[CurrentState]

    val assemblyLifecycleStateProbe  = TestProbe[LifecycleStateChanged]
    val filterLifecycleStateProbe    = TestProbe[LifecycleStateChanged]
    val disperserLifecycleStateProbe = TestProbe[LifecycleStateChanged]

    // initially container is put in Idle state and wait for all the components to move into Running mode
    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Idle)

    containerRef ! GetComponents(componentsProbe.ref)
    val components = componentsProbe.expectMsgType[Components].components
    components.size shouldBe 3

    val assemblySupervisor  = components(0).supervisor
    val filterSupervisor    = components(1).supervisor
    val disperserSupervisor = components(2).supervisor

    Thread.sleep(500)

    // once all components from container moves to Running mode,
    // container moves to Running mode and ready to accept external lifecycle messages
    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Running)

    assemblySupervisor ! ComponentStateSubscription(Subscribe(assemblyProbe.ref))
    filterSupervisor ! ComponentStateSubscription(Subscribe(filterProbe.ref))
    disperserSupervisor ! ComponentStateSubscription(Subscribe(disperserProbe.ref))

    assemblySupervisor ! LifecycleStateSubscription(Subscribe(assemblyLifecycleStateProbe.ref))
    filterSupervisor ! LifecycleStateSubscription(Subscribe(filterLifecycleStateProbe.ref))
    disperserSupervisor ! LifecycleStateSubscription(Subscribe(disperserLifecycleStateProbe.ref))

    // lifecycle messages gets forwarded to all components and their corresponding handlers gets invoked
    containerRef ! Lifecycle(GoOffline)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))

    containerRef ! Lifecycle(GoOnline)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))

    // on Restart message, container falls back to Idle mode and wait for all the components to get restarted
    // and moves to Running mode
    containerRef ! Restart

    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Idle)

    val initState      = CurrentState(prefix, Set(choiceKey.set(initChoice)))
    val runState       = CurrentState(prefix, Set(choiceKey.set(runChoice)))
    val shutdownState  = CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))
    val expectedStates = Set(initState, runState, shutdownState)

    // on restart, new TLA gets created which means TLA's onInitialize and onRun hooks gets invoked
    // also old TLA gets killed which triggers old TLA's onShutdown handler
    // hence, subscriber receives three events :
    // 1. onInitialized -> CurrentState(InitialChoice) (This is from new TLA)
    // 2. onRun -> CurrentState(RunChoice) (This is from new TLA)
    // 3. onShutdown -> CurrentState(ShutdownChoice) (This is from old TLA)
    // But the order in which onShutdown event will be received by subscribers is not guaranteed,
    // that is the reason why we are collecting all the events and then asserting on all of them together in Set
    val assemblyActualState1 = assemblyProbe.expectMsgType[CurrentState]
    val assemblyActualState2 = assemblyProbe.expectMsgType[CurrentState]
    val assemblyActualState3 = assemblyProbe.expectMsgType[CurrentState]
    val assemblyActualStates = Set(assemblyActualState1, assemblyActualState2, assemblyActualState3)

    val filterActualState1 = filterProbe.expectMsgType[CurrentState]
    val filterActualState2 = filterProbe.expectMsgType[CurrentState]
    val filterActualState3 = filterProbe.expectMsgType[CurrentState]
    val filterActualStates = Set(filterActualState1, filterActualState2, filterActualState3)

    val disperserActualState1 = disperserProbe.expectMsgType[CurrentState]
    val disperserActualState2 = disperserProbe.expectMsgType[CurrentState]
    val disperserActualState3 = disperserProbe.expectMsgType[CurrentState]
    val disperserActualStates = Set(disperserActualState1, disperserActualState2, disperserActualState3)

    assemblyActualStates shouldBe expectedStates
    filterActualStates shouldBe expectedStates
    disperserActualStates shouldBe expectedStates

    assemblyLifecycleStateProbe.expectMsg(LifecycleStateChanged(assemblySupervisor, SupervisorMode.Running))
    filterLifecycleStateProbe.expectMsg(LifecycleStateChanged(filterSupervisor, SupervisorMode.Running))
    disperserLifecycleStateProbe.expectMsg(LifecycleStateChanged(disperserSupervisor, SupervisorMode.Running))

    Thread.sleep(100)
    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Running)
  }

}
