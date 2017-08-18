package csw.common.framework.internal

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.FrameworkComponentTestInfos._
import csw.common.framework.models.CommonSupervisorMsg.LifecycleStateSubscription
import csw.common.framework.models.ContainerMsg.{CreateComponents, GetComponents, LifecycleStateChanged}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.models.{Components, ContainerMsg, SupervisorMode, ToComponentLifecycleMessage}
import org.scalatest.{FunSuite, Matchers}

class ContainerTest extends FunSuite with Matchers {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Actor.empty, "container-system")
  implicit val settings: TestKitSettings    = TestKitSettings(system)

  test("container should start in idle mode and should not accept any outside messages") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    container.mode shouldBe ContainerMode.Idle
    ctx.selfInbox.receiveMsg() shouldBe ContainerMsg.CreateComponents(containerInfo.maybeComponentInfoes.get)
  }

  test("container should change its mode to running after creating all components") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    container.onMessage(CreateComponents(containerInfo.maybeComponentInfoes.get))
    container.mode shouldBe ContainerMode.Running

    ctx.children.size shouldBe containerInfo.maybeComponentInfoes.get.size
    container.supervisors.size shouldBe 2
    container.supervisors.map(_.componentInfo).toSet shouldBe containerInfo.maybeComponentInfoes.get
  }

  test("container should handle Lifecycle messages by forwarding to all components") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    container.onMessage(CreateComponents(containerInfo.maybeComponentInfoes.get))

    container.onMessage(Lifecycle(GoOnline))
    ctx.children.map(child ⇒ ctx.childInbox(child.upcast)).map(_.receiveMsg()) should contain only Lifecycle(GoOnline)

    container.onMessage(Lifecycle(GoOffline))
    ctx.children.map(child ⇒ ctx.childInbox(child.upcast)).map(_.receiveMsg()) should contain only Lifecycle(GoOffline)

    container.onMessage(Lifecycle(ToComponentLifecycleMessage.Shutdown))
    ctx.children.map(child ⇒ ctx.childInbox(child.upcast)).map(_.receiveMsg()) should contain only Lifecycle(
      ToComponentLifecycleMessage.Shutdown
    )
  }

  test(
    "container should handle restart message by changing its mode to restart and subscribing to all components for their running state"
  ) {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    container.onMessage(CreateComponents(containerInfo.maybeComponentInfoes.get))

    container.restarted shouldBe List.empty
    container.onMessage(Lifecycle(Restart))
    container.mode shouldBe ContainerMode.Restart

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(Subscribe[LifecycleStateChanged](ctx.self))

    ctx.children.map(child ⇒ ctx.childInbox(child.upcast)).map(_.receiveMsg()) should contain only Lifecycle(Restart)
  }

  test("container should change its mode from restarting to running after all components have restarted") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    container.onMessage(CreateComponents(containerInfo.maybeComponentInfoes.get))

    container.onMessage(Lifecycle(Restart))

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveAll())

    ctx.children.map(child ⇒ container.onMessage(LifecycleStateChanged(SupervisorMode.Running, child.upcast)))

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(Unsubscribe[LifecycleStateChanged](ctx.self))

    container.mode shouldBe ContainerMode.Running

  }

  test("container should be able to handle GetAllComponents message by responding with list of all components") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)
    val probe     = TestProbe[Components]

    container.onMessage(CreateComponents(containerInfo.maybeComponentInfoes.get))

    container.onMessage(GetComponents(probe.ref))

    probe.expectMsg(Components(container.supervisors))
  }

}
