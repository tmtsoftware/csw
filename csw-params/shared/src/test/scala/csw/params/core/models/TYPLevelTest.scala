package csw.params.core.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TYPLevelTest extends AnyFunSpec with Matchers {
  describe("create TYPLevel") {
    it("should create TYPLevel | CSW-121") {
      val typLevel = TYPLevel("SCI0")
      typLevel.toString should ===("SCI0")
      typLevel should ===(TYPLevel(TYP.SCI, CalibrationLevel.Raw))
      typLevel.calibrationLevel should ===(CalibrationLevel.Raw)
      typLevel.calibrationLevelNumber shouldBe 0
    }

    it("should throw exception if invalid TYP | CSW-121") {
      val exception = intercept[NoSuchElementException](TYPLevel("XYZ0"))
      exception.getMessage should ===(
        "XYZ is not a member of Enum (SCI, CAL, ARC, IDP, DRK, MDK, FFD, NFF, BIA, TEL, FLX, SKY)"
      )
    }

    it("should throw exception if invalid calibrationLevel | CSW-121") {
      val exception = intercept[IllegalArgumentException](TYPLevel("SCI5"))
      exception.getMessage should ===(
        "Failed to parse calibration level 5: 5 is out of bounds (min 0, max 4). Calibration level should be a digit."
      )
    }

    it("should throw exception if no calibrationLevel | CSW-121") {
      val exception = intercept[IllegalArgumentException](TYPLevel("SCI"))
      exception.getMessage should ===("requirement failed: TYPLevel must be a 3 character TYP followed by a calibration level")
    }

    it("should throw exception if calibrationLevel is char | CSW-121") {
      val exception = intercept[IllegalArgumentException](TYPLevel("SCIC"))
      exception.getMessage should ===(
        "Failed to parse calibration level C: For input string: \"C\". Calibration level should be a digit."
      )
    }
  }
}
