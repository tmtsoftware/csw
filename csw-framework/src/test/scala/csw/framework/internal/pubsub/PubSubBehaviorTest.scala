package csw.framework.internal.pubsub

import akka.actor.ActorSystem
import akka.typed
import akka.typed.ActorRef
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import csw.messages.framework.SupervisorLifecycleState
import csw.messages.models.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.messages.models.{LifecycleStateChanged, PubSub}
import csw.messages.{models, SupervisorExternalMessage}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PubSubBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll {

  trait MutableActorMock[T] { this: Actor.MutableBehavior[T] ⇒
    protected lazy val log: Logger = MockitoSugar.mock[Logger]
  }

  private val actorSystem                        = ActorSystem("test-1")
  implicit val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  private val ctx = new StubbedActorContext[PubSub[LifecycleStateChanged]]("test-supervisor", 100, typedSystem)

  private val lifecycleProbe1 = TestProbe[LifecycleStateChanged]
  private val lifecycleProbe2 = TestProbe[LifecycleStateChanged]

  def createPubSubBehavior(): PubSubBehavior[LifecycleStateChanged] = new PubSubBehavior(ctx, new LoggerFactory("test-component"))

  override protected def afterAll(): Unit = Await.result(actorSystem.terminate(), 5.seconds)

  test("initially set of subscribers should be empty") {
    createPubSubBehavior().subscribers shouldBe Set.empty[ActorRef[LifecycleStateChanged]]
  }

  test("should able to subscribe") {
    val pubSubBehavior: PubSubBehavior[LifecycleStateChanged] = createPubSubBehavior()

    pubSubBehavior.onMessage(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.onMessage(Subscribe(lifecycleProbe2.ref))

    pubSubBehavior.subscribers shouldBe Set(lifecycleProbe1.ref, lifecycleProbe2.ref)
  }

  test("message should be published to all the subscribers") {
    val pubSubBehavior: PubSubBehavior[LifecycleStateChanged] = createPubSubBehavior()
    val supervisorProbe                                       = TestProbe[SupervisorExternalMessage]

    pubSubBehavior.onMessage(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.onMessage(Subscribe(lifecycleProbe2.ref))

    pubSubBehavior.onMessage(
      Publish(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    )
    lifecycleProbe1.expectMsg(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe2.expectMsg(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
  }

  test("should not receive messages on un-subscription") {
    val pubSubBehavior: PubSubBehavior[LifecycleStateChanged] = createPubSubBehavior()
    val supervisorProbe                                       = TestProbe[SupervisorExternalMessage]

    pubSubBehavior.onMessage(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.onMessage(Subscribe(lifecycleProbe2.ref))
    pubSubBehavior.onMessage(Unsubscribe(lifecycleProbe1.ref))

    pubSubBehavior.onMessage(
      Publish(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    )

    lifecycleProbe2.expectMsg(models.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe1.expectNoMsg(50.millis)
  }
}
