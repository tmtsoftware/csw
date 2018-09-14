package csw.event.cli.utils

import csw.params.events.{ObserveEvent, SystemEvent}
import csw.params.core.formats.JsonSupport
import csw.event.cli.IterableExtensions.RichStringIterable
import csw.event.cli.args.Options
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

    onelineOutput shouldEqualContentsOf "oneline/inspect_entire_event.txt"
  }

  test("should be able to get entire event in oneline format") {

    val options       = Options(cmd = "get", eventsMap = Map(event1.eventKey → Set.empty))
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/get_entire_event.txt"
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
    onelineOutput shouldEqualContentsOf "oneline/get_multiple_paths.txt"
  }

  test("should be able to get partial struct paths from event in oneline format") {

    val options       = Options(cmd = "get", eventsMap = Map(event2.eventKey -> Set("struct-2")))
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event2))

    onelineOutput shouldEqualContentsOf "oneline/get_partial_struct_path.txt"
  }

  test("should be able to log timestamp in oneline format") {

    val options       = Options(cmd = "get", eventsMap = Map(event1.eventKey -> Set("epoch")), printTimestamp = true)
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/get_with_timestamp.txt"
  }

  test("should be able to log id per event in oneline format") {

    val options = Options(cmd = "get", eventsMap = Map(event1.eventKey -> Set("epoch")), printId = true)

    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/get_with_id.txt"
  }

  test("should be able to log units per event in oneline format") {

    val options       = Options(cmd = "get", eventsMap = Map(event1.eventKey -> Set("epoch")), printUnits = true)
    val onelineOutput = new EventOnelineTransformer(options).transform(List(event1))

    onelineOutput shouldEqualContentsOf "oneline/get_with_units.txt"
  }
}
