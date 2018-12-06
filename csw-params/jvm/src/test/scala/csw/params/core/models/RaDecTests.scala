package csw.params.core.models
import csw.params.core.generics.Parameter
import csw.params.core.models.PositionsHelpers._
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.Json

class RaDecTests extends FunSpec with Matchers {
  import Angle._

  describe("Basic Coordinate Tests") {

    def raToUas(h: Long, m: Long, s: Double): Long =
      h*15L*60L*60L*1000L*1000L + m*15L*60L*1000L*1000L + (s*1000).toLong*15L*1000L

    def decToUas(d: Long, m: Long, s: Double): Long =
      d*60L*60L*1000L*1000L + m*60L*1000L*1000L + (s*1000).toLong*1000L


    it("Should allow creating with strings - check ra dec") {
      // One hard test
      val c1 = EqCoordinate("12:32:01.689", "+44:01:05.12") // Note special multiply to accomodate fraction
      c1.ra.uas shouldEqual raToUas(12L, 32L, 1.689)
      c1.dec.uas shouldEqual decToUas(44L, 1L, 5.12)
    }

    it("should allow creating with degrees - check ra dec") {
      val c1 = EqCoordinate(185.0, 32.0)
      //c1.ra.uas shouldEqual raToUas((185d*Angle.D2H).toLong, 0, 0)

      println("Y: " + c1)

      val z = EqCoordinate(18.arcHour, -35.degree, ICRS, tag = OIWFS1, pmy = 2.0, catalogName = "NGC1234")

      println("Z: " + z)

      val a = EqCoordinate.asBoth("10:12:45.3-45:17:50", FK5)

      println("A: " + a)

    }

    it("check defaults") {
      val c1 = EqCoordinate(18.arcHour, -1.degree)
      c1.ra shouldEqual Angle(18*Angle.H2Uas)
      c1.dec shouldEqual Angle(-1*Angle.D2Uas)
      c1.catalogName shouldBe "none"
      c1.tag shouldBe BASE
      c1.frame shouldBe ICRS
      c1.pm shouldEqual ProperMotion.DEFAULT_PROPERMOTION
    }

    it("Should convert to/from JSON") {

      val y = EqCoordinate(ra = 180.0, frame = FK5, dec = 32.0)

      println("Y: " + y)

      val json = Json.toJson(y)

      println("JS: " + json)

      val eqc = json.as[EqCoordinate]

      println("EQC: " + eqc)

    }

    def findTag(param:Parameter[EqCoordinate], tag: Tag):Option[EqCoordinate] = {
      param.values.find(_.tag == tag)
    }

    it("Should create a key!") {
      // This example creates a key called positions with all the positions.
      val eqKey = EqCoordinateKey.make("positions")

      val c0 = EqCoordinate("12:32:45.3", "+45:17:50", tag = BASE, frame = FK5, pmx = 0.9, pmy = -0.4)
      val c1 = EqCoordinate("12:32:01.689", "45:01:05.12", tag= OIWFS1)
      val c2 = EqCoordinate("12:32:03.1", "45:15:02.22", tag=OIWFS2)

      println("C1: " + c1.ra.toDegree + " " + c1.dec.toDegree)

      val param:Parameter[EqCoordinate]= eqKey.set(c0, c1, c2)

      // Access second coordinate
      val getc1 = param.get(1)
      println("C1 again: " + c1)

      println("Param: " + param)

      val keyJson = param.toJson
      println("Pramjson: " + keyJson)

      println("findTag: OIWFS2: " + findTag(param, OIWFS2))

    }
  }

}
