package csw.services.event.cli

import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}
import ujson.Js
import upickle.default.read

import scala.io.Source

class EventJsonTransformerTest extends FunSuite with Matchers with BeforeAndAfterEach {

  test("should be able to get entire event json when no paths are specified") {

    val jsonString        = Source.fromResource("get/input/event1.json").mkString
    val eventJson         = read[Js.Obj](jsonString)
    val expectedEventJson = read[Js.Obj](jsonString)

    val transformedEventJson = EventJsonTransformer.transform(eventJson, Nil)
    transformedEventJson shouldBe expectedEventJson
  }

  test("should be able to get top level non struct key from json") {

    val eventJson            = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val expectedEventJson    = read[Js.Obj](Source.fromResource("get/expected/top_level_non_struct_key.json").mkString)
    val paths                = List("epoch")
    val transformedEventJson = EventJsonTransformer.transform(eventJson, paths)
    transformedEventJson shouldBe expectedEventJson
  }

  test("should be able to get top level partial struct key from json") {

    val eventJson            = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val expectedEventJson    = read[Js.Obj](Source.fromResource("get/expected/top_level_partial_struct_key.json").mkString)
    val paths                = List("struct-1")
    val transformedEventJson = EventJsonTransformer.transform(eventJson, paths)
    transformedEventJson shouldBe expectedEventJson
  }

  test("should be able to get specified paths two levels deep in event in json format") {

    val eventJson            = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val expectedEventJson    = read[Js.Obj](Source.fromResource("get/expected/get_path_2_levels_deep.json").mkString)
    val paths                = List("struct-1/ra")
    val transformedEventJson = EventJsonTransformer.transform(eventJson, paths)
    transformedEventJson shouldBe expectedEventJson
  }

  test("should be able to get multiple specified paths in event in json format") {

    val eventJson            = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val expectedEventJson    = read[Js.Obj](Source.fromResource("get/expected/get_multiple_paths.json").mkString)
    val paths                = List("struct-1/ra", "epoch")
    val transformedEventJson = EventJsonTransformer.transform(eventJson, paths)
    transformedEventJson shouldBe expectedEventJson
  }

  test("should be able to get specified paths for multiple events in json format") {

    val event1Json         = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val event2Json         = read[Js.Obj](Source.fromResource("get/input/event2.json").mkString)
    val expectedEvent1Json = read[Js.Obj](Source.fromResource("get/expected/get_multiple_events1.json").mkString)
    val expectedEvent2Json = read[Js.Obj](Source.fromResource("get/expected/get_multiple_events2.json").mkString)

    val transformedEventJson1 = EventJsonTransformer.transform(event1Json, List("struct-1/ra"))
    val transformedEventJson2 = EventJsonTransformer.transform(event2Json, List("struct-2/struct-1/ra"))
    transformedEventJson1 shouldBe expectedEvent1Json
    transformedEventJson2 shouldBe expectedEvent2Json
  }
}
