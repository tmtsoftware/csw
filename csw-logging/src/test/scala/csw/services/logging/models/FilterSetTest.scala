package csw.services.logging.models

import com.persist.JsonOps
import csw.services.logging.internal.LoggingLevels._
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
class FilterSetTest extends FunSuite with Matchers {

  val logMessage: String =
    """{
      |"@componentName":"tromboneHcd",
      | "@severity":"WARN",
      | "@version":1,
      | "class":"csw.services.logging.Class2",
      | }
    """.stripMargin

  val json = JsonOps.Json(logMessage).asInstanceOf[Map[String, String]]

  test("check should return true when filter exists with severity level less than message severity level") {
    val filters = Map("tromboneHcd" → INFO, "tromboneAssembly" → DEBUG)
    new FilterSet(filters).check(json, Level(json("@severity"))) shouldBe true
  }

  test("check should return true when filter exists with severity level greater than message severity level") {
    val filters = Map("tromboneHcd" → ERROR, "tromboneAssembly" → DEBUG)
    new FilterSet(filters).check(json, Level(json("@severity"))) shouldBe false
  }
}
