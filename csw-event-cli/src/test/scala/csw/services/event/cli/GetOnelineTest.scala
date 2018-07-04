package csw.services.event.cli

import csw.services.event.helpers.TestFutureExt.RichFuture
import org.scalatest.{FunSuite, Matchers}

class GetOnelineTest extends FunSuite with Matchers with SeedData {
  import cliWiring._

  test("should able to get entire event/events in oneline format") {

    val options = Options(eventsMap = Map(event1.eventKey -> Set("epoch"), event2.eventKey -> Set("struct-2/struct-1/ra")))

    commandLineRunner.get(options).await

    val expectedLogs =
      List("wfos.prog.cloudcover.move", "epoch = [1950]", "wfos.prog.filter.stop", "struct-2/struct-1/ra = [\"12:13:14.1\"]")

    logBuffer shouldEqual expectedLogs
  }

  test("should be able to log timestamp in oneline format") {

    val options = Options(
      eventsMap = Map(event1.eventKey -> Set("epoch"), event2.eventKey -> Set("struct-2/struct-1/ra")),
      printTimestamp = true
    )

    commandLineRunner.get(options).await

    val expectedLogs = List(
      s"${event1.eventTime.time} ${event1.eventKey.key}",
      "epoch = [1950]",
      s"${event2.eventTime.time} ${event2.eventKey.key}",
      "struct-2/struct-1/ra = [\"12:13:14.1\"]"
    )

    logBuffer shouldEqual expectedLogs
  }
  test("should be able to log id per event in oneline format") {
    val options =
      Options(
        eventsMap = Map(event1.eventKey -> Set("epoch"), event2.eventKey -> Set("struct-2/struct-1/ra")),
        printId = true
      )
    commandLineRunner.get(options).await

    val expectedLogs = List(
      s"${event1.eventId.id} ${event1.eventKey.key}",
      "epoch = [1950]",
      s"${event2.eventId.id} ${event2.eventKey.key}",
      "struct-2/struct-1/ra = [\"12:13:14.1\"]"
    )

    logBuffer shouldEqual expectedLogs
  }
}
