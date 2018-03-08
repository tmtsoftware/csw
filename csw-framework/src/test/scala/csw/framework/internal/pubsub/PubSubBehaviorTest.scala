package csw.framework.internal.pubsub

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{typed, ActorSystem}
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import csw.framework.FrameworkTestMocks
import csw.messages.framework.SupervisorLifecycleState
import csw.messages.models.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.messages.models.{LifecycleStateChanged, PubSub}
import csw.messages.{models, ComponentMessage}
import csw.services.location.commons.ActorSystemFactory
import csw.services.logging.scaladsl.Logger
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PubSubBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll {

  trait MutableActorMock[T] { this: Behaviors.MutableBehavior[T] ⇒
    protected lazy val log: Logger = MockitoSugar.mock[Logger]
  }

  implicit val untypedSystem: ActorSystem       = ActorSystemFactory.remote("test-1")
  implicit val system: typed.ActorSystem[_]     = untypedSystem.toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(system)
  private val mocks                             = new FrameworkTestMocks()

  private val lifecycleProbe1 = TestProbe[LifecycleStateChanged]
  private val lifecycleProbe2 = TestProbe[LifecycleStateChanged]

  def createPubSubBehavior(): BehaviorTestKit[PubSub[LifecycleStateChanged]] =
    BehaviorTestKit(Behaviors.mutable[PubSub[LifecycleStateChanged]](ctx ⇒ new PubSubBehavior(ctx, mocks.loggerFactory)))

  override protected def afterAll(): Unit = Await.result(untypedSystem.terminate(), 5.seconds)

  test("message should be published to all the subscribers") {
    val pubSubBehavior: BehaviorTestKit[PubSub[LifecycleStateChanged]] = createPubSubBehavior()
    val supervisorProbe                                                = TestProbe[ComponentMessage]

    pubSubBehavior.run(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.run(Subscribe(lifecycleProbe2.ref))

    pubSubBehavior.run(
      Publish(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    )
    lifecycleProbe1.expectMessage(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe2.expectMessage(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
  }

  test("should not receive messages on un-subscription") {
    val pubSubBehavior: BehaviorTestKit[PubSub[LifecycleStateChanged]] = createPubSubBehavior()
    val supervisorProbe                                                = TestProbe[ComponentMessage]

    pubSubBehavior.run(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.run(Subscribe(lifecycleProbe2.ref))
    pubSubBehavior.run(Unsubscribe(lifecycleProbe1.ref))

    pubSubBehavior.run(
      Publish(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    )

    lifecycleProbe2.expectMessage(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe1.expectNoMessage(50.millis)
  }
}
