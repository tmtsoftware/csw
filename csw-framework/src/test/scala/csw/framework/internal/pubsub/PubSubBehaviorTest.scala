package csw.framework.internal.pubsub

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.typed.scaladsl.{Behaviors, MutableBehavior}
import akka.actor.{typed, ActorSystem}
import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import csw.framework.FrameworkTestMocks
import csw.messages.commands.SubscriptionKey
import csw.messages.{framework, ComponentMessage}
import csw.messages.framework.PubSub.{Publish, Subscribe, SubscribeOnly, Unsubscribe}
import csw.messages.framework.{LifecycleStateChanged, PubSub, SupervisorLifecycleState}
import csw.messages.params.states.{CurrentState, StateName}
import csw.services.location.commons.ActorSystemFactory
import csw.services.logging.scaladsl.Logger
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PubSubBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll {

  trait MutableActorMock[T] { this: MutableBehavior[T] ⇒
    protected lazy val log: Logger = MockitoSugar.mock[Logger]
  }

  implicit val untypedSystem: ActorSystem       = ActorSystemFactory.remote("test-1")
  implicit val system: typed.ActorSystem[_]     = untypedSystem.toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(system)
  private val mocks                             = new FrameworkTestMocks()
  private val prefix                            = "wfos.red.detector"

  private val lifecycleProbe1    = TestProbe[LifecycleStateChanged]
  private val lifecycleProbe2    = TestProbe[LifecycleStateChanged]
  private val currentStateProbe1 = TestInbox[CurrentState]()
  private val currentStateProbe2 = TestInbox[CurrentState]()
  val currentState1              = new CurrentState(prefix, new StateName("testStateName1"))
  val currentState2              = new CurrentState(prefix, new StateName("testStateName2"))

  def createLifecycleStatePubSubBehavior(): BehaviorTestKit[PubSub[LifecycleStateChanged]] =
    BehaviorTestKit(Behaviors.setup[PubSub[LifecycleStateChanged]](ctx ⇒ new PubSubBehavior(ctx, mocks.loggerFactory)))

  def createCurrentStateStatePubSubBehavior(): BehaviorTestKit[PubSub[CurrentState]] =
    BehaviorTestKit(Behaviors.setup[PubSub[CurrentState]](ctx ⇒ new PubSubBehavior(ctx, mocks.loggerFactory)))

  override protected def afterAll(): Unit = Await.result(untypedSystem.terminate(), 5.seconds)

  test("message should be published to all the subscribers") {
    val pubSubBehavior: BehaviorTestKit[PubSub[LifecycleStateChanged]] = createLifecycleStatePubSubBehavior()
    val supervisorProbe                                                = TestProbe[ComponentMessage]

    pubSubBehavior.run(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.run(Subscribe(lifecycleProbe2.ref))

    pubSubBehavior.run(
      Publish(LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    )
    lifecycleProbe1.expectMessage(framework.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe2.expectMessage(framework.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
  }

  // DEOPSCSW-434 : Allow subscription of CurrentState using StateName
  test("message should be published to the subscribers depending on subscription key") {
    val pubSubBehavior: BehaviorTestKit[PubSub[CurrentState]] = createCurrentStateStatePubSubBehavior()
    val supervisorProbe                                       = TestProbe[ComponentMessage]

    val subscriptionKey = new SubscriptionKey[CurrentState](state ⇒ state.stateName == StateName("testStateName2"))

    pubSubBehavior.run(Subscribe(currentStateProbe1.ref))
    pubSubBehavior.run(SubscribeOnly(currentStateProbe2.ref, subscriptionKey))

    pubSubBehavior.run(Publish(currentState1))
    pubSubBehavior.run(Publish(currentState2))

    val currentStates  = currentStateProbe1.receiveAll()
    val currentStates2 = currentStateProbe2.receiveAll()

    currentStates should contain allOf (currentState1, currentState2)
    currentStates2 should contain only currentState2
  }

  test("should not receive messages on un-subscription") {
    val pubSubBehavior: BehaviorTestKit[PubSub[LifecycleStateChanged]] = createLifecycleStatePubSubBehavior()
    val supervisorProbe                                                = TestProbe[ComponentMessage]

    pubSubBehavior.run(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.run(Subscribe(lifecycleProbe2.ref))
    pubSubBehavior.run(Unsubscribe(lifecycleProbe1.ref))

    pubSubBehavior.run(
      Publish(framework.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    )

    lifecycleProbe2.expectMessage(framework.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe1.expectNoMessage(50.millis)
  }
}
