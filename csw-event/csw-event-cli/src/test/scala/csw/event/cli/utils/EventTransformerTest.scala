package csw.event.cli.utils

import csw.event.cli.extenstion.RichStringExtentions.JsonDecodeRichString
import csw.params.core.formats.ParamCodecs._
import csw.params.events.Event
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

import scala.io.Source
class EventTransformerTest extends FunSuite with Matchers with BeforeAndAfterEach {
  private val event1 = Source.fromResource("seedData/event1.json").mkString.parse[Event]

  test("should be able to get entire event when no paths are specified") {
    val transformedEvent = EventTransformer.transform(event1, Nil)
    transformedEvent shouldBe event1
  }

  test("should be able to get top level non struct key from json") {
    val expectedEvent = Source.fromResource("json/top_level_non_struct_key.json").mkString.parse[Event]
    val paths = List("epoch")
    val transformedEvent = EventTransformer.transform(event1, paths)
    transformedEvent shouldBe expectedEvent
  }

  test("should be able to get top level partial struct key from json") {

    val expectedEvent = Source.fromResource("json/top_level_partial_struct_key.json").mkString.parse[Event]
    val paths = List("struct-1")
    val transformedEvent = EventTransformer.transform(event1, paths)
    transformedEvent shouldBe expectedEvent
  }

  test("should be able to get specified paths two levels deep in event in json format") {

    val expectedEvent = Source.fromResource("json/get_path_2_levels_deep.json").mkString.parse[Event]
    val paths = List("struct-1/ra")
    val transformedEvent = EventTransformer.transform(event1, paths)
    transformedEvent shouldBe expectedEvent
  }

  test("should be able to get multiple specified paths in event in json format") {

    val expectedEvent = Source.fromResource("json/get_multiple_paths.json").mkString.parse[Event]
    val paths = List("struct-1/ra", "epoch")
    val transformedEvent = EventTransformer.transform(event1, paths)
    transformedEvent shouldBe expectedEvent
  }

  test("should be able to get specified paths for multiple events in json format") {

    val event1 = Source.fromResource("seedData/event1.json").mkString.parse[Event]
    val event2 = Source.fromResource("seedData/event2.json").mkString.parse[Event]
    val expectedEvent1 = Source.fromResource("json/get_multiple_events1.json").mkString.parse[Event]
    val expectedEvent2 = Source.fromResource("json/get_multiple_events2.json").mkString.parse[Event]

    val transformedEvent1 = EventTransformer.transform(event1, List("struct-1/ra"))
    val transformedEvent2 = EventTransformer.transform(event2, List("struct-2/struct-1/ra"))
    transformedEvent1 shouldBe expectedEvent1
    transformedEvent2 shouldBe expectedEvent2
  }

  test("should be able to get full struct if both partial struct path and full path are given") {

    val event1 = Source.fromResource("seedData/event1.json").mkString.parse[Event]
    val expectedEvent1 = Source.fromResource("json/top_level_partial_struct_key.json").mkString.parse[Event]

    val transformedEvent1 = EventTransformer.transform(event1, List("struct-1/ra", "struct-1"))
    transformedEvent1 shouldBe expectedEvent1
  }
}
