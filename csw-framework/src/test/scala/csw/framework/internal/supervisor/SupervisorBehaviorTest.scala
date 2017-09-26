package csw.framework.internal.supervisor

import akka.typed.scaladsl.Actor
import akka.typed.testkit.Effect._
import akka.typed.testkit.EffectfulActorContext
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{Behavior, Props}
import csw.common.components.SampleComponentBehaviorFactory
import csw.framework.ComponentInfos._
import csw.framework.FrameworkTestMocks.TypedActorMock
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.param.messages.{ContainerIdleMessage, SupervisorExternalMessage, SupervisorMessage}
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import org.scalatest.mockito.MockitoSugar

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
class SupervisorBehaviorTest extends FrameworkTestSuite with MockitoSugar {
  val testMocks: FrameworkTestMocks = frameworkTestMocks()
  import testMocks._

  val containerIdleMessageProbe: TestProbe[ContainerIdleMessage] = TestProbe[ContainerIdleMessage]
  val supervisorBehavior: Behavior[SupervisorExternalMessage]    = createBehavior()
  val componentTLAName                                           = s"${hcdInfo.name}-${SupervisorBehavior.ComponentActorNameSuffix}"

  test("Supervisor should create child actors for TLA, pub-sub actor for lifecycle and component state") {
    val ctx = new EffectfulActorContext[SupervisorExternalMessage]("supervisor", supervisorBehavior, 100, system)

    ctx.getAllEffects() should contain allOf (
      Spawned(componentTLAName, Props.empty),
      Spawned(SupervisorBehavior.PubSubLifecycleActor, Props.empty),
      Spawned(SupervisorBehavior.PubSubComponentActor, Props.empty)
    )
  }

  test("Supervisor should watch child component actor [TLA]") {
    val ctx = new EffectfulActorContext[SupervisorExternalMessage]("supervisor", supervisorBehavior, 100, system)

    val componentActor       = ctx.childInbox(componentTLAName).ref
    val pubSubLifecycleActor = ctx.childInbox(SupervisorBehavior.PubSubLifecycleActor).ref
    val pubSubComponentActor = ctx.childInbox(SupervisorBehavior.PubSubComponentActor).ref

    ctx.getAllEffects() should contain(Watched(componentActor))
    ctx.getAllEffects() should not contain Watched(pubSubLifecycleActor)
    ctx.getAllEffects() should not contain Watched(pubSubComponentActor)
  }

  private def createBehavior(): Behavior[SupervisorExternalMessage] = {
    Actor
      .withTimers[SupervisorMessage](
        timerScheduler ⇒
          Actor
            .mutable[SupervisorMessage](
              ctx =>
                new SupervisorBehavior(
                  ctx,
                  timerScheduler,
                  None,
                  hcdInfo,
                  new SampleComponentBehaviorFactory,
                  new PubSubBehaviorFactory,
                  registrationFactory,
                  locationService
                ) with TypedActorMock[SupervisorMessage]
          )
      )
      .narrow
  }
}
