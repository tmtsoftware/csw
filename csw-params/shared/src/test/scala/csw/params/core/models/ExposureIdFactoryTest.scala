package csw.params.core.models

import csw.prefix.models.Subsystem
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExposureIdFactoryTest extends AnyFunSpec with Matchers {
  describe("create ExposureId") {
    it("should create valid ExposureId | CSW-121") {
      val exposureId = ExposureIdFactory.makeExposureId("2020A-001-123-CSW-IMG1-SCI0-0001")
      exposureId.toString should ===("2020A-001-123-CSW-IMG1-SCI0-0001")
      exposureId should ===(ExposureId(ObsId("2020A-001-123"), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001")))
    }

    it("should create valid ExposureId with subArray in exposure number | CSW-121") {
      val exposureId = ExposureIdFactory.makeExposureId("2020A-001-123-CSW-IMG1-SCI0-0001-01")
      exposureId.toString should ===("2020A-001-123-CSW-IMG1-SCI0-0001-01")
      exposureId should ===(
        ExposureId(ObsId("2020A-001-123"), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001-01"))
      )
    }

    it("should throw exception if invalid obsId in exposure Id | CSW-121") {
      val exception =
        intercept[IllegalArgumentException](ExposureIdFactory.makeExposureId("2020A-ABC-123-CSW-IMG1-SCI0-0001"))
      exception.getMessage should ===(
        "requirement failed: ProgramId must form with semesterId, programNumber separated with '-' ex: 2020A-001"
      )
    }

    it("should throw exception if invalid exposure Id: typLevel is missing | CSW-121") {
      val exception = intercept[IllegalArgumentException](ExposureIdFactory.makeExposureId("2020A-001-123-CSW-IMG1-0001"))
      exception.getMessage should ===(
        "requirement failed: Invalid exposure Id: ExposureId should be - string composing " +
          "SemesterId-ProgramNumber-ObservationNumber-Subsystem-DET-TYPLevel-ExposureNumber"
      )

      val exception1 = intercept[NoSuchElementException](ExposureIdFactory.makeExposureId("2020A-001-123-CSW-IMG1-0001-01"))
      exception1.getMessage should ===(
        "000 is not a member of Enum (SCI, CAL, ARC, IDP, DRK, MDK, FFD, NFF, BIA, TEL, FLX, SKY)"
      )
    }
  }
  describe("create StandaloneExposureId") {
    val utcTimeStr: String = "20200114-122334"

    it("should create valid StandaloneExposureId") {
      val exposureId = ExposureIdFactory.makeStandaloneExposureId(s"$utcTimeStr-CSW-IMG1-SCI0-0001")
      exposureId.toString should ===(s"$utcTimeStr-CSW-IMG1-SCI0-0001")
      exposureId should ===(StandaloneExposureId(utcTimeStr, Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001")))
    }

    it("should create valid StandaloneExposureId with subArray in exposure number") {
      val exposureId = ExposureIdFactory.makeStandaloneExposureId(s"$utcTimeStr-CSW-IMG1-SCI0-0001-01")
      exposureId.toString should ===(s"$utcTimeStr-CSW-IMG1-SCI0-0001-01")
      exposureId should ===(
        StandaloneExposureId(utcTimeStr, Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001-01"))
      )
    }

    it("should throw exception if invalid StandaloneExposureId: typLevel is missing") {
      val exception =
        intercept[IllegalArgumentException](ExposureIdFactory.makeStandaloneExposureId(s"$utcTimeStr-CSW-IMG1-0001"))
      exception.getMessage should ===(
        "Invalid StandaloneExposureId Id: StandaloneExposureId should be - string composing " +
          "YYYYMMDD-HHMMSS-Subsystem-DET-TYPLevel-ExposureNumber"
      )

      val exception1 =
        intercept[NoSuchElementException](ExposureIdFactory.makeStandaloneExposureId(s"$utcTimeStr-CSW-IMG1-0001-01"))
      exception1.getMessage should ===(
        "000 is not a member of Enum (SCI, CAL, ARC, IDP, DRK, MDK, FFD, NFF, BIA, TEL, FLX, SKY)"
      )
    }
  }
}
