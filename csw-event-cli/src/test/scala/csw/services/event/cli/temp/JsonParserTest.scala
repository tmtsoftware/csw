package csw.services.event.cli.temp

import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.{JsValue, Json}
import ujson.Js

import scala.io.Source

class JsonParserTest extends FunSuite with Matchers {

  ignore("json test") {
    val inputJson: JsValue          = Json.parse(Source.fromResource("input.json").mkString)
    val expectedOutputJson: JsValue = Json.parse(Source.fromResource("output.json").mkString)
    val actualOutputJson            = EventParser.parse(inputJson, "top1/ra")
    expectedOutputJson shouldBe actualOutputJson
  }

  test("ujson test") {
    val inputJson: Js          = ujson.read(Source.fromResource("input.json").mkString)
    val expectedOutputJson: Js = ujson.read(Source.fromResource("output.json").mkString)

    val actualOutputJson = EventParser.parse(inputJson.obj, "top1/ra")
    expectedOutputJson shouldBe actualOutputJson
  }

}
