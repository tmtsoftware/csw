package csw.event.cli.utils

import csw.event.cli.extenstion.RichStringExtentions.JsonDecodeRichString
import csw.params.core.formats.ParamCodecs.*
import csw.params.events.Event
import org.scalatest.BeforeAndAfterEach

import scala.io.Source
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
class EventTransformerTest extends AnyFunSuite with Matchers with BeforeAndAfterEach {
  private val event1 = Source.fromResource("seedData/event1.json").mkString.parse[Event]

  test("should be able to get entire event when no paths are specified") {
    val transformedEvent = EventTransformer.transform(event1, Nil)
    transformedEvent shouldBe event1
  }

  test("should be able to get top level non struct key from json") {
    val expectedEvent    = Source.fromResource("json/top_level_non_struct_key.json").mkString.parse[Event]
    val paths            = List("epoch")
    val transformedEvent = EventTransformer.transform(event1, paths)
    transformedEvent shouldBe expectedEvent
  }

  test("should be able to get specified paths for multiple events in json format") {

    val event1         = Source.fromResource("seedData/event1.json").mkString.parse[Event]
    val event2         = Source.fromResource("seedData/event2.json").mkString.parse[Event]
    val expectedEvent1 = Source.fromResource("json/get_multiple_events1.json").mkString.parse[Event]
    val expectedEvent2 = Source.fromResource("json/get_multiple_events2.json").mkString.parse[Event]

    val transformedEvent1 = EventTransformer.transform(event1, List("ra"))
    val transformedEvent2 = EventTransformer.transform(event2, List("ra"))
    transformedEvent1 shouldBe expectedEvent1
    transformedEvent2 shouldBe expectedEvent2
  }

}
