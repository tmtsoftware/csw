package csw.command.client

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.client.CompleterActor.{OverallFailure, OverallSuccess}
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, ControlCommand, Setup}
import csw.params.core.models.{Id, ObsId, Prefix}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class CompleterTest extends FunSuite with Matchers with BeforeAndAfterEach with MockitoSugar with ArgumentMatchersSugar {

  implicit private val timeout: Timeout = Timeout(10.seconds)

  private def blockFor[T](f: Future[T], duration: FiniteDuration = 10.seconds): T = Await.result(f, duration)

  var testKit: ActorTestKit = _

  override def beforeEach(): Unit = testKit = ActorTestKit()
  override def afterEach(): Unit  = testKit.shutdownTestKit()

  private def getCompleter(responses: Set[Future[SubmitResponse]]): Completer = {
    implicit val system: ActorSystem[Nothing] = testKit.system
    new Completer(
      testKit.spawn(CompleterActor.behavior(responses, None, None, None))
    )
  }

  private def getCompleterWithAutoCompletion(
      responses: Set[Future[SubmitResponse]],
      parentId: Id,
      parentCommand: ControlCommand,
      crm: CommandResponseManager
  ): Completer = {
    implicit val system: ActorSystem[Nothing] = testKit.system
    new Completer(
      testKit.spawn(CompleterActor.behavior(responses, Some(parentId), Some(parentCommand), Some(crm)))
    )
  }

  test("case with all started") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Future.successful(Started(CommandName("1"), id1))
    val r2  = Future.successful(Started(CommandName("2"), id2))
    val r3  = Future.successful(Started(CommandName("3"), id3))

    val completer = getCompleter(Set(r1, r2, r3))

    val eventualResponse = completer.waitComplete()
    completer.update(Completed(CommandName("1"), id1))
    completer.update(Completed(CommandName("2"), id2))
    completer.update(Completed(CommandName("3"), id3))

    val response = Await.result(eventualResponse, 5.seconds)
    response shouldBe OverallSuccess(
      Set(
        Completed(CommandName("1"), id1),
        Completed(CommandName("2"), id2),
        Completed(CommandName("3"), id3)
      )
    )
  }

  test("case with all started, first returns error") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Future.successful(Started(CommandName("1"), id1))
    val r2  = Future.successful(Started(CommandName("2"), id2))
    val r3  = Future.successful(Started(CommandName("3"), id3))

    val completer = getCompleter(Set(r1, r2, r3))

    val eventualResponse = completer.waitComplete()
    completer.update(Error(CommandName("1"), id1, "ERROR"))
    completer.update(Completed(CommandName("2"), id2))
    completer.update(Completed(CommandName("3"), id3))

    val response = blockFor(eventualResponse)
    response shouldEqual OverallFailure(
      Set(Error(CommandName("1"), id1, "ERROR"), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
    )
  }

  test("case with 2 completed and 1 started") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Future.successful(Completed(CommandName("1"), id1))
    val r2  = Future.successful(Started(CommandName("2"), id2))
    val r3  = Future.successful(Completed(CommandName("3"), id3))

    val completer = getCompleter(Set(r1, r2, r3))

    val eventualResponse = completer.waitComplete()
    completer.update(Completed(CommandName("2"), id2))

    val response = blockFor(eventualResponse)
    response shouldEqual OverallSuccess(
      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
    )
  }

  test("case with update not in completer") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Future.successful(Started(CommandName("1"), id1))
    val r3  = Future.successful(Started(CommandName("3"), id3))

    val c1 = getCompleter(Set(r1, r3))

    val eventualResponse = c1.waitComplete()
    c1.update(Completed(CommandName("1"), id1))
    c1.update(Completed(CommandName("2"), id2)) // This one is not in completer
    c1.update(Completed(CommandName("3"), id3))

    val res = blockFor(eventualResponse)
    res shouldEqual OverallSuccess(Set(Completed(CommandName("1"), id1), Completed(CommandName("3"), id3)))
  }

  test("case with all started but makes an error") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Future.successful(Started(CommandName("1"), id1))
    val r2  = Future.successful(Started(CommandName("2"), id2))
    val r3  = Future.successful(Started(CommandName("3"), id3))

    val completer = getCompleter(Set(r1, r2, r3))

    val eventualResponse = completer.waitComplete()
    completer.update(Completed(CommandName("1"), id1))
    completer.update(Error(CommandName("2"), id2, "Error"))
    completer.update(Completed(CommandName("3"), id3))

    val response = blockFor(eventualResponse)
    response shouldEqual OverallFailure(
      Set(Completed(CommandName("1"), id1), Error(CommandName("2"), id2, "Error"), Completed(CommandName("3"), id3))
    )
  }

  test("case with all already completed") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Future.successful(Completed(CommandName("1"), id1))
    val r2  = Future.successful(Completed(CommandName("2"), id2))
    val r3  = Future.successful(Completed(CommandName("3"), id3))

    val completer = getCompleter(Set(r1, r2, r3))

    val res = blockFor(completer.waitComplete())
    res shouldEqual OverallSuccess(
      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
    )
  }

  test("case with all already Error") {
    val id1 = Id("1")
    val id2 = Id("2")
    val r1  = Future.successful(Error(CommandName("1"), id1, "Error"))
    val r2  = Future.successful(Error(CommandName("2"), id2, "Error"))

    val completer = getCompleter(Set(r1, r2))

    val res = blockFor(completer.waitComplete())
    res shouldEqual OverallFailure(Set(Error(CommandName("1"), id1, "Error"), Error(CommandName("2"), id2, "Error")))
  }

  test("case with one Invalid") {
    val id1 = Id("1")
    val id2 = Id("2")
    val r1  = Future.successful(Completed(CommandName("1"), id1))
    val r2  = Future.successful(Invalid(CommandName("2"), id2, OtherIssue("TEST")))

    val completer = getCompleter(Set(r1, r2))

    val res = blockFor(completer.waitComplete())
    res shouldEqual OverallFailure(Set(Completed(CommandName("1"), id1), Invalid(CommandName("2"), id2, OtherIssue("TEST"))))
  }

  test("case with a future fails") {
    val testKit = ActorTestKit()
    val r1      = Future.failed(new RuntimeException("network error"))
    val r2      = Future.successful(Completed(CommandName("some command"), Id("1")))

    implicit val system = testKit.system

    val completer = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r2), None, None, None)))

    val res = blockFor(completer.waitComplete())
    res shouldEqual OverallFailure(
      Set(
        Error(CommandName("Invalid"), Id("Invalid"), "One or more response future failed"),
        Completed(CommandName("some command"), Id("1"))
      )
    )
  }

  // This works because of use of Sets
  test("case where updating same response more than once") {
    val id1 = Id("1")
    val id2 = Id("2")
    val r1  = Future.successful(Started(CommandName("1"), id1))
    val r2  = Future.successful(Started(CommandName("2"), id2))

    val completer = getCompleter(Set(r1, r2))

    val eventualResponse = completer.waitComplete()

    completer.update(Completed(CommandName("1"), id1))
    completer.update(Error(CommandName("1"), id1, "Error"))
    completer.update(Completed(CommandName("2"), id2))

    val response = blockFor(eventualResponse)
    response shouldEqual OverallSuccess(Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2)))
  }

  test("should update CRM with success") {
    val r1 = Future.successful(Completed(CommandName("some command"), Id("1")))

    val parentId      = Id()
    val parentCommand = Setup(Prefix("aoesw"), CommandName("setup"), Some(ObsId("abc")))
    val crm           = mock[CommandResponseManager]

    val completer = getCompleterWithAutoCompletion(Set(r1), parentId, parentCommand, crm)

    val res = blockFor(completer.waitComplete())
    res shouldEqual OverallSuccess(Set(Completed(CommandName("some command"), Id("1"))))

    verify(crm).updateCommand(Completed(parentCommand.commandName, parentId))
  }

  test("should update CRM with error") {
    val r1 = Future.failed(new RuntimeException("network error"))
    val r2 = Future.successful(Completed(CommandName("some command"), Id("1")))

    val parentId      = Id()
    val parentCommand = Setup(Prefix("aoesw"), CommandName("setup"), Some(ObsId("abc")))
    val crm           = mock[CommandResponseManager]

    val completer = getCompleterWithAutoCompletion(Set(r1, r2), parentId, parentCommand, crm)

    val res = blockFor(completer.waitComplete())
    res shouldEqual OverallFailure(
      Set(
        Error(CommandName("Invalid"), Id("Invalid"), "One or more response future failed"),
        Completed(CommandName("some command"), Id("1"))
      )
    )

    verify(crm).updateCommand(Error(parentCommand.commandName, parentId, "Downstream failed"))
  }
}
