package csw.params.core.models

import csw.prefix.models.Subsystem
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
      exposureId should ===(
        ExposureIdWithObsId(Some(ObsId("2020A-001-123")), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001"))
      )
    }

    it("should increment ExposureId exposure number | CSW-121") {
      val exposureId = ExposureId("2020A-001-123-CSW-IMG1-SCI0-0001")

      exposureId should ===(
        ExposureIdWithObsId(Some(ObsId("2020A-001-123")), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001"))
      )

      val exposureId2 = ExposureId.nextExposureNumber(exposureId)
      exposureId2 should ===(
        ExposureIdWithObsId(Some(ObsId("2020A-001-123")), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0002"))
      )
    }

    it("should create valid ExposureId with no ObsId | CSW-121") {
      val exposureId = ExposureId("CSW-IMG1-SCI0-0001")
      // For testing only to get at UTC for equality test below
      val standaloneExpId = exposureId.asInstanceOf[StandaloneExposureId]
      exposureId.toString shouldBe (standaloneExpId.utcAsString + "-CSW-IMG1-SCI0-0001")

      // Verify parts are correct once
      exposureId.obsId shouldBe None
      exposureId.det shouldBe "IMG1"
      exposureId.subsystem shouldBe Subsystem.CSW
      exposureId.typLevel shouldBe TYPLevel("SCI0")
      exposureId.exposureNumber shouldBe ExposureNumber("0001")

      // verify total equality ignoring time
      exposureId should ===(
        StandaloneExposureId(standaloneExpId.utcTime, Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001"))
      )
    }

    it("should create valid ExposureId with no ObsId and then add ObsId | CSW-121") {
      val exposureId = ExposureId("CSW-IMG1-SCI0-0001")
      exposureId.obsId shouldBe None

      val exposureIdWithObsId = ExposureId.withObsId(exposureId, "2020B-100-456")
      // verify total equality
      exposureIdWithObsId should ===(
        ExposureIdWithObsId(Some(ObsId("2020B-100-456")), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001"))
      )

      val obsId                = ObsId("2021B-200-007")
      val exposureIdWithObsId2 = ExposureId.withObsId(exposureId, obsId)
      exposureIdWithObsId2 should ===(
        ExposureIdWithObsId(Some(ObsId("2021B-200-007")), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001"))
      )
    }

    it("should create valid ExposureId with subArray in exposure number | CSW-121") {
      val exposureId = ExposureId("2020A-001-123-CSW-IMG1-SCI0-0001-01")
      exposureId.toString should ===("2020A-001-123-CSW-IMG1-SCI0-0001-01")
      exposureId.obsId should ===(Some(ObsId("2020A-001-123")))
      exposureId should ===(
        ExposureIdWithObsId(Some(ObsId("2020A-001-123")), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001-01"))
      )
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
