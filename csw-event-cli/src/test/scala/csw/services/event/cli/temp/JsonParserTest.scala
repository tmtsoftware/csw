package csw.services.event.cli.temp

import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.{JsValue, Json}
import ujson.Js

import scala.collection.mutable
import scala.io.Source

class JsonParserTest extends FunSuite with Matchers {

  ignore("json test") {
    val inputJson: JsValue          = Json.parse(Source.fromResource("input.json").mkString)
    val expectedOutputJson: JsValue = Json.parse(Source.fromResource("output_top1_ra.json").mkString)
    val actualOutputJson            = EventParser.parse(inputJson, "top1/ra")
    actualOutputJson shouldBe expectedOutputJson
  }

  test("ujson test - inspect") {
    val logBuffer = mutable.Buffer.empty[String]

    val inputJson: Js = ujson.read(Source.fromResource("input.json").mkString)

    val expectedInspectResult =
      List(
        "top1 = StructKey[NoUnits]",
        "top1/ra = StringKey[NoUnits]",
        "top1/dec = StringKey[NoUnits]",
        "top1/epoch = DoubleKey[NoUnits]",
        "top3 = StructKey[NoUnits]",
        "top3/ra = StringKey[NoUnits]",
        "top3/dec = StringKey[NoUnits]",
        "top3/epoch = DoubleKey[NoUnits]",
        "top2 = StringKey[NoUnits]"
      )

    EventParser.inspect(inputJson.obj, msg ⇒ logBuffer += msg.toString)
    logBuffer shouldBe expectedInspectResult
  }

  test("ujson test - complete struct key") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top1_ra.json").mkString)

    val actualOutputJson = EventParser.parse(inputJson.obj, "top1/ra")
    actualOutputJson shouldBe expectedOutputJson
  }

  test("ujson test - partial struct key") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top1.json").mkString)

    val actualOutputJson = EventParser.parse(inputJson.obj, "top1")
    actualOutputJson shouldBe expectedOutputJson
  }

  test("ujson test - top level non struct key") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top2.json").mkString)

    val actualOutputJson = EventParser.parse(inputJson.obj, "top2")
    actualOutputJson shouldBe expectedOutputJson
  }

  test("ujson test - multiple keys - all different") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top1_ra_top2.json").mkString)

    val actualOutputJson = EventParser.parseWithMultipleKeys(inputJson.obj, List("top1/ra", "top2"))
    actualOutputJson shouldBe expectedOutputJson
  }

  test("ujson test - multiple struct keys - nested key same") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output_top1_ra_top3_dec.json").mkString)

    val actualOutputJson = EventParser.parseWithMultipleKeys(inputJson.obj, List("top1/ra", "top3/dec"))
    actualOutputJson shouldBe expectedOutputJson
  }
}
