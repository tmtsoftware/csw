package csw.services.event.cli

import org.scalatest.{FunSuite, Matchers}
import ujson.Js
import upickle.default.read

import scala.io.Source

class EventJsonTransformerTest extends FunSuite with Matchers {

  test("should be able to get top level key non struct key from json") {

    val eventJson         = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val expectedEventJson = read[Js.Obj](Source.fromResource("get/expected/top_level_non_struct_key.json").mkString)
    val paths             = List(List("epoch"))
    EventJsonTransformer.transformInPlace(eventJson, paths)
    eventJson shouldBe expectedEventJson
  }

  test("should be able to get top level partial struct key from json") {

    val eventJson         = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val expectedEventJson = read[Js.Obj](Source.fromResource("get/expected/top_level_partial_struct_key.json").mkString)
    val paths             = List(List("struct-1"))
    EventJsonTransformer.transformInPlace(eventJson, paths)
    eventJson shouldBe expectedEventJson
  }

  test("should be able to get specified paths two levels deep in event in json format") {

    val eventJson         = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val expectedEventJson = read[Js.Obj](Source.fromResource("get/expected/get_path_2_levels_deep.json").mkString)
    val paths             = List(List("struct-1", "ra"))
    EventJsonTransformer.transformInPlace(eventJson, paths)
    expectedEventJson shouldBe eventJson
  }

  test("should be able to get multiple specified paths in event in json format") {

    val eventJson         = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val expectedEventJson = read[Js.Obj](Source.fromResource("get/expected/get_multiple_paths.json").mkString)
    val paths             = List(List("struct-1", "ra"), List("epoch"))
    EventJsonTransformer.transformInPlace(eventJson, paths)
    expectedEventJson shouldBe eventJson
  }

  test("should be able to get specified paths for multiple events in json format") {

    val event1Json         = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val event2Json         = read[Js.Obj](Source.fromResource("get/input/event2.json").mkString)
    val expectedEvent1Json = read[Js.Obj](Source.fromResource("get/expected/get_multiple_events1.json").mkString)
    val expectedEvent2Json = read[Js.Obj](Source.fromResource("get/expected/get_multiple_events2.json").mkString)

    val paths = List(List("struct-1", "ra"), List("struct-2", "struct-1", "ra"))
    EventJsonTransformer.transformInPlace(event1Json, paths)
    EventJsonTransformer.transformInPlace(event2Json, paths)
    event1Json shouldBe expectedEvent1Json
    event2Json shouldBe expectedEvent2Json
  }

}
