package csw.services.event.cli

import csw.services.event.cli.BufferExtensions.RichBuffer
import csw.services.event.helpers.TestFutureExt.RichFuture
import org.scalatest.{FunSuite, Matchers}

class GetOnelineTest extends FunSuite with Matchers with SeedData {
  import cliWiring._

  test("should able to get entire event/events in oneline format") {

    val options =
      Options(cmd = "get", eventsMap = Map(event1.eventKey -> Set("epoch"), event2.eventKey -> Set("struct-2/struct-1/ra")))
    commandLineRunner.get(options).await

    logBuffer shouldEqualContentsOf "get/oneline/expected/entire_event.txt"
  }

  test("should able to get partial struct paths from event in oneline format") {

    val options =
      Options(cmd = "get", eventsMap = Map(event2.eventKey -> Set("struct-2")))

    commandLineRunner.get(options).await

    logBuffer shouldEqualContentsOf "get/oneline/expected/partial_struct_path.txt"
  }

  test("should be able to log timestamp in oneline format") {

    val options = Options(
      cmd = "get",
      eventsMap = Map(event1.eventKey -> Set("epoch"), event2.eventKey -> Set("struct-2/struct-1/ra")),
      printTimestamp = true
    )

    commandLineRunner.get(options).await

    logBuffer shouldEqualContentsOf "get/oneline/expected/with_timestamp.txt"
  }

  test("should be able to log id per event in oneline format") {
    val options =
      Options(
        cmd = "get",
        eventsMap = Map(event1.eventKey -> Set("epoch"), event2.eventKey -> Set("struct-2/struct-1/ra")),
        printId = true
      )
    commandLineRunner.get(options).await

    logBuffer shouldEqualContentsOf "get/oneline/expected/with_id.txt"
  }
}
