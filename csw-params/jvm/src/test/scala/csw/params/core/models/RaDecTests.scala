package csw.params.core.models
import csw.params.core.models.PositionsHelpers.EqCoordinate
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json
import play.api.libs.json.Json

class RaDecTests extends FunSpec with Matchers {
  import Angle._

  describe("Basic Coordinate Tests") {

    it("Should allow creating with angles") {

      val x = EqCoordinate("12:32.01", "+44:00:10")

      println("X: " + x)

      val y = EqCoordinate(180.0, 32.0)

      println("Y: " + y)

      val z = EqCoordinate(18.arcHour, -35.degree)

      println("Z: " + z)

      val a = EqCoordinate("10:12:45.3-45:17:50")

      println("A: " + a)

    }

    it("Should convert to/from JSON") {

      val y = EqCoordinate(180.0, 32.0)

      println("Y: " + y)

      val json = Json.toJson(y)

      println("JS: " + json)

      val eqc = json.as[EqCoordinate]

      println("EQC: " + eqc)

    }
  }

}
