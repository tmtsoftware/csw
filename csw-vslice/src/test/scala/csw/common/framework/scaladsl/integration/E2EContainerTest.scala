package csw.common.framework.scaladsl.integration

import akka.actor
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.common.components.SampleComponentState._
import csw.common.framework.internal.{ContainerMode, SupervisorMode}
import csw.common.framework.models.ContainerCommonMsg.{GetComponents, GetContainerMode}
import csw.common.framework.models.ModeMsg.ContainerModeMsg
import csw.common.framework.models.SupervisorCommonMsg.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.common.framework.models.{Components, LifecycleStateChanged}
import csw.common.framework.models.PubSub.Subscribe
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.scaladsl.Component
import csw.param.states.CurrentState
import org.scalatest.{FunSuite, Matchers}
class E2EContainerTest extends FunSuite with Matchers {
  implicit val system: ActorSystem[_]           = actor.ActorSystem("system").toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(system)

  test("should start multiple components withing a single container") {
    val containerRef = Component.createContainer(ConfigFactory.load("container.conf"))

    val componentsProbe    = TestProbe[Components]
    val containerModeProbe = TestProbe[ContainerModeMsg]
    val assemblyProbe      = TestProbe[CurrentState]
    val filterProbe        = TestProbe[CurrentState]
    val disperserProbe     = TestProbe[CurrentState]

    val assemblyLifecycleStateProbe  = TestProbe[LifecycleStateChanged]
    val filterLifecycleStateProbe    = TestProbe[LifecycleStateChanged]
    val disperserLifecycleStateProbe = TestProbe[LifecycleStateChanged]

    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerModeMsg(ContainerMode.Idle))

    containerRef ! GetComponents(componentsProbe.ref)
    val components = componentsProbe.expectMsgType[Components].components
    components.size shouldBe 3

    val assemblySupervisor  = components(0).supervisor
    val filterSupervisor    = components(1).supervisor
    val disperserSupervisor = components(2).supervisor

    assemblySupervisor ! ComponentStateSubscription(Subscribe(assemblyProbe.ref))
    filterSupervisor ! ComponentStateSubscription(Subscribe(filterProbe.ref))
    disperserSupervisor ! ComponentStateSubscription(Subscribe(disperserProbe.ref))

    assemblySupervisor ! LifecycleStateSubscription(Subscribe(assemblyLifecycleStateProbe.ref))
    filterSupervisor ! LifecycleStateSubscription(Subscribe(filterLifecycleStateProbe.ref))
    disperserSupervisor ! LifecycleStateSubscription(Subscribe(disperserLifecycleStateProbe.ref))

    /*assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(runChoice))))

    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(runChoice))))

    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(initChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(runChoice))))

    assemblyLifecycleStateProbe.expectMsg(LifecycleStateChanged(assemblySupervisor, SupervisorMode.Running))
    disperserLifecycleStateProbe.expectMsg(LifecycleStateChanged(disperserSupervisor, SupervisorMode.Running))
    filterLifecycleStateProbe.expectMsg(LifecycleStateChanged(filterSupervisor, SupervisorMode.Running))*/

    Thread.sleep(200)

    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerModeMsg(ContainerMode.Running))

    containerRef ! Lifecycle(GoOffline)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(offlineChoice))))

    containerRef ! Lifecycle(GoOnline)
    assemblyProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))
    filterProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))
    disperserProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(onlineChoice))))

    containerRef ! Lifecycle(Restart)

    containerRef ! GetContainerMode(containerModeProbe.ref)
    containerModeProbe.expectMsg(ContainerModeMsg(ContainerMode.Idle))

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
    containerModeProbe.expectMsg(ContainerModeMsg(ContainerMode.Running))
  }

}
