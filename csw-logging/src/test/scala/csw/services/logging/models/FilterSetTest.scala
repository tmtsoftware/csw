package csw.services.logging.models

import com.persist.JsonOps
import csw.services.logging.internal.LoggingLevels._
import org.scalatest.{FunSuite, Matchers}

class FilterSetTest extends FunSuite with Matchers {

  val logMessage: String =
    """{
      |"@componentName":"tromboneHcd",
      | "@severity":"WARN",
      | "@version":1,
      | "class":"csw.services.logging.Class2",
      | }
    """.stripMargin

  test("check should return true when filter exists with severity level less than message severity level") {
    val json    = JsonOps.Json(logMessage).asInstanceOf[Map[String, String]]
    val filters = Map("tromboneHcd" → INFO, "Class1" → DEBUG)
    new FilterSet(filters).check(json, Level(json("@severity"))) shouldBe true
  }

  test("check should return true when filter exists with severity level greater than message severity level") {
    val json    = JsonOps.Json(logMessage).asInstanceOf[Map[String, String]]
    val filters = Map("tromboneHcd" → ERROR, "Class1" → DEBUG)
    new FilterSet(filters).check(json, Level(json("@severity"))) shouldBe false
  }
}
