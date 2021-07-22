package test.csw

import csw.params.core.models.CalibrationLevel.Raw
import csw.params.core.models.TYP.DRK
import csw.params.core.models.{ExposureId, ExposureIdWithObsId, ExposureNumber, ObsId, TYPLevel}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.CSW
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExposureIdTest extends AnyFunSpec with Matchers {

  describe("create ExposureId") {
    it("should create valid ExposureId with ObsId | CSW-121") {
      val exposureId = ExposureId("2020A-001-123-CSW-IMG1-SCI0-0001")
      // Verify parts
      exposureId.toString shouldBe "2020A-001-123-CSW-IMG1-SCI0-0001"
      exposureId.obsId shouldBe Some(ObsId("2020A-001-123"))
      exposureId.det shouldBe "IMG1"
      exposureId.subsystem shouldBe Subsystem.CSW
      exposureId.typLevel shouldBe TYPLevel("SCI0")
      exposureId.exposureNumber shouldBe ExposureNumber("0001")
    }

    it("should create valid ExposureId with no ObsId and then add ObsId | CSW-121") {
      val exposureId = ExposureId("CSW-IMG1-SCI0-0001")
      exposureId.obsId shouldBe None

      val exposureIdWithObsId = ExposureId.withObsId(exposureId, "2020B-100-456")
      exposureIdWithObsId.toString shouldBe "2020B-100-456-CSW-IMG1-SCI0-0001"
      exposureIdWithObsId.obsId shouldBe Some(ObsId("2020B-100-456"))
      exposureIdWithObsId.det shouldBe "IMG1"
      exposureIdWithObsId.subsystem shouldBe Subsystem.CSW
      exposureIdWithObsId.typLevel shouldBe TYPLevel("SCI0")
      exposureIdWithObsId.exposureNumber shouldBe ExposureNumber("0001")

      ExposureId.nextExposureNumber(exposureId).exposureNumber == ExposureNumber(2)
    }

    it("should create valid ExposureId with ObsId using constructor | CSW-121") {
      val exposureId = ExposureIdWithObsId(Some(ObsId("2020B-100-456")), CSW, "IMG1", TYPLevel(DRK, Raw), ExposureNumber(1))

      exposureId.toString shouldBe "2020B-100-456-CSW-IMG1-DRK0-0001"
      exposureId.obsId shouldBe Some(ObsId("2020B-100-456"))
      exposureId.det shouldBe "IMG1"
      exposureId.subsystem shouldBe Subsystem.CSW
      exposureId.typLevel shouldBe TYPLevel("DRK0")
      exposureId.exposureNumber shouldBe ExposureNumber("0001")
    }

    it("should throw exception if invalid obsId in exposure Id | CSW-121") {
      val exception =
        intercept[IllegalArgumentException](ExposureId("2020A-ABC-123-CSW-IMG1-SCI0-0001"))
      exception.getMessage shouldBe
      "A program Id consists of a semester Id and program number separated by '-' ex: 2020A-001"
    }

    it("should throw exception if invalid exposure Id: typLevel is missing | CSW-121") {
      val e1 = intercept[IllegalArgumentException](ExposureId("2020A-001-123-CSW-IMG1-0001"))
      e1.getMessage shouldBe ("requirement failed: An ExposureId must be a - separated string of the form " +
      "SemesterId-ProgramNumber-ObservationNumber-Subsystem-DET-TYPLevel-ExposureNumber")

      val e2 = intercept[NoSuchElementException](ExposureId("2020A-001-123-CSW-IMG1-0001-01"))
      e2.getMessage shouldBe
      "000 is not a member of Enum (SCI, CAL, ARC, IDP, DRK, MDK, FFD, NFF, BIA, TEL, FLX, SKY)"
    }
  }
}
