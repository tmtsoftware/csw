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
import csw.common.framework.internal.wiring.Container
import csw.common.framework.models.ContainerCommonMessage.GetContainerMode
import csw.common.framework.models.ContainerExternalMessage.GetComponents
import csw.common.framework.models.PubSub.Subscribe
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.models.{Components, LifecycleStateChanged}
import csw.param.states.CurrentState
import org.scalatest.{FunSuite, Matchers}

class ContainerIntegrationTest extends FunSuite with Matchers {

  implicit val system: ActorSystem[_]           = actor.ActorSystem("system").toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(system)

  test("should start multiple components withing a single container and able to accept lifecycle messages") {

    // start a container and verify it moves to running mode
    val containerRef = Container.spawn(ConfigFactory.load("container.conf"))

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
    Thread.sleep(500)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))

    containerRef ! Lifecycle(GoOnline)
    Thread.sleep(500)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))

    // on Restart message, container falls back to Idle mode and wait for all the components to get restarted
    // and moves to Running mode
    containerRef ! Lifecycle(Restart)

    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Idle)

    Thread.sleep(500)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(restartChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(restartChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(restartChoice))))

    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(initChoice))))

    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(runChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(runChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(runChoice))))

    assemblyLifecycleStateProbe.expectMsg(LifecycleStateChanged(assemblySupervisor, SupervisorMode.Running))
    filterLifecycleStateProbe.expectMsg(LifecycleStateChanged(filterSupervisor, SupervisorMode.Running))
    disperserLifecycleStateProbe.expectMsg(LifecycleStateChanged(disperserSupervisor, SupervisorMode.Running))

    Thread.sleep(100)
    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerMode.Running)
  }

}
