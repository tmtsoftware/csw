package csw.common.framework.internal.supervisor

import akka.typed.testkit.Effect._
import akka.typed.testkit.EffectfulActorContext
import akka.typed.{Behavior, Props, Terminated}
import csw.common.components.ComponentDomainMsg
import csw.common.framework.FrameworkComponentTestSuite
import csw.common.framework.internal.Supervisor
import csw.common.framework.models.PreparingToShutdownMsg.ShutdownTimeout
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorIdleMsg.{Initialized, Running}
import csw.common.framework.models.{SupervisorMsg, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.{ComponentHandlers, SupervisorBehaviorFactory}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuiteLike, Matchers}

class SupervisorBehaviorTest extends FrameworkComponentTestSuite with FunSuiteLike with Matchers with MockitoSugar {

  class TestData {
    val sampleHcdHandler: ComponentHandlers[ComponentDomainMsg] = mock[ComponentHandlers[ComponentDomainMsg]]
    val supervisorBehavior: Behavior[SupervisorMsg]             = SupervisorBehaviorFactory.make(hcdInfo)
    val ctx =
      new EffectfulActorContext[SupervisorMsg]("supervisor-test", supervisorBehavior, 100, system)
  }

  test(
    "Supervisor should create child actors for TLA, pub-sub actor for lifecycle and pub-sub actor for component state"
  ) {
    val testData = new TestData
    import testData._

    ctx.getAllEffects() should contain allOf (
      Spawned(Supervisor.ComponentActor, Props.empty),
      Spawned(Supervisor.PubSubLifecycleActor, Props.empty),
      Spawned(Supervisor.PubSubComponentActor, Props.empty)
    )
  }

  test("Supervisor should watch child component actor") {
    val testData = new TestData
    import testData._

    val componentActor       = ctx.childInbox(Supervisor.ComponentActor).ref
    val pubSubLifecycleActor = ctx.childInbox(Supervisor.PubSubLifecycleActor).ref
    val pubSubComponentActor = ctx.childInbox(Supervisor.PubSubComponentActor).ref

    ctx.getAllEffects() should contain(Watched(componentActor))
    ctx.getAllEffects() should not contain Watched(pubSubLifecycleActor)
    ctx.getAllEffects() should not contain Watched(pubSubComponentActor)
  }

  test("Supervisor should handle Shutdown message by scheduling a timer") {
    val testData = new TestData
    import testData._

    val componentActor = ctx.childInbox(Supervisor.ComponentActor).ref

    ctx.run(Initialized(componentActor))
    ctx.run(Running(componentActor))
    ctx.run(Lifecycle(ToComponentLifecycleMessage.Shutdown))

    ctx.getAllEffects() should contain(Scheduled(Supervisor.shutdownTimeout, ctx.self, ShutdownTimeout))
  }

  test("Supervisor should handle Terminated signal by unwatching component actor and stopping other child actors") {
    val testData = new TestData
    import testData._

    val componentActor = ctx.childInbox(Supervisor.ComponentActor).ref

    ctx.signal(Terminated(ctx.getChildren.get(0).ref)(null))

    ctx.getAllEffects() should contain allOf (
      Unwatched(componentActor),
      Stopped(Supervisor.PubSubLifecycleActor),
      Stopped(Supervisor.PubSubComponentActor)
    )
  }
}
