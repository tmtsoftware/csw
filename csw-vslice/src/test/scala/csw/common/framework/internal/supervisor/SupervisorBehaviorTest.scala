package csw.common.framework.internal.supervisor

import akka.typed.testkit.Effect._
import akka.typed.testkit.EffectfulActorContext
import akka.typed.{Behavior, Props, Terminated}
import csw.common.framework.FrameworkComponentTestInfos._
import csw.common.framework.FrameworkComponentTestSuite
import csw.common.framework.internal.Supervisor
import csw.common.framework.models.SupervisorExternalMessage
import csw.common.framework.scaladsl.SupervisorBehaviorFactory
import org.scalatest.mockito.MockitoSugar

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
class SupervisorBehaviorTest extends FrameworkComponentTestSuite with MockitoSugar {

  val supervisorBehavior: Behavior[SupervisorExternalMessage] =
    SupervisorBehaviorFactory.behavior(hcdInfo, locationService, registrationFactory)

  test("Supervisor should create child actors for TLA, pub-sub actor for lifecycle and component state") {
    val ctx = new EffectfulActorContext[SupervisorExternalMessage]("supervisor", supervisorBehavior, 100, system)

    ctx.getAllEffects() should contain allOf (
      Spawned(Supervisor.ComponentActor, Props.empty),
      Spawned(Supervisor.PubSubLifecycleActor, Props.empty),
      Spawned(Supervisor.PubSubComponentActor, Props.empty)
    )
  }

  test("Supervisor should watch child component actor [TLA]") {
    val ctx = new EffectfulActorContext[SupervisorExternalMessage]("supervisor", supervisorBehavior, 100, system)

    val componentActor       = ctx.childInbox(Supervisor.ComponentActor).ref
    val pubSubLifecycleActor = ctx.childInbox(Supervisor.PubSubLifecycleActor).ref
    val pubSubComponentActor = ctx.childInbox(Supervisor.PubSubComponentActor).ref

    ctx.getAllEffects() should contain(Watched(componentActor))
    ctx.getAllEffects() should not contain Watched(pubSubLifecycleActor)
    ctx.getAllEffects() should not contain Watched(pubSubComponentActor)
  }

  test("Supervisor should handle Terminated signal by unwatching component actor and stopping other child actors") {
    val ctx = new EffectfulActorContext[SupervisorExternalMessage]("supervisor", supervisorBehavior, 100, system)

    val componentActor = ctx.childInbox(Supervisor.ComponentActor).ref

    ctx.signal(Terminated(ctx.getChildren.get(0).ref)(null))

    ctx.getAllEffects() should contain allOf (
      Unwatched(componentActor),
      Stopped(Supervisor.PubSubLifecycleActor),
      Stopped(Supervisor.PubSubComponentActor)
    )
  }
}
