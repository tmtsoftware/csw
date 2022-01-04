package csw.params.core.models
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AngleTests extends AnyFunSpec with Matchers {

  describe("Basic parsing of radec as strings") {
    import Angle._

    it("should allow basic parsing using Angle ") {

      (Angle.parseRa("20 54 05.689"), Angle.parseDe("+37 01 17.38")) shouldEqual
      Angle.parseRaDe("20 54 05.689 +37 01 17.38")

      (Angle.parseRa("10:12:45.3"), Angle.parseDe("-45:17:50")) shouldEqual
      Angle.parseRaDe("10:12:45.3-45:17:50")

      (Angle.parseRa("15h17m"), Angle.parseDe("-11d10m")) shouldEqual
      Angle.parseRaDe("15h17m-11d10m")

      (Angle.parseRa("275d11m15.6954s"), Angle.parseDe("+17d59m59.876s")) shouldEqual
      Angle.parseRaDe("275d11m15.6954s+17d59m59.876s")
    }

    it("should allow using implicits") {

      (12.34567.arcHour, -17.87654d.degree) shouldEqual Angle.parseRaDe("12.34567h-17.87654d")

      (350.123456.degree, -17.33333.degree) shouldEqual Angle.parseRaDe("350.123456d-17.33333d")

      (350.123456.degree, -17.33333.degree) shouldEqual Angle.parseRaDe("350.123456 -17.33333")
    }

  }

  describe("Test parsing") {

    it("should allow testing of parser to microarcsecs -1") {
      Angle
        .parseRa("1", "2", "3")
        .uas shouldEqual 1L * 15L * 60L * 60L * 1000L * 1000L + 2L * 15L * 60L * 1000L * 1000L + 3L * 15L * 1000L * 1000L
      Angle
        .parseDe("+", "1", "2", "3")
        .uas shouldEqual 1L * 60L * 60L * 1000L * 1000L + 2L * 60L * 1000L * 1000L + 3L * 1000L * 1000L
    }

    it("should allow parsing to microsarcsecs -2") {
      Angle
        .parseRa("1h2m3s")
        .uas shouldEqual 1L * 15L * 60L * 60L * 1000L * 1000L + 2L * 15L * 60L * 1000L * 1000L + 3L * 15L * 1000L * 1000L
      Angle.parseRa("02 51.2").uas shouldEqual 2L * 15L * 60L * 60L * 1000L * 1000L + 512L * 15L * 60L * 1000L * 100L
      Angle.parseDe("+1d2'3\"").uas shouldEqual 1L * 60L * 60L * 1000L * 1000L + 2L * 60L * 1000L * 1000L + 3L * 1000L * 1000L
      Angle.parseDe("-1d2'3\"").uas shouldEqual -(1L * 60L * 60L * 1000L * 1000L + 2L * 60L * 1000L * 1000L + 3L * 1000L * 1000L)
      Angle.parseDe("+13 12").uas shouldEqual 13L * 60L * 60L * 1000L * 1000L + 12L * 60L * 1000L * 1000L
    }
  }

  describe("conversion tests") {
    it("should allow conversions") {
      Angle.D2R * 1d shouldEqual math.toRadians(1d)
      Angle.R2D * 1d shouldEqual math.toDegrees(1d)
      Angle.H2D * 1d shouldEqual 15d
      Angle.D2H * 1d shouldEqual 1d / 15d
      Angle.D2M shouldEqual 60d
      Angle.M2D shouldEqual 1d / 60d
      Angle.D2S shouldEqual 3600d
      Angle.S2D shouldEqual 1d / 3600d
      Angle.H2R * 1d shouldEqual math.toRadians(15d)
      Angle.R2H * math.toRadians(15d) shouldEqual 1d
      Angle.M2R * 60d shouldEqual math.toRadians(1d)
      Angle.R2M * math.toRadians(1d) shouldEqual 60d
      Angle.Mas2R shouldEqual Angle.D2R / 3600000d
      Angle.R2Mas shouldEqual 1d / Angle.Mas2R
    }
  }

  describe("Should allow distance calculation") {

    it("Should do distance") {
      Angle.distance(Angle.D2R * 1d, 0d, Angle.D2R * 2d, 0d) shouldEqual Angle.D2R * 1d
      Angle.distance(0, Angle.D2R * 90d, Angle.D2R * 180d, -(Angle.D2R * 90d)) shouldEqual Angle.D2R * 180d
    }
  }

  describe("Positions to String") {

    it("should convert RA to string") {
      "11h" shouldBe Angle.raToString(Angle.H2R * 11, withColon = false)
      "11:00:00.000" shouldBe Angle.raToString(Angle.H2R * 11)

      "11h 12m" shouldBe Angle.raToString(Angle.H2R * 11 + Angle.H2R * 12 / 60, withColon = false)
      "11:12:00.000" shouldBe Angle.raToString(Angle.H2R * 11 + Angle.H2R * 12 / 60)

      "11h 12m 13s" shouldBe Angle.raToString(Angle.H2R * 11 + Angle.H2R * 12 / 60 + Angle.H2R * 13 / 3600, withColon = false)
      "11:12:13.000" shouldBe Angle.raToString(Angle.H2R * 11 + Angle.H2R * 12 / 60 + Angle.H2R * 13 / 3600)

      "11h 12m 13.3s" shouldBe Angle.raToString(Angle.H2R * 11 + Angle.H2R * 12 / 60 + Angle.H2R * 13.3 / 3600, withColon = false)
      "11:12:13.300" shouldBe Angle.raToString(Angle.H2R * 11 + Angle.H2R * 12 / 60 + Angle.H2R * 13.3 / 3600)

      "01:02:03.330" shouldBe Angle.raToString(Angle.parseRa("01:02:03.33").toRadian)
    }

    it("should convert Dec to string") {
      "11" + Angle.DEGREE_SIGN shouldBe Angle.deToString(Angle.D2R * 11, withColon = false)
      "11:00:00.000" shouldBe Angle.deToString(Angle.D2R * 11)

      "11" + Angle.DEGREE_SIGN + "12'" shouldBe Angle.deToString(Angle.D2R * 11 + Angle.M2R * 12, withColon = false)
      "11:12:00.000" shouldBe Angle.deToString(Angle.D2R * 11 + Angle.M2R * 12)

      "11" + Angle.DEGREE_SIGN + "12'13\"" shouldBe Angle.deToString(
        Angle.D2R * 11 + Angle.M2R * 12 + Angle.S2R * 13,
        withColon = false
      )
      "11:12:13.000" shouldBe Angle.deToString(Angle.D2R * 11 + Angle.M2R * 12 + Angle.S2R * 13)

      "11" + Angle.DEGREE_SIGN + "12'13.3\"" shouldBe Angle.deToString(
        Angle.D2R * 11 + Angle.M2R * 12 + Angle.S2R * 13.3,
        withColon = false
      )
      "11:12:13.300" shouldBe Angle.deToString(Angle.D2R * 11 + Angle.M2R * 12 + Angle.S2R * 13.3)

      "-11" + Angle.DEGREE_SIGN + "12'" shouldBe Angle.deToString(-(Angle.D2R * 11 + Angle.M2R * 12), withColon = false)
      "-11:12:00.000" shouldBe Angle.deToString(-(Angle.D2R * 11 + Angle.M2R * 12))

      "01:02:03.330" shouldBe Angle.deToString(Angle.parseDe("01:02:03.33").toRadian)
    }
  }
}
