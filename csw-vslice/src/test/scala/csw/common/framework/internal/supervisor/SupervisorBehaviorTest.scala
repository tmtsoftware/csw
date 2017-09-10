package csw.common.framework.internal.supervisor

import akka.typed.testkit.Effect._
import akka.typed.testkit.EffectfulActorContext
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{Behavior, Props}
import csw.common.framework.ComponentInfos._
import csw.common.framework.internal.pubsub.PubSubBehaviorFactory
import csw.common.framework.models.ContainerIdleMessage.RegistrationComplete
import csw.common.framework.models.{ContainerIdleMessage, SupervisorExternalMessage}
import csw.common.framework.{FrameworkTestMocks, FrameworkTestSuite}
import org.scalatest.mockito.MockitoSugar

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
class SupervisorBehaviorTest extends FrameworkTestSuite with MockitoSugar {
  val testMocks: FrameworkTestMocks = frameworkTestMocks()
  import testMocks._

  val containerIdleMessageProbe: TestProbe[ContainerIdleMessage] = TestProbe[ContainerIdleMessage]
  val supervisorBehavior: Behavior[SupervisorExternalMessage] =
    SupervisorBehaviorFactory.make(
      None,
      hcdInfo,
      locationService,
      registrationFactory,
      new PubSubBehaviorFactory
    )

  test("Supervisor should create child actors for pub-sub actor for lifecycle and component state") {
    val ctx = new EffectfulActorContext[SupervisorExternalMessage]("supervisor", supervisorBehavior, 100, system)

    ctx.getAllEffects() should contain allOf (
      Spawned(SupervisorBehavior.PubSubLifecycleActor, Props.empty),
      Spawned(SupervisorBehavior.PubSubComponentActor, Props.empty)
    )

    val pubSubLifecycleActor = ctx.childInbox(SupervisorBehavior.PubSubLifecycleActor).ref
    val pubSubComponentActor = ctx.childInbox(SupervisorBehavior.PubSubComponentActor).ref

    ctx.getAllEffects() should not contain Watched(pubSubLifecycleActor)
    ctx.getAllEffects() should not contain Watched(pubSubComponentActor)
  }
}
