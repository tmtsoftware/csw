package csw.command.client

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.util.Timeout
import csw.command.client.CompleterActor.{OverallFailure, OverallSuccess}
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.{Completed, Error, Invalid, Started}
import csw.params.core.models.Id
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class CompleterTest extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {

  implicit private val timeout: Timeout = Timeout(10.seconds)

  private def blockFor[T](f: Future[T], duration: FiniteDuration = 3.seconds): T = Await.result(f, duration)

  test("case with all started") {
    val testKit = ActorTestKit()
    val id1     = Id("1")
    val id2     = Id("2")
    val id3     = Id("3")
    val r1      = Future.successful(Started(CommandName("1"), id1))
    val r2      = Future.successful(Started(CommandName("2"), id2))
    val r3      = Future.successful(Started(CommandName("3"), id3))

    implicit val system = testKit.system

    val completer = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r2, r3), None, None, None)))

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
    val testKit = ActorTestKit()
    val id1     = Id("1")
    val id2     = Id("2")
    val id3     = Id("3")
    val r1      = Future.successful(Started(CommandName("1"), id1))
    val r2      = Future.successful(Started(CommandName("2"), id2))
    val r3      = Future.successful(Started(CommandName("3"), id3))

    implicit val system = testKit.system

    val completer = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r2, r3), None, None, None)))

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
    val testKit = ActorTestKit()
    val id1     = Id("1")
    val id2     = Id("2")
    val id3     = Id("3")
    val r1      = Future.successful(Completed(CommandName("1"), id1))
    val r2      = Future.successful(Started(CommandName("2"), id2))
    val r3      = Future.successful(Completed(CommandName("3"), id3))

    implicit val system = testKit.system

    val completer = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r2, r3), None, None, None)))

    val eventualResponse = completer.waitComplete()
    completer.update(Completed(CommandName("2"), id2))

    val response = blockFor(eventualResponse)
    response shouldEqual OverallSuccess(
      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
    )
  }

  test("case with update not in completer") {
    val testKit = ActorTestKit()
    val id1     = Id("1")
    val id2     = Id("2")
    val id3     = Id("3")
    val r1      = Future.successful(Started(CommandName("1"), id1))
    val r3      = Future.successful(Started(CommandName("3"), id3))

    implicit val system = testKit.system

    val c1 = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r3), None, None, None)))

    val eventualResponse = c1.waitComplete()
    c1.update(Completed(CommandName("1"), id1))
    c1.update(Completed(CommandName("2"), id2)) // This one is not in completer
    c1.update(Completed(CommandName("3"), id3))

    val res = blockFor(eventualResponse)
    res shouldEqual OverallSuccess(Set(Completed(CommandName("1"), id1), Completed(CommandName("3"), id3)))
  }

  test("case with all started but makes an error") {
    val testKit = ActorTestKit()
    val id1     = Id("1")
    val id2     = Id("2")
    val id3     = Id("3")
    val r1      = Future.successful(Started(CommandName("1"), id1))
    val r2      = Future.successful(Started(CommandName("2"), id2))
    val r3      = Future.successful(Started(CommandName("3"), id3))

    implicit val system = testKit.system

    val completer = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r2, r3), None, None, None)))

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
    val testKit = ActorTestKit()
    val id1     = Id("1")
    val id2     = Id("2")
    val id3     = Id("3")
    val r1      = Future.successful(Completed(CommandName("1"), id1))
    val r2      = Future.successful(Completed(CommandName("2"), id2))
    val r3      = Future.successful(Completed(CommandName("3"), id3))

    implicit val system = testKit.system

    val completer = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r2, r3), None, None, None)))

    val res = blockFor(completer.waitComplete())
    res shouldEqual OverallSuccess(
      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
    )
  }

  test("case with all already Error") {
    val testKit = ActorTestKit()
    val id1     = Id("1")
    val id2     = Id("2")
    val r1      = Future.successful(Error(CommandName("1"), id1, "Error"))
    val r2      = Future.successful(Error(CommandName("2"), id2, "Error"))

    implicit val system = testKit.system

    val completer = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r2), None, None, None)))

    val res = blockFor(completer.waitComplete())
    res shouldEqual OverallFailure(Set(Error(CommandName("1"), id1, "Error"), Error(CommandName("2"), id2, "Error")))
  }

  test("case with one Invalid") {
    val testKit = ActorTestKit()
    val id1     = Id("1")
    val id2     = Id("2")
    val r1      = Future.successful(Completed(CommandName("1"), id1))
    val r2      = Future.successful(Invalid(CommandName("2"), id2, OtherIssue("TEST")))

    implicit val system = testKit.system

    val completer = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r2), None, None, None)))

    val res = blockFor(completer.waitComplete())
    res shouldEqual OverallFailure(Set(Completed(CommandName("1"), id1), Invalid(CommandName("2"), id2, OtherIssue("TEST"))))
  }

  // This works because of use of Sets
  test("case where updating same response more than once") {
    val testKit         = ActorTestKit()
    implicit val system = testKit.system

    val id1 = Id("1")
    val id2 = Id("2")
    val r1  = Future.successful(Started(CommandName("1"), id1))
    val r2  = Future.successful(Started(CommandName("2"), id2))

    val completer = new Completer(testKit.spawn(CompleterActor.behavior(Set(r1, r2), None, None, None)))

    val eventualResponse = completer.waitComplete()

    completer.update(Completed(CommandName("1"), id1))
    completer.update(Error(CommandName("1"), id1, "Error"))
    completer.update(Completed(CommandName("2"), id2))

    val response = blockFor(eventualResponse)
    response shouldEqual OverallSuccess(Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2)))
  }
}
