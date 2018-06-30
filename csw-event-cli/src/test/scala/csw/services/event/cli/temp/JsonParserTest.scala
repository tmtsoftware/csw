package csw.services.event.cli.temp

import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.{JsValue, Json}
import ujson.Js

import scala.io.Source

class JsonParserTest extends FunSuite with Matchers {

  ignore("json test") {
    val inputJson: JsValue          = Json.parse(Source.fromResource("input.json").mkString)
    val expectedOutputJson: JsValue = Json.parse(Source.fromResource("output_top1_ra.json").mkString)
    val actualOutputJson            = EventParser.parse(inputJson, "top1/ra")
    expectedOutputJson shouldBe actualOutputJson
  }

  test("ujson test - complete struct key") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top1_ra.json").mkString)

    val actualOutputJson = EventParser.parse(inputJson.obj, "top1/ra")
    expectedOutputJson shouldBe actualOutputJson
  }

  test("ujson test - partial struct key") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top1.json").mkString)

    val actualOutputJson = EventParser.parse(inputJson.obj, "top1")
    expectedOutputJson shouldBe actualOutputJson
  }

  test("ujson test - top level non struct key") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top2.json").mkString)

    val actualOutputJson = EventParser.parse(inputJson.obj, "top2")
    expectedOutputJson shouldBe actualOutputJson
  }

  test("ujson test - multiple keys - all different") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top1_ra_top2.json").mkString)

    val actualOutputJson = EventParser.parseWithMultipleKeys(inputJson.obj, List("top1/ra", "top2"))
    expectedOutputJson shouldBe actualOutputJson
  }

  ignore("ujson test - multiple struct keys - nested key same") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top1_ra_top3_dec.json").mkString)

    val actualOutputJson = EventParser.parseWithMultipleKeys(inputJson.obj, List("top1/ra", "top3/dec"))
    expectedOutputJson shouldBe actualOutputJson
  }
}
