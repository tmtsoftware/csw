package csw.params.core.models

import csw.params.commands.{CommandName, Setup}
import csw.params.core.formats.JsonSupport
import csw.params.core.generics.{KeyType, Parameter}
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.Json

class CoordsTests extends FunSpec with Matchers {
  import Angle._
  import KeyType._
  import Coords._

  private val src = Prefix("esw.ocs.seq")

  describe("Basic Coordinate Tests") {

    def raToUas(h: Long, m: Long, s: Double): Long =
      h * 15L * 60L * 60L * 1000L * 1000L + m * 15L * 60L * 1000L * 1000L + (s * 1000).toLong * 15L * 1000L

    def decToUas(d: Long, m: Long, s: Double): Long =
      d * 60L * 60L * 1000L * 1000L + m * 60L * 1000L * 1000L + (s * 1000).toLong * 1000L

    it("Should allow creating with strings - check ra dec") {
      // One hard test
      val c1 = EqCoord("12:32:01.689", "+44:01:05.12") // Note special multiply to accomodate fraction
      c1.ra.uas shouldEqual raToUas(12L, 32L, 1.689)
      c1.dec.uas shouldEqual decToUas(44L, 1L, 5.12)
    }

    it("should allow creating with degrees - check ra dec") {
      val c1 = EqCoord(185.0, 32.0)
      //c1.ra.uas shouldEqual raToUas((185d*Angle.D2H).toLong, 0, 0)

      println("Y: " + c1)

      val z = EqCoord(18.arcHour, -35.degree, ICRS, tag = OIWFS1, pmy = 2.0, catalogName = "NGC1234")

      println("Z: " + z)

      val a = EqCoord.asBoth("10:12:45.3-45:17:50", FK5)

      println("A: " + a)

    }
  }
  describe("Test defaults") {

    it("check defaults") {
      val c1 = EqCoord(18.arcHour, -1.degree)
      c1.ra shouldEqual Angle(18 * Angle.H2Uas)
      c1.dec shouldEqual Angle(-1 * Angle.D2Uas)
      c1.catalogName shouldBe "none"
      c1.tag shouldBe BASE
      c1.frame shouldBe ICRS
      c1.pm shouldEqual ProperMotion.DEFAULT_PROPERMOTION
    }
  }

  describe("JSON tests") {

    it("Should convert to/from JSON") {
      // Check proper motions
      val pm = ProperMotion(0.5, 2.33)
      val pmjs = Json.toJson(pm)

      println("pmjs: " + pmjs)

      val pmIn = pmjs.as[ProperMotion]
      pmIn shouldEqual pm

      // Check EqCoordinate
      val eq = EqCoord(ra = 180.0, frame = FK5, dec = 32.0, pmx = pm.pmx, pmy = pm.pmy)

      println("Eq: " + eq)

      val js = Json.toJson(eq)

      println("JS: " + js)

      val eqIn = js.as[EqCoord]

      println("EQC: " + eqIn)

      eqIn shouldBe eq

      val baseKey = EqCoordKey.make("BasePosition")
      val posParam = baseKey.set(eq)


      val paramOut = posParam.toJson
      val paramIn = paramOut.as[Parameter[EqCoord]]

      println("ParamIn: " + paramIn)

      val setup:Setup = Setup(src, CommandName("test"), None).add(posParam)

      val setupOut = JsonSupport.writeSequenceCommand(setup)

      println("sOut: " + setupOut)

      val setupIn = JsonSupport.readSequenceCommand[Setup](setupOut)
      println("SetupIN: " + setupIn)

      setupIn shouldEqual setup
    }
  }

  describe("Setup as positions with all") {
    val obsModeKey = StringKey.make("obsmode")

    it("Create multiple positions in one parameter") {
      // This example creates a key called positions with several positions.
      // A simple search allows fetching a specific position
      val eqKey = EqCoordKey.make("positions")

      val c0 = EqCoord("12:32:45", "+45:17:50", tag = BASE, frame = FK5, pmx = 0.9, pmy = -0.4)
      val c1 = EqCoord(188.0070373.degree, 45.018088889.degree, tag = OIWFS1)
      val c2 = EqCoord("12:32:03.1", "45:15:02.22", tag = OIWFS2)

      val positions: Parameter[EqCoord] = eqKey.set(c0, c1, c2)

      // Access second coordinate using param API
      val getc1 = positions.get(1)
      getc1 shouldEqual Some(c1)

      // Small function to extract a specific position
      def findTag(param: Parameter[EqCoord], tag: Tag): Option[EqCoord] = {
        param.values.find(_.tag == tag)
      }
      findTag(positions, OIWFS2) shouldEqual Some(c2)

      val obsMode = obsModeKey.set("IRIS LGS Mode 1")
      val setup      = Setup(src, CommandName("slewAndFollow"), None).madd(obsMode, positions)

      println("Setup: " + setup)

    }

    it("Create multiple positions in individual params with positions catalog") {

      val c0 = EqCoord("12:32:45", "+45:17:50", tag = BASE, frame = FK5, pmx = 0.9, pmy = -0.4)
      val c1 = EqCoord("12:32:01.689", "45:01:05.12", tag = OIWFS1)
      val c2 = EqCoord("12:32:03.1", "45:15:02.22", tag = OIWFS2)

      val obsMode = obsModeKey.set("IRIS LGS Mode 1")
      val setup = Setup(src, CommandName("slewAndFollow"), None).madd(obsMode,
                                                                      EqCoordKey.make(c0.tag.toString).set(c0),
                                                                      EqCoordKey.make(c1.tag.toString).set(c1),
                                                                      EqCoordKey.make(c2.tag.toString).set(c2))
      println("Setup: " + setup)

      // Small function to extract a specific position
      def findTag(setup: Setup, tag: Tag): Option[EqCoord] = {
        setup.get(EqCoordKey.make(tag.toString)) match {
          case None      => None
          case Some(eqp) => eqp.get(0)
        }
      }
      // Access second coordinate using param API
      val getc1 = findTag(setup, OIWFS1)
      getc1 shouldEqual Some(c1)
    }


    it("Create multiple positions in individual params for each major type: base, oiwfs, guide") {

      val baseKey = EqCoordKey.make("BasePosition")
      val oiwfsKey = EqCoordKey.make("OIWFSPositions")
      val guideKey = EqCoordKey.make("GuidePositions")


      val c0 = EqCoord("12:32:45", "+45:17:50", tag = BASE, frame = FK5, pmx = 0.9, pmy = -0.4)
      val c1 = EqCoord("12:32:01.689", "45:01:05.12", tag = OIWFS1)
      val c2 = EqCoord("12:32:03.1", "45:15:02.22", tag = OIWFS2)
      val c3 = EqCoord("12:33:03", "45:20:05", tag = GUIDE1)
      val c4 = EqCoord("12:32:03", "45:15:04", tag = GUIDE2)

      val obsMode = obsModeKey.set("IRIS LGS Mode 1")
      val setup = Setup(src, CommandName("slewAndFollow"), None).madd(obsMode,
        baseKey.set(c0),
        oiwfsKey.set(c1, c2),
        guideKey.set(c3, c4))

      println("Setup: " + setup)

      // Small functions to extract a specific position
      def findTag(param: Parameter[EqCoord], tag: Tag): Option[EqCoord] = {
        param.values.find(_.tag == tag)
      }

      // Need one of these maybe for each tupe
      def findOIWFS(setup: Setup, tag: Tag): Option[EqCoord] = {
        setup.get(oiwfsKey) match {
          case None      => None
          case Some(eqp) => findTag(eqp, tag)
        }
      }
      // Access second coordinate using param API
      val getc1 = findOIWFS(setup, OIWFS1)
      getc1 shouldEqual Some(c1)
    }
  }
}