package csw.params.core.models

import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExposureIdTest extends AnyFunSpec with Matchers {

  describe("create ExposureId from String and validate members") {
    it("should create valid ExposureId with ObsId | CSW-121") {
      val exposureId = ExposureId("2020A-001-123-CSW-IMG1-SCI0-0001")
      // Verify parts
      exposureId.toString shouldBe "2020A-001-123-CSW-IMG1-SCI0-0001"
      exposureId.obsId shouldBe Some(ObsId("2020A-001-123"))
      exposureId.det shouldBe "IMG1"
      exposureId.subsystem shouldBe Subsystem.CSW
      exposureId.typLevel shouldBe TYPLevel("SCI0")
      exposureId.exposureNumber shouldBe ExposureNumber("0001")
      // verify total equality once
      exposureId should ===(
        ExposureIdWithObsId(Some(ObsId("2020A-001-123")), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001"))
      )
    }

    it("should create valid ExposureId from a String with no ObsId | CSW-121") {
      // For testing only to get at UTC for equality test below
      val utcTime    = UTCTime.now()
      val exposureId = ExposureId.withUTC(ExposureId("CSW-IMG1-SCI0-0001"), utcTime)

      val standaloneExpId = exposureId.asInstanceOf[StandaloneExposureId]
      exposureId.toString shouldBe s"${ExposureId.utcAsStandaloneString(utcTime)}-CSW-IMG1-SCI0-0001"

      // Verify parts are correct once for standalone
      exposureId.obsId shouldBe None
      exposureId.det shouldBe "IMG1"
      exposureId.subsystem shouldBe Subsystem.CSW
      exposureId.typLevel shouldBe TYPLevel("SCI0")
      exposureId.exposureNumber shouldBe ExposureNumber("0001")
      // verify total equality once
      exposureId should ===(
        StandaloneExposureId(standaloneExpId.utcTime, Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0001"))
      )
    }

    it("should create valid ExposureId from a String with and without ObsId or subArray | CSW-121") {
      val exposureId = ExposureId("2031A-001-123-CSW-IMG1-SCI0-0001-00")
      exposureId.toString shouldBe "2031A-001-123-CSW-IMG1-SCI0-0001-00"
      exposureId.exposureNumber.exposureNumber shouldBe 1
      exposureId.exposureNumber.subArray shouldBe Some(0)

      val exposureId2 = ExposureId("2031A-001-123-CSW-IMG1-SCI0-0001")
      exposureId2.toString shouldBe "2031A-001-123-CSW-IMG1-SCI0-0001"
      exposureId2.exposureNumber.exposureNumber shouldBe 1
      exposureId2.exposureNumber.subArray shouldBe None

      val testUTC     = UTCTime.now()
      val exposureId3 = ExposureId.withUTC(ExposureId("CSW-IMG1-SCI0-0001"), testUTC)
      exposureId3.toString shouldBe s"${ExposureId.utcAsStandaloneString(testUTC)}-CSW-IMG1-SCI0-0001"
      exposureId3.exposureNumber.exposureNumber shouldBe 1
      exposureId3.exposureNumber.subArray shouldBe None

      val exposureId4 = ExposureId.withUTC(ExposureId("CSW-IMG1-SCI0-0001-04"), testUTC)
      exposureId4.toString shouldBe s"${ExposureId.utcAsStandaloneString(testUTC)}-CSW-IMG1-SCI0-0001-04"
      exposureId4.exposureNumber.exposureNumber shouldBe 1
      exposureId4.exposureNumber.subArray shouldBe Some(4)
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

    it("should increment ExposureId exposure number with helper | CSW-121") {
      val exposureId = ExposureId("2020A-001-123-CSW-IMG1-SCI0-0001")
      exposureId.exposureNumber shouldBe ExposureNumber(1)
      val exposureId2 = ExposureId.nextExposureNumber(exposureId)
      exposureId2.exposureNumber shouldBe ExposureNumber(2)
      exposureId2 should ===(
        ExposureIdWithObsId(Some(ObsId("2020A-001-123")), Subsystem.CSW, "IMG1", TYPLevel("SCI0"), ExposureNumber("0002"))
      )
    }

    it("should add subarray to ExposureId if empty with helper | CSW-121") {
      val exposureId = ExposureId("2031A-001-123-CSW-IMG1-SCI0-0001")
      exposureId.toString shouldBe "2031A-001-123-CSW-IMG1-SCI0-0001"
      exposureId.exposureNumber shouldBe ExposureNumber(1)
      exposureId.exposureNumber.subArray shouldBe None
      // Should add it at 00
      val exposureId2 = ExposureId.nextSubArrayNumber(exposureId)
      exposureId2.toString shouldBe "2031A-001-123-CSW-IMG1-SCI0-0001-00"
      exposureId2.exposureNumber shouldBe ExposureNumber(1, Some(0))
      exposureId2.exposureNumber.subArray shouldBe Some(0)
      // Should now increment
      val exposureId3 = ExposureId.nextSubArrayNumber(exposureId2)
      exposureId3.toString shouldBe "2031A-001-123-CSW-IMG1-SCI0-0001-01"
      exposureId3.exposureNumber shouldBe ExposureNumber(1, Some(1))
      exposureId3.exposureNumber.subArray shouldBe Some(1)
    }

    it("should convert with ObsId to standalone ExposureId | CSW-121") {
      val exposureId = ExposureId("2031A-001-123-CSW-IMG1-SCI0-0001")
      exposureId.obsId shouldBe Some(ObsId("2031A-001-123"))

      val utcTime     = UTCTime.now()
      val exposureId2 = ExposureId.withUTC(exposureId, utcTime)
      exposureId2.obsId shouldBe None
    }

    it("should convert without ObsId to ExposureId with ObsId | CSW-121") {
      val exposureId = ExposureId("CSW-IMG1-SCI0-0001")
      exposureId.obsId shouldBe None

      val exposureId2 = ExposureId.withObsId(exposureId, "2031A-001-123")
      exposureId2.obsId shouldBe Some(ObsId("2031A-001-123"))
    }

    it("should verify String format for standalone ExposureId | CSW-121") {
      import java.time.Instant
      // Make up a date time for creating standalone
      val instant       = Instant.parse("1980-04-09T15:30:45.123Z")
      val utcTime       = UTCTime(instant)
      val utcTimeString = ExposureId.utcAsStandaloneString(utcTime)
      utcTimeString.length shouldBe 15
      utcTimeString shouldBe "19800409-153045"
    }
  }
}
