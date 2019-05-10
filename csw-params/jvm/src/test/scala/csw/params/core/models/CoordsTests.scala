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

  describe("Basic Eq Coordinate Tests") {

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
      // Degrees
      val c1 = EqCoord(185.0.degree, 32.0.degree)
      Angle(185 * Angle.D2Uas) shouldEqual c1.ra
      Angle(32 * Angle.D2Uas) shouldEqual c1.dec

      // HMS/Deg, check all values here
      val c2 = EqCoord(18.arcHour, -35.degree, ICRS, tag = OIWFS1, pmy = 2.0, catalogName = "NGC1234")
      c2.ra.toDegree shouldEqual 18 * Angle.H2D
      c2.dec.toDegree shouldEqual -35
      c2.tag shouldBe OIWFS1
      c2.pm shouldEqual ProperMotion(0.0, 2.0)
      c2.catalogName shouldBe "NGC1234"

      // Strings
      val c3 = EqCoord("12:13:14.15", "-30:31:32.3")
      c3.ra.uas shouldEqual (12 * Angle.H2Uas + 13 * Angle.HMin2Uas + 14.15 * Angle.HSec2Uas)
      c3.dec.uas shouldEqual -1 * (30 * Angle.D2Uas + 31 * Angle.M2Uas + 32.3 * Angle.S2Uas)

      // Both as String
      val c4 = EqCoord.asBoth("10:12:45.3-45:17:50", FK5)
      c4.ra.uas shouldEqual (10 * Angle.H2Uas + 12 * Angle.HMin2Uas + 45.3 * Angle.HSec2Uas)
      c4.dec.uas shouldEqual -1 * (45 * Angle.D2Uas + 17 * Angle.M2Uas + 50 * Angle.S2Uas)
    }

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
    val pm = ProperMotion(0.5, 2.33)

    it("Should convert to/from JSON") {
      // Check proper motions
      val pm   = ProperMotion(0.5, 2.33)
      val pmjs = Json.toJson(pm)

      println("pmjs: " + pmjs)

      val pmIn = pmjs.as[ProperMotion]
      pmIn shouldEqual pm
    }

    it("should convert frame to/from") {
      val f1 = ICRS
      val j1 = Json.toJson(f1)
      println("J1: " + j1)
      j1.as[EQ_FRAME] shouldEqual ICRS

    }

    it("should allow alt az") {
      val c0 = AltAzCoord(BASE, 301.degree, 42.5.degree)

      val js = Json.toJson(c0)
      println("JS: " + js)
      val c1 = js.as[AltAzCoord]

      c0 shouldEqual c1
    }

    it("Should allow solar system coord") {
      val c0 = SolarSystemCoord(BASE, Venus)

      val js = Json.toJson(c0)
      println("JS: " + js)
      val c1 = js.as[SolarSystemCoord]

      c0 shouldEqual c1

      val c2: Coord = c0
      val js2       = Json.toJson(c2)
      println("COord: " + js2)
      js2.as[Coord] shouldEqual c2
      println("Its: " + js2.as[Coord])
    }

    it("Should allow minor planet coord") {
      val c0 = MinorPlanetCoord(GUIDER1, 2000.0d, 90.degree, 2.degree, 100.degree, 1.4d, 0.234d, 220.degree)

      val js = Json.toJson(c0)
      println("JS: " + js)
      val c1 = js.as[MinorPlanetCoord]

      c0 shouldEqual c1

      val c2: Coord = c0
      val js2       = Json.toJson(c2)
      println("COord: " + js2)
      js2.as[Coord] shouldEqual c2
      println("Its: " + js2.as[Coord])
    }

    it("should json an EqCoord") {
      // Check EqCoordinate
      val eq = EqCoord(ra = 180.0, frame = FK5, dec = 32.0, pmx = pm.pmx, pmy = pm.pmy)

      println("Eq: " + eq)

      val js = Json.toJson(eq)

      println("JS: " + js)

      val eqIn = js.as[EqCoord]

      println("EQC: " + eqIn)

      eqIn shouldBe eq
    }

    it("should allow a eqcoord parameter") {

      // Check EqCoordinate
      val eq = EqCoord(ra = 180.0, frame = FK5, dec = 32.0, pmx = pm.pmx, pmy = pm.pmy)

      val baseKey  = EqCoordKey.make("BasePosition")
      val posParam = baseKey.set(eq)

      val paramOut = Json.toJson(posParam)
      val paramIn  = paramOut.as[Parameter[EqCoord]]

      println("Its: " + paramOut)

      val setup: Setup = Setup(src, CommandName("test"), None).add(posParam)
      val setupOut     = JsonSupport.writeSequenceCommand(setup)
      val setupIn      = JsonSupport.readSequenceCommand[Setup](setupOut)

      setupIn shouldEqual setup
    }

    it("should allow as coord parameter") {

      // Check EqCoordinate
      val eq = EqCoord(ra = 180.0, frame = FK5, dec = 32.0, pmx = pm.pmx, pmy = pm.pmy)

      val baseKey  = CoordKey.make("BasePosition")
      val posParam = baseKey.set(eq)

      val paramOut = Json.toJson(posParam)
      val paramIn  = paramOut.as[Parameter[Coord]]
      paramIn shouldEqual posParam

      val setup: Setup = Setup(src, CommandName("test"), None).add(posParam)

      val setupOut = JsonSupport.writeSequenceCommand(setup)
      val setupIn  = JsonSupport.readSequenceCommand[Setup](setupOut)

      setupIn shouldEqual setup
    }
  }

  describe("position alternatives") {
    val obsModeKey = StringKey.make("obsmode")

    // Small functions to extract a specific tag
    def findCoordTagInParameter(param: Parameter[Coord], tag: Tag): Option[Coord] = {
      param.values.find(_.tag == tag)
    }

    def findTagInParameter(param: Parameter[EqCoord], tag: Tag): Option[EqCoord] = {
      param.values.find(_.tag == tag)
    }

    // Small function to extract a specific position
    def findTag(setup: Setup, tag: Tag): Option[Coord] = {
      setup.get(CoordKey.make(tag.name)) match {
        case None      => None
        case Some(eqp) => eqp.get(0)
      }
    }

    def findAllTags(setup:Setup): Set[String] = {
      val xx = allTags.map(fn => setup.get(CoordKey.make(fn.name)))
println("XX: " + xx)
      val yy = setup.paramSet.collect { case p: Parameter[_] if (allTagsNames.contains(p.keyName)) => p }
      println("YY: " + yy)
      allTags.flatMap(fn => setup.get(CoordKey.make(fn.name)).map(_.keyName))
    }

    it("Create multiple positions in individual params with positions catalog") {
      val obsModeKey = StringKey.make("obsmode")

      val c0 = EqCoord("12:32:45", "+45:17:50", tag = BASE, frame = FK5, pmx = 0.9, pmy = -0.4)
      val c1 = EqCoord("12:32:01.689", "45:01:05.12", tag = OIWFS1)
      val c2 = EqCoord("12:32:03.1", "45:15:02.22", tag = OIWFS2)

      val obsMode = obsModeKey.set("IRIS LGS Mode 1")
      val setup = Setup(src, CommandName("slewAndFollow"), None).madd(
        obsMode,
        CoordKey.make(c0.tag.name).set(c0),
        CoordKey.make(c1.tag.name).set(c1),
        CoordKey.make(c2.tag.name).set(c2)
      )
      println("Setup: " + setup)
      println("All Tags: " + findAllTags(setup))

      // Access second coordinate using param API
      val getc1 = findTag(setup, OIWFS1)
      getc1 shouldEqual Some(c1)

    }

    it("Create multiple positions in one parameter") {
      // This example creates a key called positions with several positions.
      // A simple search allows fetching a specific position
      val coordKey = CoordKey.make("targets")

      val c0 = EqCoord("12:32:45", "+45:17:50", tag = BASE, frame = FK5, pmx = 0.9, pmy = -0.4)
      val c1 = EqCoord(188.0070373.degree, 45.018088889.degree, tag = OIWFS1)
      val c2 = SolarSystemCoord(tag = OIWFS2, Pluto)

      val positions: Parameter[Coord] = coordKey.set(c0, c1, c2)

      println("Positions: " + positions)

      // Access second coordinate using param API
      //val getc1 = positions.get(1)
      //getc1 shouldEqual Some(c1)

      // Small function to extract a specific position
      def findtag(param: Parameter[Coord], tag: Tag): Option[Coord] = {
        param.values.find(_.tag == tag)
      }

      findTag(positions, OIWFS1) shouldEqual Some(c1)

      val obsMode = obsModeKey.set("IRIS LGS Mode 1")
      val setup   = Setup(src, CommandName("slewAndFollow"), None).madd(obsMode, positions)


    }

    it("Create multiple positions in individual params for each major type: base, oiwfs, guide") {
      val obsModeKey = StringKey.make("obsmode")

      val baseKey   = EqCoordKey.make("BasePosition")
      val oiwfsKey  = EqCoordKey.make("OIWFSPositions")
      val odgwKey   = EqCoordKey.make("ODGWPositions")
      val guiderKey = EqCoordKey.make("GuiderPositions")

      val c0 = EqCoord("12:32:45", "+45:17:50", tag = BASE, frame = FK5, pmx = 0.9, pmy = -0.4)
      val c1 = EqCoord("12:32:01.689", "45:01:05.12", tag = OIWFS1)
      val c2 = EqCoord("12:32:03.1", "45:15:02.22", tag = OIWFS2)
      val c3 = EqCoord("12:33:03", "45:20:05", tag = GUIDER1)
      val c4 = EqCoord("12:32:03", "45:15:04", tag = GUIDER2)
      val c5 = EqCoord.asBoth("12:32:03.3 +45:15:03", tag = ODGW1)
      val c6 = EqCoord("12:32:00.1", "45:14:49.5", tag = ODGW2)

      val obsMode = obsModeKey.set("IRIS LGS Mode 1")
      val setup =
        Setup(src, CommandName("slewAndFollow"), None).madd(
          obsMode,
          baseKey.set(c0),
          oiwfsKey.set(c1, c2),
          guiderKey.set(c3, c4),
          odgwKey.set(c5, c6)
        )

      println("Setup: " + setup)
      // Small function to extract a specific position
    }

  }

}
