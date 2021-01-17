package csw.params.core.models

import csw.prefix.models.Subsystem
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExposureIdTest extends AnyFunSpec with Matchers {
  describe("create ExposureId") {
    it("should create valid ExposureId | CSW-121") {
      val exposureId = ExposureId("2020A-001-123-CSW-IMG1-SCI0-0001")
      exposureId.toString should ===("2020A-001-123-CSW-IMG1-SCI0-0001")
      exposureId should ===(ExposureId(ObsId("2020A-001-123"), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001")))
    }

    it("should create valid ExposureId with subArray in exposure number | CSW-121") {
      val exposureId = ExposureId("2020A-001-123-CSW-IMG1-SCI0-0001-01")
      exposureId.toString should ===("2020A-001-123-CSW-IMG1-SCI0-0001-01")
      exposureId should ===(
        ExposureId(ObsId("2020A-001-123"), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001-01"))
      )
    }

    it("should throw exception if invalid obsId in exposure Id | CSW-121") {
      val exception =
        intercept[IllegalArgumentException](ExposureId("2020A-ABC-123-CSW-IMG1-SCI0-0001"))
      exception.getMessage should ===(
        "requirement failed: ProgramId must form with semesterId, programNumber separated with '-' ex: 2020A-001"
      )
    }

    it("should throw exception if invalid exposure Id: typLevel is missing | CSW-121") {
      val exception = intercept[IllegalArgumentException](ExposureId("2020A-001-123-CSW-IMG1-0001"))
      exception.getMessage should ===(
        "requirement failed: Invalid exposure Id: ExposureId should be - string composing " +
          "SemesterId-ProgramNumber-ObservationNumber-Subsystem-DET-TYPLevel-ExposureNumber"
      )

      val exception1 = intercept[NoSuchElementException](ExposureId("2020A-001-123-CSW-IMG1-0001-01"))
      exception1.getMessage should ===(
        "000 is not a member of Enum (SCI, CAL, ARC, IDP, DRK, MDK, FFD, NFF, BIA, TEL, FLX, SKY)"
      )
    }
  }
}
