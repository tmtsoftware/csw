package csw.command.client

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.CommandResponseManagerActor.CRMState
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.{CommandNotAvailable, Completed, Error, QueryResponse, Started}
import csw.params.core.models.Id
import org.scalatest.{Assertion, BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Await, Future}

class CommandResponseManagerTest extends FunSuite with Matchers with BeforeAndAfterAll {
  val testKit                                 = ActorTestKit()
  implicit val actorSys: ActorSystem[Nothing] = testKit.system
  implicit val timeout: Timeout               = Timeout(5.seconds)
  val maxSize                                 = 10

  object TestDsl {
    implicit class RichBlockingFuture[T](f: Future[T]) {
      def shouldResolveTo(expected: T, timeout: FiniteDuration = 5.seconds): Assertion = {
        val actual = Await.result(f, timeout)
        actual shouldBe expected
      }

      def block(timeout: Timeout = Timeout(2.seconds)): T = Await.result(f, timeout.duration)
    }

    implicit class RichBlockingCRM[T](crm: CommandResponseManager) {

      def stateFor(id: Id)(implicit timeout: Timeout = Timeout(5.seconds)): CRMState = {
        crm.getState.block()(id)
      }

      def subscribersFor(id: Id)(implicit timeout: Timeout = Timeout(2.seconds)): Set[ActorRef[QueryResponse]] = {
        crm.stateFor(id).subscribers
      }

      def subscriberCountFor(id: Id): Int = subscribersFor(id).size

      def subscriberCount: Int = allState.values.flatMap(_.subscribers).size

      def responseFor(id: Id): QueryResponse = crm.queryFinal(id).block()

      def allState: Map[Id, CRMState] = crm.getState.block()
    }

    def sleep(duration: FiniteDuration): Unit = Thread.sleep(duration.toMillis)

    def id(id: Int): Id                   = Id(id.toString)
    def commandName(id: Int): CommandName = CommandName(id.toString)

    def completed(number: Int): Completed     = Completed(commandName(number), id(number))
    def started(number: Int): Started         = Started(commandName(number), id(number))
    def error(id: Id, message: String): Error = Error(commandName(id.id.toInt), id, message)

    def id1: Id = id(1)
    def id2: Id = id(2)

    def tooManyCommands(id: Id): Error                   = error(id, "too many commands")
    def commandNotAvailable(id: Id): CommandNotAvailable = CommandNotAvailable(CommandName("CommandNotAvailable"), id)

    def completed1: Completed = completed(1)
    def completed2: Completed = completed(2)

    def started1: Started = started(1)
    def started2: Started = started(2)
  }
  import TestDsl._

  override def afterAll(): Unit = testKit.shutdownTestKit()

  // This test just adds responses to make sure they are added properly to the response list
  test("add responses, check inclusion") {

    val crm = new CommandResponseManager(testKit.spawn(CommandResponseManagerActor.make(maxSize)))

    crm.addCommand(completed1)
    crm.responseFor(id1) shouldBe completed1

    crm.addCommand(completed2)
    crm.responseFor(id2) shouldBe completed2
  }

  // This test adds a response and then does a queryFinal to make sure that
  // 1. The queryFinal completes correctly
  // 2. The response remains
  // 4. The subscribers are removed after command completion
  test("add response, then queryFinal") {
    val crm = new CommandResponseManager(testKit.spawn(CommandResponseManagerActor.make(maxSize)))

    crm.addCommand(started1)

    val future = crm.queryFinal(id1)

    crm.subscriberCountFor(id1) shouldBe 1

    //mark the command done
    crm.updateCommand(completed1)

    sleep(200.millis)

    //ensure future got completed
    future.isCompleted shouldBe true

    future shouldResolveTo completed1

    //it should clear subscribers
    crm.subscriberCountFor(id1) shouldBe 0

    //ensure response remains
    crm.stateFor(id1) shouldBe CRMState(completed1)

  }

  test("multiple subscribers - same command") {
    val crm = new CommandResponseManager(testKit.spawn(CommandResponseManagerActor.make(maxSize)))

    crm.addCommand(started1)

    val eventualQueryResponse1 = crm.queryFinal(id1)

    crm.subscriberCountFor(id1) shouldBe 1

    val eventualQueryResponse2 = crm.queryFinal(id1)

    crm.subscriberCountFor(id1) shouldBe 2

    //mark the command done
    crm.updateCommand(completed1)

    sleep(100.millis)

    //ensure both futures got completed
    (eventualQueryResponse1.isCompleted && eventualQueryResponse2.isCompleted) shouldBe true

    eventualQueryResponse1.block() shouldBe completed1
    eventualQueryResponse2.block() shouldBe completed1

    //it should clear subscribers
    crm.subscriberCountFor(id1) shouldBe 0

    //ensure response remains
    crm.stateFor(id1) shouldBe CRMState(completed1)
  }

  test("multiple subscribers - different commands") {
    val crm = new CommandResponseManager(testKit.spawn(CommandResponseManagerActor.make(maxSize)))

    crm.addCommand(started1)
    crm.addCommand(started2)

    val f1 = crm.queryFinal(id1)
    val f2 = crm.queryFinal(id2)

    crm.subscriberCountFor(id1) shouldBe 1
    crm.subscriberCountFor(id2) shouldBe 1

    //mark the command done
    crm.updateCommand(completed1)
    crm.updateCommand(completed2)

    sleep(100.millis)

    //ensure both futures got completed
    (f1.isCompleted && f2.isCompleted) shouldBe true

    f1.block() shouldBe completed1
    f2.block() shouldBe completed2

    //it should clear subscribers
    crm.subscriberCount shouldBe 0

    //ensure response remains
    crm.stateFor(id1) shouldBe CRMState(completed1)
    crm.stateFor(id2) shouldBe CRMState(completed2)
  }

  test("queryFinal waiting for final, then check queryFinal again") {
    val crm = new CommandResponseManager(testKit.spawn(CommandResponseManagerActor.make(maxSize)))

    crm.addCommand(started1)

    val future = crm.queryFinal(id1)

    crm.subscriberCountFor(id1) shouldBe 1

    crm.query(id1) shouldResolveTo started1

    //mark the command done
    crm.updateCommand(completed1)

    sleep(100.millis)

    //ensure future got completed
    future.isCompleted shouldBe true

    future.block() shouldBe completed1

    //it should clear subscribers
    crm.subscriberCountFor(id1) shouldBe 0

    //ensure response remains
    crm.stateFor(id1) shouldBe CRMState(completed1)

    crm.query(id1) shouldResolveTo completed1

    crm.queryFinal(id1) shouldResolveTo completed1
  }

  test("maxSize - oldest response should get removed and subscribers should get notified") {

    val crm = new CommandResponseManager(testKit.spawn(CommandResponseManagerActor.make(1)))

    crm.addCommand(started1)
    crm.addCommand(started2)

    val future1 = crm.queryFinal(id1)

    val future2 = crm.queryFinal(id2)

    sleep(1.second)

    future1 shouldResolveTo tooManyCommands(id1)

    crm.query(id2) shouldResolveTo started2

    crm.updateCommand(completed2)

    future2 shouldResolveTo completed2

    crm.subscriberCount shouldBe 0

    crm.responseFor(id1) shouldBe commandNotAvailable(id1)
    crm.responseFor(id2) shouldBe completed2
  }
}
