package csw.common.framework.internal.supervisor

import akka.typed.testkit.Effect._
import akka.typed.testkit.EffectfulActorContext
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{Behavior, Props, Terminated}
import csw.common.framework.ComponentInfos._
import csw.common.framework.FrameworkTestSuite
import csw.common.framework.models.{ContainerIdleMessage, SupervisorExternalMessage}
import org.scalatest.mockito.MockitoSugar

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
class SupervisorBehaviorTest extends FrameworkTestSuite with MockitoSugar {

  val containerIdleMessageProbe: TestProbe[ContainerIdleMessage] = TestProbe[ContainerIdleMessage]
  val supervisorBehavior: Behavior[SupervisorExternalMessage] =
    SupervisorBehaviorFactory.behavior(
      Some(containerIdleMessageProbe.testActor),
      hcdInfo,
      locationService,
      registrationFactory
    )

  test("Supervisor should create child actors for TLA, pub-sub actor for lifecycle and component state") {
    val ctx = new EffectfulActorContext[SupervisorExternalMessage]("supervisor", supervisorBehavior, 100, system)

    ctx.getAllEffects() should contain allOf (
      Spawned(SupervisorBehavior.ComponentActor, Props.empty),
      Spawned(SupervisorBehavior.PubSubLifecycleActor, Props.empty),
      Spawned(SupervisorBehavior.PubSubComponentActor, Props.empty)
    )
  }

  test("Supervisor should watch child component actor [TLA]") {
    val ctx = new EffectfulActorContext[SupervisorExternalMessage]("supervisor", supervisorBehavior, 100, system)

    val componentActor       = ctx.childInbox(SupervisorBehavior.ComponentActor).ref
    val pubSubLifecycleActor = ctx.childInbox(SupervisorBehavior.PubSubLifecycleActor).ref
    val pubSubComponentActor = ctx.childInbox(SupervisorBehavior.PubSubComponentActor).ref

    ctx.getAllEffects() should contain(Watched(componentActor))
    ctx.getAllEffects() should not contain Watched(pubSubLifecycleActor)
    ctx.getAllEffects() should not contain Watched(pubSubComponentActor)
  }

  test("Supervisor should handle Terminated signal by unwatching component actor and stopping other child actors") {
    val ctx = new EffectfulActorContext[SupervisorExternalMessage]("supervisor", supervisorBehavior, 100, system)

    val componentActor = ctx.childInbox(SupervisorBehavior.ComponentActor).ref

    ctx.signal(Terminated(ctx.getChildren.get(0).ref)(null))

    ctx.getAllEffects() should contain allOf (
      Unwatched(componentActor),
      Stopped(SupervisorBehavior.PubSubLifecycleActor),
      Stopped(SupervisorBehavior.PubSubComponentActor)
    )
  }
}
