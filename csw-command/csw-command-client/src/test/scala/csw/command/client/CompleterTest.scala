package csw.command.client

import csw.command.api.Completer.{Completer, OverallFailure, OverallSuccess}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.{Completed, Error, Invalid, Started}
import csw.params.core.models.Id
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class CompleterTest extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {

  private val loggerFactory = mock[LoggerFactory]
  private val logger        = mock[Logger]
  when(loggerFactory.getLogger).thenReturn(logger)

  test("test easy case with all started") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Started(CommandName("1"), id1)
    val r2  = Started(CommandName("2"), id2)
    val r3  = Started(CommandName("3"), id3)

    val c1 = new Completer(Set(r1, r2, r3), loggerFactory)

    val x = c1.waitComplete()
    c1.update(Completed(CommandName("1"), id1))
    c1.update(Completed(CommandName("2"), id2))
    c1.update(Completed(CommandName("3"), id3))

    val res = Await.result(x, 1.seconds)
    res shouldEqual OverallSuccess(
      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
    )
  }

  test("test case with all started, first returns error") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Started(CommandName("1"), id1)
    val r2  = Started(CommandName("2"), id2)
    val r3  = Started(CommandName("3"), id3)

    val c1 = new Completer(Set(r1, r2, r3), loggerFactory)

    val x = c1.waitComplete()
    c1.update(Error(CommandName("1"), id1, "ERROR"))
    c1.update(Completed(CommandName("2"), id2))
    c1.update(Completed(CommandName("3"), id3))

    val res = Await.result(x, 1.seconds)
    res shouldEqual OverallFailure(
      Set(Error(CommandName("1"), id1, "ERROR"), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
    )
  }

  test("test easy case with 2 completed and 1 started") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Completed(CommandName("1"), id1)
    val r2  = Started(CommandName("2"), id2)
    val r3  = Completed(CommandName("3"), id3)

    val c1 = new Completer(Set(r1, r2, r3), loggerFactory)

    val x = c1.waitComplete()
    c1.update(Completed(CommandName("2"), id2))

    val res = Await.result(x, 1.seconds)
    res shouldEqual OverallSuccess(
      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
    )
  }

  test("test easy case with update not in completer") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Started(CommandName("1"), id1)
    val r3  = Started(CommandName("3"), id3)

    val c1 = new Completer(Set(r1, r3), loggerFactory)

    val x = c1.waitComplete()
    c1.update(Completed(CommandName("1"), id1))
    c1.update(Completed(CommandName("2"), id2)) // This one is not in completer
    c1.update(Completed(CommandName("3"), id3))

    val res = Await.result(x, 1.seconds)
    res shouldEqual OverallSuccess(Set(Completed(CommandName("1"), id1), Completed(CommandName("3"), id3)))
  }

  test("test easy case with all started but makes an error") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Started(CommandName("1"), id1)
    val r2  = Started(CommandName("2"), id2)
    val r3  = Started(CommandName("3"), id3)

    val c1 = new Completer(Set(r1, r2, r3), loggerFactory)

    val x = c1.waitComplete()
    c1.update(Completed(CommandName("1"), id1))
    c1.update(Error(CommandName("2"), id2, "Error"))
    c1.update(Completed(CommandName("3"), id3))

    val res = Await.result(x, 1.seconds)
    res shouldEqual OverallFailure(
      Set(Completed(CommandName("1"), id1), Error(CommandName("2"), id2, "Error"), Completed(CommandName("3"), id3))
    )
  }

  test("test with all already completed") {
    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val r1  = Completed(CommandName("1"), id1)
    val r2  = Completed(CommandName("2"), id2)
    val r3  = Completed(CommandName("3"), id3)

    val c1 = new Completer(Set(r1, r2, r3), loggerFactory)

    val x = c1.waitComplete()

    val res = Await.result(x, 1.seconds)
    res shouldEqual OverallSuccess(
      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
    )
  }

  test("test with all already Error") {
    val id1 = Id("1")
    val id2 = Id("2")
    val r1  = Error(CommandName("1"), id1, "Error")
    val r2  = Error(CommandName("2"), id2, "Error")

    val c1 = new Completer(Set(r1, r2), loggerFactory)

    val x = c1.waitComplete()

    val res = Await.result(x, 1.seconds)
    res shouldEqual OverallFailure(Set(Error(CommandName("1"), id1, "Error"), Error(CommandName("2"), id2, "Error")))
  }

  test("test with one Invalid") {
    val id1 = Id("1")
    val id2 = Id("2")
    val r1  = Completed(CommandName("1"), id1)
    val r2  = Invalid(CommandName("2"), id2, OtherIssue("TEST"))

    val c1 = new Completer(Set(r1, r2), loggerFactory)

    val x = c1.waitComplete()

    val res = Await.result(x, 1.seconds)
    res shouldEqual OverallFailure(Set(r1, r2))
  }

  // This works because of use of Sets
  test("test easy case where updating same response more than once") {
    val id1 = Id("1")
    val id2 = Id("2")
    val r1  = Started(CommandName("1"), id1)
    val r2  = Started(CommandName("2"), id2)

    val c1 = new Completer(Set(r1, r2), loggerFactory)

    val x = c1.waitComplete()
    c1.update(Completed(CommandName("1"), id1))
    c1.update(Error(CommandName("1"), id1, "Error"))
    c1.update(Completed(CommandName("2"), id2))

    val res = Await.result(x, 1.seconds)
    res shouldEqual OverallSuccess(Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2)))
  }
}
