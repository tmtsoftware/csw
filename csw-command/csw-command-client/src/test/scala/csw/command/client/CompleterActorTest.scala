//package csw.command.client
//
//import akka.actor.Scheduler
//import akka.actor.testkit.typed.scaladsl.ActorTestKit
//import akka.actor.typed.scaladsl.AskPattern._
//import akka.util.Timeout
//import csw.command.api.Completer.{OverallFailure, OverallResponse, OverallSuccess}
//import csw.params.commands.CommandIssue.OtherIssue
//import csw.params.commands.CommandName
//import csw.params.commands.CommandResponse.{Completed, Error, Invalid, Started}
//import csw.params.core.models.Id
//import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
//
//import scala.concurrent.duration._
//import scala.concurrent.{Await, Future}
//
//class CompleterActorTest extends FunSuite with Matchers with BeforeAndAfterAll {
//
//  val testKit = ActorTestKit()
//
//  private implicit val timeout: Timeout     = 3.seconds
//  private implicit val scheduler: Scheduler = testKit.scheduler
//
//  override def afterAll(): Unit = testKit.shutdownTestKit()
//
//  test("test case with all started then update") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val id3 = Id("3")
//    val r1  = Started(CommandName("1"), id1)
//    val r2  = Started(CommandName("2"), id2)
//    val r3  = Started(CommandName("3"), id3)
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val c1            = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//
//    c1 ! Update(Completed(CommandName("1"), id1))
//    c1 ! Update(Completed(CommandName("2"), id2))
//    c1 ! Update(Completed(CommandName("3"), id3))
//
//    c1 ! WaitComplete(responseProbe.ref)
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//    res shouldEqual OverallSuccess(
//      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
//    )
//  }
//
//  test("test case with all started then update using ask") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val id3 = Id("3")
//    val r1  = Started(CommandName("1"), id1)
//    val r2  = Started(CommandName("2"), id2)
//    val r3  = Started(CommandName("3"), id3)
//
//    val c1 = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//
//    c1 ! Update(Completed(CommandName("1"), id1))
//    c1 ! Update(Completed(CommandName("2"), id2))
//    c1 ! Update(Completed(CommandName("3"), id3))
//
//    val f: Future[OverallResponse] = c1 ? (ref => WaitComplete(ref))
//    val res                        = Await.result(f, 5.seconds)
//
//    res shouldEqual OverallSuccess(
//      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
//    )
//
//  }
//
//  test("test case with all started but wait first") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val id3 = Id("3")
//    val r1  = Started(CommandName("1"), id1)
//    val r2  = Started(CommandName("2"), id2)
//    val r3  = Started(CommandName("3"), id3)
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val c1            = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//
//    c1 ! WaitComplete(responseProbe.ref)
//
//    c1 ! Update(Completed(CommandName("1"), id1))
//    c1 ! Update(Completed(CommandName("2"), id2))
//    c1 ! Update(Completed(CommandName("3"), id3))
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//
//    /*
//        val f:Future[OverallResponse] = c1.ask(ref => WaitComplete(ref))
//        val res = Await.result(f, 5.seconds)
//        println("Res: " + res)
//     */
//    res shouldEqual OverallSuccess(
//      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
//    )
//
//  }
//
//  test("test case with all started, first returns error") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val id3 = Id("3")
//    val r1  = Started(CommandName("1"), id1)
//    val r2  = Started(CommandName("2"), id2)
//    val r3  = Started(CommandName("3"), id3)
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val expected = OverallFailure(
//      Set(Error(CommandName("1"), id1, "ERROR"), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
//    )
//
//    val c1 = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//
//    c1 ! Update(Error(CommandName("1"), id1, "ERROR"))
//    c1 ! Update(Completed(CommandName("2"), id2))
//    c1 ! Update(Completed(CommandName("3"), id3))
//    c1 ! WaitComplete(responseProbe.ref)
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//    res shouldEqual expected
//    /*
//        val f:Future[OverallResponse] = c1.ask(ref => WaitComplete(ref))
//        val res = Await.result(f, 5.seconds)
//        println("Res: " + res)
//     */
//    val c2 = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//
//    c2 ! WaitComplete(responseProbe.ref)
//
//    c2 ! Update(Error(CommandName("1"), id1, "ERROR"))
//    c2 ! Update(Completed(CommandName("2"), id2))
//    c2 ! Update(Completed(CommandName("3"), id3))
//
//    val res2 = responseProbe.expectMessageType[OverallResponse]
//    res2 shouldEqual expected
//  }
//
//  test("test case with 2 completed and 1 started") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val id3 = Id("3")
//    val r1  = Completed(CommandName("1"), id1)
//    val r2  = Started(CommandName("2"), id2)
//    val r3  = Completed(CommandName("3"), id3)
//
//    val expected =
//      OverallSuccess(Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3)))
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val c1            = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//
//    c1 ! Update(Completed(CommandName("2"), id2))
//    c1 ! WaitComplete(responseProbe.ref)
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//    res shouldEqual expected
//
//    val c2 = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//
//    c2 ! WaitComplete(responseProbe.ref)
//    c2 ! Update(Completed(CommandName("2"), id2))
//
//    val res2 = responseProbe.expectMessageType[OverallResponse]
//    res2 shouldEqual expected
//  }
//
//  test("test case with update not in completer") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val id3 = Id("3")
//    val r1  = Started(CommandName("1"), id1)
//    val r3  = Started(CommandName("3"), id3)
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val c1            = testKit.spawn(CompleterActor(Set(r1, r3)))
//
//    c1 ! Update(Completed(CommandName("1"), id1))
//    c1 ! Update(Completed(CommandName("2"), id2)) // This one is not in completer
//    c1 ! Update(Completed(CommandName("3"), id3))
//    c1 ! WaitComplete(responseProbe.ref)
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//    res shouldEqual OverallSuccess(Set(Completed(CommandName("1"), id1), Completed(CommandName("3"), id3)))
//  }
//
//  test("test case with all started but makes an error") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val id3 = Id("3")
//    val r1  = Started(CommandName("1"), id1)
//    val r2  = Started(CommandName("2"), id2)
//    val r3  = Started(CommandName("3"), id3)
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val c1            = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//
//    c1 ! Update(Completed(CommandName("1"), id1))
//    c1 ! Update(Error(CommandName("2"), id2, "Error"))
//    c1 ! Update(Completed(CommandName("3"), id3))
//    c1 ! WaitComplete(responseProbe.ref)
//
//    val expected = OverallFailure(
//      Set(Completed(CommandName("1"), id1), Error(CommandName("2"), id2, "Error"), Completed(CommandName("3"), id3))
//    )
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//    res shouldEqual expected
//
//    val c2 = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//
//    // Now with wait first
//    c2 ! WaitComplete(responseProbe.ref)
//
//    c2 ! Update(Completed(CommandName("1"), id1))
//    c2 ! Update(Error(CommandName("2"), id2, "Error"))
//    c2 ! Update(Completed(CommandName("3"), id3))
//
//    val res2 = responseProbe.expectMessageType[OverallResponse]
//    res2 shouldEqual expected
//  }
//
//  test("test with all already completed") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val id3 = Id("3")
//    val r1  = Completed(CommandName("1"), id1)
//    val r2  = Completed(CommandName("2"), id2)
//    val r3  = Completed(CommandName("3"), id3)
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val c1            = testKit.spawn(CompleterActor(Set(r1, r2, r3)))
//    c1 ! WaitComplete(responseProbe.ref)
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//    res shouldEqual OverallSuccess(
//      Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2), Completed(CommandName("3"), id3))
//    )
//  }
//
//  test("test with all already Error") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val r1  = Error(CommandName("1"), id1, "Error")
//    val r2  = Error(CommandName("2"), id2, "Error")
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val c1            = testKit.spawn(CompleterActor(Set(r1, r2)))
//    c1 ! WaitComplete(responseProbe.ref)
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//    res shouldEqual OverallFailure(Set(Error(CommandName("1"), id1, "Error"), Error(CommandName("2"), id2, "Error")))
//  }
//
//  test("test with one Invalid") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val r1  = Started(CommandName("1"), id1)
//    val r2  = Invalid(CommandName("2"), id2, OtherIssue("TEST"))
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val c1            = testKit.spawn(CompleterActor(Set(r1, r2)))
//    c1 ! WaitComplete(responseProbe.ref)
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//    res shouldEqual OverallFailure(Set(Started(CommandName("1"), id1), Invalid(CommandName("2"), id2, OtherIssue("TEST"))))
//  }
//
//  // This works because of use of Sets
//  test("test case where updating same response more than once") {
//    val id1 = Id("1")
//    val id2 = Id("2")
//    val r1  = Started(CommandName("1"), id1)
//    val r2  = Started(CommandName("2"), id2)
//
//    val responseProbe = testKit.createTestProbe[OverallResponse]()
//    val c1            = testKit.spawn(CompleterActor(Set(r1, r2)))
//
//    c1 ! Update(Completed(CommandName("1"), id1))
//    c1 ! Update(Error(CommandName("1"), id1, "Error"))
//    c1 ! Update(Completed(CommandName("2"), id2))
//    c1 ! WaitComplete(responseProbe.ref)
//
//    val res = responseProbe.expectMessageType[OverallResponse]
//    res shouldEqual OverallSuccess(Set(Completed(CommandName("1"), id1), Completed(CommandName("2"), id2)))
//  }
//}
