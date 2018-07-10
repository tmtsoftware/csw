package csw.services.event.cli

import csw.messages.events._
import csw.messages.params.formats.JsonSupport
import csw.services.event.helpers.TestFutureExt.RichFuture
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json

// DEOPSCSW-431: [Event Cli] Get command
class GetJsonTest extends FunSuite with Matchers with SeedData {

  import cliWiring._

  test("should able to get entire event/events in json format") {

    val options1 = Options(
      cmd = "get",
      out = "json",
      eventKeys = Seq(event1.eventKey)
    )

    val options2 = Options(
      cmd = "get",
      out = "json",
      eventKeys = Seq(event1.eventKey, event2.eventKey)
    )

    commandLineRunner.get(options1).await
    JsonSupport.readEvent[SystemEvent](Json.parse(logBuffer.head)) shouldBe event1

    logBuffer.clear()

    commandLineRunner.get(options2).await
    val events = logBuffer.map(event ⇒ JsonSupport.readEvent[Event](Json.parse(event))).toSet
    events shouldEqual Set(event1, event2)
  }

}
