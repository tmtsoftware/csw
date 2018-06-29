package csw.services.event.cli.temp

import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.{JsValue, Json}

import scala.io.Source

class JsonParserTest extends FunSuite with Matchers {

  val inputJson: JsValue          = Json.parse(Source.fromResource("input.json").mkString)
  val expectedOutputJson: JsValue = Json.parse(Source.fromResource("output.json").mkString)

  ignore("json test") {
    val actualOutputJson = EventParser.parse(inputJson, "top1/ra")
    expectedOutputJson shouldBe actualOutputJson
  }

}
