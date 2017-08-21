package csw.common.framework.internal

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import csw.common.framework.FrameworkComponentTestInfos._
import csw.common.framework.models.CommonSupervisorMsg.LifecycleStateSubscription
import csw.common.framework.models.ContainerMsg.{GetComponents, LifecycleStateChanged}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.models.{Components, ContainerMsg, ToComponentLifecycleMessage}
import org.scalatest.{FunSuite, Matchers}

//DEOPSCSW-182-Control Life Cycle of Components
class ContainerTest extends FunSuite with Matchers {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Actor.empty, "container-system")
  implicit val settings: TestKitSettings    = TestKitSettings(system)

  class RunningContainer() {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    ctx.children.map(child ⇒ container.onMessage(LifecycleStateChanged(SupervisorMode.Running, child.upcast)))

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveAll())
  }

  test("container should start in initialize mode and should not accept any outside messages") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    container.mode shouldBe ContainerMode.Initialize
  }

  test("container should change its mode to running after all components move to running mode") {
    val ctx       = new StubbedActorContext[ContainerMsg]("test-container", 100, system)
    val container = new Container(ctx, containerInfo)

    ctx.children.size shouldBe containerInfo.maybeComponentInfoes.get.size
    container.supervisors.size shouldBe 2
    container.supervisors.map(_.componentInfo).toSet shouldBe containerInfo.maybeComponentInfoes.get

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(Subscribe[LifecycleStateChanged](ctx.self))

    ctx.children.map(child ⇒ container.onMessage(LifecycleStateChanged(SupervisorMode.Running, child.upcast)))

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(Unsubscribe[LifecycleStateChanged](ctx.self))

    container.mode shouldBe ContainerMode.Running
  }

  test("container should handle Lifecycle messages by forwarding to all components") {
    val runningContainer = new RunningContainer
    import runningContainer._

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
    val runningContainer = new RunningContainer
    import runningContainer._

    container.runningComponents shouldBe List.empty
    container.onMessage(Lifecycle(Restart))
    container.mode shouldBe ContainerMode.Initialize

    ctx.children
      .map(child ⇒ ctx.childInbox(child.upcast))
      .map(_.receiveMsg()) should contain only LifecycleStateSubscription(Subscribe[LifecycleStateChanged](ctx.self))

    ctx.children.map(child ⇒ ctx.childInbox(child.upcast)).map(_.receiveMsg()) should contain only Lifecycle(Restart)
  }

  test("container should change its mode from restarting to running after all components have restarted") {
    val runningContainer = new RunningContainer
    import runningContainer._

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
    val runningContainer = new RunningContainer
    import runningContainer._

    val probe = TestProbe[Components]

    container.onMessage(GetComponents(probe.ref))

    probe.expectMsg(Components(container.supervisors))
  }

}
