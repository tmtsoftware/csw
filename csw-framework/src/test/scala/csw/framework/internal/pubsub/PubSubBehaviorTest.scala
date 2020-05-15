package csw.framework.internal.pubsub

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox, TestProbe}
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.{ActorSystem, typed}
import csw.command.client.messages.ComponentMessage
import csw.command.client.models.framework.PubSub.{Publish, Subscribe, SubscribeOnly, Unsubscribe}
import csw.command.client.models.framework.{LifecycleStateChanged, PubSub, SupervisorLifecycleState}
import csw.framework.FrameworkTestMocks
import csw.location.client.ActorSystemFactory
import csw.logging.api.scaladsl.Logger
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.Prefix
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PubSubBehaviorTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  trait MutableActorMock[T] {
    protected lazy val log: Logger = MockitoSugar.mock[Logger]
  }

  implicit val system: typed.ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "test-1")
  implicit val untypedSystem: ActorSystem                       = system.toClassic
  implicit val testKitSettings: TestKitSettings                 = TestKitSettings(system)
  private val mocks                                             = new FrameworkTestMocks()
  private val prefix                                            = Prefix("wfos.red.detector")

  private val lifecycleProbe1    = TestProbe[LifecycleStateChanged]()
  private val lifecycleProbe2    = TestProbe[LifecycleStateChanged]()
  private val currentStateProbe1 = TestInbox[CurrentState]()
  private val currentStateProbe2 = TestInbox[CurrentState]()

  val currentState1 = CurrentState(prefix, StateName("testStateName1"))
  val currentState2 = CurrentState(prefix, StateName("testStateName2"))
  val currentState3 = CurrentState(prefix, StateName("testStateName3"))

  def createLifecycleStatePubSubBehavior(): BehaviorTestKit[PubSub[LifecycleStateChanged]] =
    BehaviorTestKit(PubSubBehavior.make(mocks.loggerFactory))

  def createCurrentStatePubSubBehavior(): BehaviorTestKit[PubSub[CurrentState]] =
    BehaviorTestKit(PubSubBehavior.make(mocks.loggerFactory))

  override protected def afterAll(): Unit = Await.result(untypedSystem.terminate(), 5.seconds)

  test("message should be published to all the subscribers | ") {
    val pubSubBehavior: BehaviorTestKit[PubSub[LifecycleStateChanged]] = createLifecycleStatePubSubBehavior()
    val supervisorProbe                                                = TestProbe[ComponentMessage]()

    pubSubBehavior.run(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.run(Subscribe(lifecycleProbe2.ref))

    pubSubBehavior.run(
      Publish(LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    )
    lifecycleProbe1.expectMessage(LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe2.expectMessage(LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
  }

  // DEOPSCSW-434 : Allow subscription of CurrentState using StateName
  test("message should be published to the subscribers depending on names | DEOPSCSW-434") {
    val pubSubBehavior: BehaviorTestKit[PubSub[CurrentState]] = createCurrentStatePubSubBehavior()

    pubSubBehavior.run(Subscribe(currentStateProbe1.ref))
    pubSubBehavior.run(SubscribeOnly(currentStateProbe2.ref, Set(currentState2.stateName, currentState3.stateName)))

    pubSubBehavior.run(Publish(currentState1))
    pubSubBehavior.run(Publish(currentState2))
    pubSubBehavior.run(Publish(currentState3))

    val currentStates  = currentStateProbe1.receiveAll()
    val currentStates2 = currentStateProbe2.receiveAll()

    currentStates shouldEqual Seq(currentState1, currentState2, currentState3)
    currentStates2 shouldEqual Seq(currentState2, currentState3)
  }

  test("should not receive messages on un-subscription | ") {
    val pubSubBehavior: BehaviorTestKit[PubSub[LifecycleStateChanged]] = createLifecycleStatePubSubBehavior()
    val supervisorProbe                                                = TestProbe[ComponentMessage]()

    pubSubBehavior.run(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.run(Subscribe(lifecycleProbe2.ref))
    pubSubBehavior.run(Unsubscribe(lifecycleProbe1.ref))

    pubSubBehavior.run(
      Publish(LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    )

    lifecycleProbe2.expectMessage(LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe1.expectNoMessage(50.millis)
  }
}
