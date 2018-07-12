package csw.services.event.cli.utils

import csw.messages.events.{ObserveEvent, SystemEvent}
import csw.messages.params.formats.JsonSupport
import csw.services.event.cli.IterableExtensions.RichStringIterable
import csw.services.event.cli.args.Options
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json

import scala.io.Source

class EventOnelineTransformerTest extends FunSuite with Matchers {

  private val event1Str = Source.fromResource("seedData/event1.json").mkString
  private val event2Str = Source.fromResource("seedData/event2.json").mkString
  private val event1    = JsonSupport.readEvent[SystemEvent](Json.parse(event1Str))
  private val event2    = JsonSupport.readEvent[ObserveEvent](Json.parse(event2Str))

  test("should be able to inspect entire event in oneline format") {

    val options       = Options(cmd = "inspect", eventsMap = Map(event1.eventKey → Set.empty))
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/entire_event_inspect.txt"
  }

  test("should be able to get entire event in oneline format") {

    val options       = Options(cmd = "get", eventsMap = Map(event1.eventKey → Set.empty))
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/entire_event_get.txt"
  }

  test("should be able to get specified paths for event in oneline format") {

    val options       = Options(cmd = "get", eventsMap = Map(event1.eventKey → Set("epoch", "struct-1/dec")))
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/full_paths.txt"
  }

  test("should be able to get paths for multiple events in oneline format") {

    val options =
      Options(cmd = "get", eventsMap = Map(event1.eventKey -> Set("epoch"), event2.eventKey -> Set("struct-2/struct-1/ra")))

    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1, event2))
    onelineOutput shouldEqualContentsOf "oneline/multiple_events_get.txt"
  }

  test("should be able to get partial struct paths from event in oneline format") {

    val options       = Options(cmd = "get", eventsMap = Map(event2.eventKey -> Set("struct-2")))
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event2))

    onelineOutput shouldEqualContentsOf "oneline/partial_struct_path.txt"
  }

  test("should be able to log timestamp in oneline format") {

    val options       = Options(cmd = "get", eventsMap = Map(event1.eventKey -> Set("epoch")), printTimestamp = true)
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/with_timestamp.txt"
  }

  test("should be able to log id per event in oneline format") {

    val options = Options(cmd = "get", eventsMap = Map(event1.eventKey -> Set("epoch")), printId = true)

    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/with_id.txt"
  }

  test("should be able to log units per event in oneline format") {

    val options       = Options(cmd = "get", eventsMap = Map(event1.eventKey -> Set("epoch")), printUnits = true)
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/with_units.txt"
  }
}
