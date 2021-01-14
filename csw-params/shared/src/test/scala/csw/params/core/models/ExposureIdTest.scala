package csw.params.core.models

import csw.prefix.models.Subsystem
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExposureIdTest extends AnyFunSpec with Matchers {
  describe("create ExposureId") {
    it("should create valid ExposureId") {
      val exposureId = ExposureId("2020A-001-123-CSW-IMG1-SCI0-0001")
      exposureId.toString should ===("2020A-001-123-CSW-IMG1-SCI0-0001")
      exposureId should ===(ExposureId(ObsId("2020A-001-123"), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001")))
    }

    it("should create valid ExposureId with subArray in exposure number") {
      val exposureId = ExposureId("2020A-001-123-CSW-IMG1-SCI0-0001-01")
      exposureId.toString should ===("2020A-001-123-CSW-IMG1-SCI0-0001-01")
      exposureId should ===(
        ExposureId(ObsId("2020A-001-123"), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001-01"))
      )
    }

    it("should throw exception if invalid obsId in exposure Id") {
      val exception = intercept[IllegalArgumentException](ExposureId("2020A-ABC-123-CSW-IMG1-SCI0-0001"))
      exception.getMessage should ===(
        "requirement failed: Invalid exposure Id: ObsId format should be [Year][Semester]-XXX-XXX e.g. 2020A-001-123"
      )
    }

    it("should throw exception if invalid exposure Id: tYPLevel is missing") {
      val exception = intercept[IllegalArgumentException](ExposureId("2020A-001-123-CSW-IMG1-0001"))
      exception.getMessage should ===(
        "requirement failed: Invalid exposure Id: ExposureId should be - string composing SemesterId-ProgramId-ObservationNumber-Subsystem-DET-TyPLevel-ExposureNumber"
      )

      val exception1 = intercept[NoSuchElementException](ExposureId("2020A-001-123-CSW-IMG1-0001-01"))
      exception1.getMessage should ===(
        "000 is not a member of Enum (SCI, CAL, ARC, IDP, DRK, MDK, FFD, NFF, BIA, TEL, FLX, SKY)"
      )
    }
  }
}
