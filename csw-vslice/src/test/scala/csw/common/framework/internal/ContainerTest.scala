package csw.common.framework.internal

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import csw.common.framework.FrameworkComponentTestInfos._
import csw.common.framework.models.CommonSupervisorMsg.LifecycleStateSubscription
import csw.common.framework.models.ContainerMsg.{GetComponents, SupervisorModeChanged}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.models._
import org.scalatest.{FunSuite, Matchers}

//DEOPSCSW-182-Control Life Cycle of Components
class ContainerTest extends FunSuite with Matchers {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Actor.empty, "container-system")
  implicit val settings: TestKitSettings    = TestKitSettings(system)

  class RunningContainer() {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    ctx.children.map(
      child ⇒ container.onMessage(SupervisorModeChanged(LifecycleStateChanged(SupervisorMode.Running, child.upcast)))
    )

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveAll())
  }

  test("should start in initialize mode and should not accept any outside messages") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    container.mode shouldBe ContainerMode.Idle
  }

  test("should change its mode to running after all components move to running mode") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    // supervisor per component + lifecycleStateTrackerRef
    ctx.children.size shouldBe (containerInfo.components.size + 1)
    container.supervisors.size shouldBe 2
    container.supervisors.map(_.componentInfo).toSet shouldBe containerInfo.components

    // check that all components received LifecycleStateSubscription message
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[CommonSupervisorMsg](component.name))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(Subscribe(container.lifecycleStateTrackerRef))

    // simulate that container receives LifecycleStateChanged to Running message from all components
    ctx.children.map(
      child ⇒ container.onMessage(SupervisorModeChanged(LifecycleStateChanged(SupervisorMode.Running, child.upcast)))
    )

    // check that lifecycleStateTrackerRef gets un-subscribed from all components
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[CommonSupervisorMsg](component.name))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(
      Unsubscribe(container.lifecycleStateTrackerRef)
    )

    container.mode shouldBe ContainerMode.Running
  }

  test("should handle Shutdown message by changing it's mode to Idle and forwarding the message to all components") {
    val runningContainer = new RunningContainer
    import runningContainer._

    container.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(ToComponentLifecycleMessage.Shutdown)

    container.mode shouldBe ContainerMode.Idle
  }

  test("should handle restart message by changing its mode to initialize and subscribes to all components") {
    val runningContainer = new RunningContainer
    import runningContainer._

    container.runningComponents shouldBe List.empty
    container.onMessage(Lifecycle(Restart))
    container.mode shouldBe ContainerMode.Idle

    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[CommonSupervisorMsg](component.name))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(Subscribe(container.lifecycleStateTrackerRef))

    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(Restart)
  }

  test("should change its mode from restarting to running after all components have restarted") {
    val runningContainer = new RunningContainer
    import runningContainer._

    container.onMessage(Lifecycle(Restart))

    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox(component.name))
      .map(_.receiveAll())

    ctx.children.map(
      child ⇒ container.onMessage(SupervisorModeChanged(LifecycleStateChanged(SupervisorMode.Running, child.upcast)))
    )

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(
      Unsubscribe(container.lifecycleStateTrackerRef)
    )

    container.mode shouldBe ContainerMode.Running
  }

  test("should handle GoOnline and GoOffline Lifecycle messages by forwarding to all components") {
    val runningContainer = new RunningContainer
    import runningContainer._

    val initialMode = container.mode

    container.onMessage(Lifecycle(GoOnline))
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(GoOnline)

    initialMode shouldBe container.mode

    container.onMessage(Lifecycle(GoOffline))
    containerInfo.components.toList
      .map(component ⇒ ctx.childInbox[SupervisorExternalMessage](component.name))
      .map(_.receiveMsg()) should contain only Lifecycle(GoOffline)

    initialMode shouldBe container.mode
  }

  test("container should be able to handle GetAllComponents message by responding with list of all components") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    // Container should handle GetComponents message in Idle mode
    container.mode shouldBe ContainerMode.Idle
    val probe = TestProbe[Components]

    container.onMessage(GetComponents(probe.ref))

    probe.expectMsg(Components(container.supervisors))

    // Container should handle GetComponents message in Running mode
    ctx.children.map(
      child ⇒ container.onMessage(SupervisorModeChanged(LifecycleStateChanged(SupervisorMode.Running, child.upcast)))
    )

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveAll())

    container.mode shouldBe ContainerMode.Running

    container.onMessage(GetComponents(probe.ref))

    probe.expectMsg(Components(container.supervisors))
  }
}
