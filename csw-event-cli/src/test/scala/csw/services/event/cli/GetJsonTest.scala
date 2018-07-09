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

    commandLineRunner.get(argsParser.parse(Seq("get", "-e", s"${event1.eventKey}", "-o", "json")).get).await
    JsonSupport.readEvent[SystemEvent](Json.parse(logBuffer.head)) shouldBe event1

    logBuffer.clear()

    commandLineRunner.get(argsParser.parse(Seq("get", "-e", s"${event1.eventKey},${event2.eventKey}", "--out", "json")).get).await
    val events = logBuffer.map(event ⇒ JsonSupport.readEvent[Event](Json.parse(event))).toSet
    events shouldEqual Set(event1, event2)
  }

}
