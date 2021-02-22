package csw.params.core.models

import csw.prefix.models.Subsystem
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class StandaloneExposureIdTest extends AnyFunSpec with Matchers {
  describe("create StandaloneExposureId") {
    val utcTimeStr: String = "20200114-122334"

    it("should create valid StandaloneExposureId | CSW-121") {
      val exposureId = StandaloneExposureId(s"$utcTimeStr-CSW-IMG1-SCI0-0001")
      exposureId.toString should ===(s"$utcTimeStr-CSW-IMG1-SCI0-0001")
      exposureId should ===(StandaloneExposureId(utcTimeStr, Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001")))
    }

    it("should create valid StandaloneExposureId with subArray in exposure number | CSW-121") {
      val exposureId = StandaloneExposureId(s"$utcTimeStr-CSW-IMG1-SCI0-0001-01")
      exposureId.toString should ===(s"$utcTimeStr-CSW-IMG1-SCI0-0001-01")
      exposureId should ===(
        StandaloneExposureId(utcTimeStr, Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001-01"))
      )
    }

    it("should throw exception if invalid StandaloneExposureId: typLevel is missing | CSW-121") {
      val exception =
        intercept[IllegalArgumentException](StandaloneExposureId(s"$utcTimeStr-CSW-IMG1-0001"))
      exception.getMessage should ===(
        "Invalid StandaloneExposureId Id: StandaloneExposureId should be - string composing " +
          "YYYYMMDD-HHMMSS-Subsystem-DET-TYPLevel-ExposureNumber"
      )

      val exception1 =
        intercept[NoSuchElementException](StandaloneExposureId(s"$utcTimeStr-CSW-IMG1-0001-01"))
      exception1.getMessage should ===(
        "000 is not a member of Enum (SCI, CAL, ARC, IDP, DRK, MDK, FFD, NFF, BIA, TEL, FLX, SKY)"
      )
    }
  }
}
