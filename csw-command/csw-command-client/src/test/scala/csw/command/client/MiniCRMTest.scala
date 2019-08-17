package csw.command.client

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import csw.command.client.internal.MiniCRM
import csw.command.client.internal.MiniCRM.{Responses, Starters, Waiters}
import csw.command.client.internal.MiniCRM.MiniCRMMessage.{
  AddResponse,
  AddStarted,
  GetResponses,
  GetStarters,
  GetWaiters,
  Query,
  QueryFinal
}
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.{CommandNotAvailable, Completed, QueryResponse, Started, SubmitResponse}
import csw.params.core.models.Id
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration._

class MiniCRMTest extends FunSuite with Matchers with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  // This test just adds responses to make sure they are added properly to the response list
  test("add responses, check inclusion") {
    val crm           = testKit.spawn(MiniCRM.make())
    val responseProbe = testKit.createTestProbe[Responses]()

    val id = Id("1")

    val r1 = Completed(CommandName("1"), id)
    crm ! AddResponse(r1)
    crm ! GetResponses(responseProbe.ref)

    val responses1 = responseProbe.expectMessageType[Responses]
    responses1.size shouldBe 1
    responses1.head shouldBe r1

    val r2 = Completed(CommandName("2"), id)
    crm ! AddResponse(r2)
    crm ! GetResponses(responseProbe.ref)

    // Responses are appended
    val responses2 = responseProbe.expectMessageType[Responses]
    responses2.size shouldBe 2
    responses2.head shouldBe r1
    responses2(1) shouldBe r2
  }

  // This test makes sure waiters are added properly
  test("add waiters, check inclusion") {
    val crm             = testKit.spawn(MiniCRM.make())
    val responseProbe   = testKit.createTestProbe[SubmitResponse]()
    val waiterListProbe = testKit.createTestProbe[Waiters]

    val id1 = Id("1")
    crm ! QueryFinal(id1, responseProbe.ref)
    responseProbe.expectNoMessage(100.millis)

    crm ! GetWaiters(waiterListProbe.ref)
    val waiters1 = waiterListProbe.expectMessageType[Waiters]
    waiters1.size shouldBe 1
    waiters1 shouldBe List((id1, responseProbe.ref))

    val id2 = Id("2")
    crm ! QueryFinal(id2, responseProbe.ref)
    responseProbe.expectNoMessage(100.millis)

    crm ! GetWaiters(waiterListProbe.ref)
    val waiters2 = waiterListProbe.expectMessageType[Waiters]
    waiters2.size shouldBe 2
    waiters2 shouldBe List((id1, responseProbe.ref), (id2, responseProbe.ref))
  }

  // This test adds a response and then does a queryFinal to make sure that
  // 1. The queryFinal completes correctly
  // 2. The response remains
  // 3. The waiters are removed
  test("add response first then queryFinal") {
    val crm                  = testKit.spawn(MiniCRM.make())
    val commandResponseProbe = testKit.createTestProbe[SubmitResponse]()
    val responseProbe        = testKit.createTestProbe[Responses]()
    val waiterListProbe      = testKit.createTestProbe[Waiters]()

    val id1 = Id("1")

    val r1 = Completed(CommandName("1"), id1)
    crm ! AddResponse(r1)
    commandResponseProbe.expectNoMessage(100.millis)

    crm ! GetResponses(responseProbe.ref)
    val responses1 = responseProbe.expectMessageType[Responses]
    responses1.size shouldBe 1

    crm ! GetWaiters(waiterListProbe.ref)
    val waiters1 = waiterListProbe.expectMessageType[Waiters]
    waiters1.isEmpty shouldBe true

    // Here the message is delivered to queryFinal
    crm ! QueryFinal(id1, commandResponseProbe.ref)
    commandResponseProbe.expectMessage(r1)

    // Check that the response remains
    crm ! GetResponses(responseProbe.ref)
    val responses2 = responseProbe.expectMessageType[Responses]
    responses2.size shouldBe 1

    // Check that the waiter is removed
    crm ! GetWaiters(waiterListProbe.ref)
    val waiters2 = waiterListProbe.expectMessageType[Waiters]
    waiters2.isEmpty shouldBe true
  }

  // This scenario has the queryFinal first so there is a waiter, then add the response
  // 1. queryFinal should complete
  // 2. After waiter list should be empty
  // 3. Response remains
  test("queryFinal first, then add response") {
    val crm                  = testKit.spawn(MiniCRM.make())
    val commandResponseProbe = testKit.createTestProbe[SubmitResponse]()
    val responseProbe        = testKit.createTestProbe[Responses]()
    val waiterListProbe      = testKit.createTestProbe[Waiters]()

    val id1 = Id("1")

    crm ! QueryFinal(id1, commandResponseProbe.ref)
    commandResponseProbe.expectNoMessage(100.milli)

    crm ! GetResponses(responseProbe.ref)
    val responses1 = responseProbe.expectMessageType[Responses]
    responses1.isEmpty shouldBe true

    crm ! GetWaiters(waiterListProbe.ref)
    val waiters1 = waiterListProbe.expectMessageType[Waiters]
    waiters1.size shouldBe 1

    val r1 = Completed(CommandName("1"), id1)
    crm ! AddResponse(r1)
    commandResponseProbe.expectMessage(r1)

    crm ! GetResponses(responseProbe.ref)
    val responses2 = responseProbe.expectMessageType[Responses]
    responses2.size shouldBe 1

    crm ! GetWaiters(waiterListProbe.ref)
    val waiters2 = waiterListProbe.expectMessageType[Waiters]
    waiters2.isEmpty shouldBe true
  }

  // Verify that it is possible to have two waiters for the same id and both get updated
  // It is desired that if the same client does queryFinal twice, both get an answer
  test("queryFinal twice, then add response -- should get both updates") {
    val crm                   = testKit.spawn(MiniCRM.make())
    val commandResponseProbe1 = testKit.createTestProbe[SubmitResponse]()
    val commandResponseProbe2 = testKit.createTestProbe[SubmitResponse]()
    val responseProbe         = testKit.createTestProbe[Responses]()
    val waiterListProbe       = testKit.createTestProbe[Waiters]()

    val id1 = Id("1")

    crm ! QueryFinal(id1, commandResponseProbe1.ref)
    commandResponseProbe1.expectNoMessage(100.milli)

    crm ! QueryFinal(id1, commandResponseProbe2.ref)
    commandResponseProbe2.expectNoMessage(100.milli)

    crm ! GetWaiters(waiterListProbe.ref)
    val waiters1 = waiterListProbe.expectMessageType[Waiters]
    waiters1.size shouldBe 2

    val r1 = Completed(CommandName("1"), id1)
    crm ! AddResponse(r1)

    // Both get updated
    commandResponseProbe1.expectMessage(r1)
    commandResponseProbe2.expectMessage(r1)

    crm ! GetResponses(responseProbe.ref)
    val responses1 = responseProbe.expectMessageType[Responses]
    responses1.size shouldBe 1

    crm ! GetWaiters(waiterListProbe.ref)
    val waiters2 = waiterListProbe.expectMessageType[Waiters]
    waiters2.isEmpty shouldBe true
  }

  // Verify that it is possible to have two waiters for the same id and both get updated
  // In this case the same client does queryFinal twice, both get an answer
  test("queryFinal twice, but receiver is the same actor!") {
    val crm                   = testKit.spawn(MiniCRM.make())
    val commandResponseProbe1 = testKit.createTestProbe[SubmitResponse]()
    val responseProbe         = testKit.createTestProbe[Responses]()
    val waiterListProbe       = testKit.createTestProbe[Waiters]()

    val id1 = Id("1")

    crm ! QueryFinal(id1, commandResponseProbe1.ref)
    commandResponseProbe1.expectNoMessage(100.milli)

    crm ! QueryFinal(id1, commandResponseProbe1.ref)
    commandResponseProbe1.expectNoMessage(100.milli)

    crm ! GetWaiters(waiterListProbe.ref)
    val waiters1 = waiterListProbe.expectMessageType[Waiters]
    waiters1.size shouldBe 2

    val r1 = Completed(CommandName("1"), id1)
    crm ! AddResponse(r1)

    // Both get updated
    commandResponseProbe1.expectMessage(r1)
    commandResponseProbe1.expectMessage(r1)

    crm ! GetResponses(responseProbe.ref)
    val responses1 = responseProbe.expectMessageType[Responses]
    responses1.size shouldBe 1
  }

  // This scenario checks that same source doing queryFinal after completed still works
  // 1. queryFinal should complete
  // 2. second queryFinal should still work and return the same
  test("queryFinal waiting for final, then check queryFinal again") {
    val crm                  = testKit.spawn(MiniCRM.make())
    val commandResponseProbe = testKit.createTestProbe[SubmitResponse]()

    val id1 = Id("1")

    crm ! QueryFinal(id1, commandResponseProbe.ref)
    commandResponseProbe.expectNoMessage(100.milli)

    val r1 = Completed(CommandName("1"), id1)
    crm ! AddResponse(r1)
    commandResponseProbe.expectMessage(r1)

    // Second should still work
    crm ! QueryFinal(id1, commandResponseProbe.ref)
    commandResponseProbe.expectMessage(r1)
  }

  // This scenario checks that same source doing query after completed still works
  // 1. Start the command
  // 2. Query should show started
  // 3, Complete command and queryFinal should complete
  // 4. Second query should still work and return the same complete
  test("query after starting, wait for final, then check query again") {
    val crm                  = testKit.spawn(MiniCRM.make())
    val commandResponseProbe = testKit.createTestProbe[QueryResponse]()

    val id1 = Id("1")
    val r0 = Started(CommandName("1"), id1)
    crm ! AddStarted(r0)

    crm ! Query(id1, commandResponseProbe.ref)
    commandResponseProbe.expectMessage(r0)

    val r1 = Completed(CommandName("1"), id1)
    crm ! AddResponse(r1)

    crm ! QueryFinal(id1, commandResponseProbe.ref)

    // Second should still work
    crm ! Query(id1, commandResponseProbe.ref)
    commandResponseProbe.expectMessage(r1)

    // Make sure miniCRM isn't removing, check again
    crm ! Query(id1, commandResponseProbe.ref)
    commandResponseProbe.expectMessage(r1)
  }

  // This test adds responses to make sure SizedList is removing oldest and restricting size
  test("add responses, check max size") {
    val crm           = testKit.spawn(MiniCRM.make(2, 2))
    val responseProbe = testKit.createTestProbe[Responses]()
    val id            = Id()

    val r1 = Completed(CommandName("1"), id)
    crm ! AddResponse(r1)
    crm ! GetResponses(responseProbe.ref)

    val responses1 = responseProbe.expectMessageType[Responses]
    responses1.size shouldBe 1
    responses1.head.commandName shouldBe CommandName("1")

    val r2 = Completed(CommandName("2"), id)
    crm ! AddResponse(r2)
    crm ! GetResponses(responseProbe.ref)

    val responses2 = responseProbe.expectMessageType[Responses]
    responses2.size shouldBe 2
    responses2.head.commandName shouldBe CommandName("1")
    responses2(1).commandName shouldBe CommandName("2")

    val r3 = Completed(CommandName("3"), id)
    crm ! AddResponse(r3)
    crm ! GetResponses(responseProbe.ref)

    val responses3 = responseProbe.expectMessageType[Responses]
    responses3.size shouldBe 2
    responses3.head.commandName shouldBe CommandName("2")
    responses3(1).commandName shouldBe CommandName("3")
  }

  // This test adds waiters through queryFinal and then sends a response to make sure waiters are removed, but only
  // the waiters that are supposed to be removed
  test("add responses, verify waiter removal is proper") {
    val crm         = testKit.spawn(MiniCRM.make())
    val waiterProbe = testKit.createTestProbe[Waiters]()
    val qfProbe1    = testKit.createTestProbe[SubmitResponse]()
    val qfProbe2    = testKit.createTestProbe[SubmitResponse]()
    val qfProbe3    = testKit.createTestProbe[SubmitResponse]()

    val id1 = Id()
    val id2 = Id()

    // Add two waiters for id1 and 1 for id2
    crm ! QueryFinal(id1, qfProbe1.ref)
    crm ! QueryFinal(id1, qfProbe2.ref)
    crm ! QueryFinal(id2, qfProbe3.ref)
    qfProbe1.expectNoMessage(100.milli)
    qfProbe2.expectNoMessage(100.milli)
    qfProbe3.expectNoMessage(100.milli)

    // Check that the list is right before response
    crm ! GetWaiters(waiterProbe.ref)
    val waitersBefore = waiterProbe.expectMessageType[Waiters]
    waitersBefore.size shouldBe 3

    // Send one update for id1, should get two completeds
    val r1 = Completed(CommandName("1"), id1)
    crm ! AddResponse(r1)
    qfProbe1.expectMessage(r1)
    qfProbe2.expectMessage(r1)
    qfProbe3.expectNoMessage(100.milli)

    // Shoulds still be the waiter for Id2
    crm ! GetWaiters(waiterProbe.ref)
    val waitersAfter = waiterProbe.expectMessageType[Waiters]
    waitersAfter.size shouldBe 1

    // Now update id2
    val r2 = Completed(CommandName("2"), id2)
    crm ! AddResponse(r2)
    // Nothing on id1 probes
    qfProbe1.expectNoMessage(100.milli)
    qfProbe2.expectNoMessage(100.milli)
    qfProbe3.expectMessage(r2)

    // Waiter list should now be empty
    crm ! GetWaiters(waiterProbe.ref)
    val waitersAfter2 = waiterProbe.expectMessageType[Waiters]
    waitersAfter2.isEmpty shouldBe true
  }

  // Test the query algo. This test just adds responses and some starters to see if the correct response is returned
  test("add responses and starters.  Check query.") {
    val crm           = testKit.spawn(MiniCRM.make())
    val responseProbe = testKit.createTestProbe[Responses]()
    val startersProbe = testKit.createTestProbe[Starters]()
    val queryProbe    = testKit.createTestProbe[QueryResponse]()

    val id1 = Id("1")
    val id2 = Id("2")
    val id3 = Id("3")
    val id4 = Id("4")

    val r1 = Completed(CommandName("1"), id1)
    crm ! AddResponse(r1)
    val r2 = Completed(CommandName("2"), id2)
    crm ! AddResponse(r2)

    crm ! GetResponses(responseProbe.ref)

    // Responses are appended
    val responses2 = responseProbe.expectMessageType[Responses]
    responses2.size shouldBe 2
    responses2.head shouldBe r1
    responses2(1) shouldBe r2

    val r3 = Started(CommandName("1"), id1)
    val r4 = Started(CommandName("2"), id2)
    val r5 = Started(CommandName("3"), id3)

    crm ! AddStarted(r3)
    crm ! AddStarted(r4)
    crm ! AddStarted(r5)

    crm ! GetStarters(startersProbe.ref)

    val starters = startersProbe.expectMessageType[Starters]
    starters.size shouldBe 3

    crm ! Query(id1, queryProbe.ref)
    queryProbe.expectMessage(r1)
    crm ! Query(id2, queryProbe.ref)
    queryProbe.expectMessage(r2)
    crm ! Query(id3, queryProbe.ref)
    queryProbe.expectMessage(r5)
    crm ! Query(id4, queryProbe.ref)
    queryProbe.expectMessage(CommandNotAvailable(CommandName("CommandNotAvailable"), id4))
  }
}
