package csw.services.event.cli

import org.scalatest.{FunSuite, Matchers}
import ujson.Js
import upickle.default.read

import scala.collection.mutable
import scala.io.Source

class EventJsonTransformerOnelineTest extends FunSuite with Matchers {

  private val logBuffer = mutable.Buffer.empty[String]

  private def testPrintln(msg: Any): Unit = logBuffer += msg.toString

  test("should print values for specified paths per event key") {
    val transformer = new EventJsonTransformer(testPrintln, Options())

    val eventJson  = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val event2Json = read[Js.Obj](Source.fromResource("get/input/event2.json").mkString)
    transformer.transformInPlace(eventJson, List("struct-1/ra"))
    transformer.transformInPlace(event2Json, List("struct-2/struct-1/ra"))
    val expected = List(
      "struct-1/ra = [\"12:13:14.1\"]",
      "struct-2/struct-1/ra = [\"12:13:14.1\"]"
    )
    logBuffer should contain allElementsOf expected
  }

  test("should print values with units for specified paths per event key") {
    val transformer = new EventJsonTransformer(testPrintln, Options(printUnits = true))

    val eventJson  = read[Js.Obj](Source.fromResource("get/input/event1.json").mkString)
    val event2Json = read[Js.Obj](Source.fromResource("get/input/event2.json").mkString)
    transformer.transformInPlace(eventJson, List("struct-1/ra"))
    transformer.transformInPlace(event2Json, List("struct-2/struct-1/ra"))
    val expected = List(
      "struct-1/ra = [\"12:13:14.1\"] [NoUnits]",
      "struct-2/struct-1/ra = [\"12:13:14.1\"] [NoUnits]"
    )
    logBuffer should contain allElementsOf expected
  }

}
