package csw.common.framework.internal.pubsub

import akka.actor.ActorSystem
import akka.typed
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.models.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.common.framework.models.{LifecycleStateChanged, PubSub, SupervisorExternalMessage}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PubSubBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private val actorSystem                        = ActorSystem("test-1")
  implicit val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  private val ctx = new StubbedActorContext[PubSub[LifecycleStateChanged]]("test-supervisor", 100, typedSystem)

  private val lifecycleProbe1 = TestProbe[LifecycleStateChanged]
  private val lifecycleProbe2 = TestProbe[LifecycleStateChanged]

  override protected def afterAll(): Unit = Await.result(actorSystem.terminate(), 5.seconds)

  test("initially set of subscribers should be empty") {
    val pubSubBehavior: PubSubBehavior[LifecycleStateChanged] = new PubSubBehavior(ctx, "test-component")
    pubSubBehavior.subscribers shouldBe Set.empty[ActorRef[LifecycleStateChanged]]
  }

  test("should able to subscribe") {
    val pubSubBehavior: PubSubBehavior[LifecycleStateChanged] = new PubSubBehavior(ctx, "test-component")

    pubSubBehavior.onMessage(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.onMessage(Subscribe(lifecycleProbe2.ref))

    pubSubBehavior.subscribers shouldBe Set(lifecycleProbe1.ref, lifecycleProbe2.ref)
  }

  test("message should be published to all the subscribers") {
    val pubSubBehavior: PubSubBehavior[LifecycleStateChanged] = new PubSubBehavior(ctx, "test-component")
    val supervisorProbe                                       = TestProbe[SupervisorExternalMessage]

    pubSubBehavior.onMessage(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.onMessage(Subscribe(lifecycleProbe2.ref))

    pubSubBehavior.onMessage(Publish(LifecycleStateChanged(supervisorProbe.ref, SupervisorMode.Running)))
    lifecycleProbe1.expectMsg(LifecycleStateChanged(supervisorProbe.ref, SupervisorMode.Running))
    lifecycleProbe2.expectMsg(LifecycleStateChanged(supervisorProbe.ref, SupervisorMode.Running))
  }

  test("should not receive messages on un-subscription") {
    val pubSubBehavior: PubSubBehavior[LifecycleStateChanged] = new PubSubBehavior(ctx, "test-component")
    val supervisorProbe                                       = TestProbe[SupervisorExternalMessage]

    pubSubBehavior.onMessage(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.onMessage(Subscribe(lifecycleProbe2.ref))
    pubSubBehavior.onMessage(Unsubscribe(lifecycleProbe1.ref))

    pubSubBehavior.onMessage(Publish(LifecycleStateChanged(supervisorProbe.ref, SupervisorMode.Running)))

    lifecycleProbe2.expectMsg(LifecycleStateChanged(supervisorProbe.ref, SupervisorMode.Running))
    lifecycleProbe1.expectNoMsg(50.millis)
  }
}
