package csw.params.core.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExposureNumberTest extends AnyFunSpec with Matchers {
  describe("create ExposureNumber") {
    it("should create valid ExposureNumber | CSW-121") {
      val exposureNumber = ExposureNumber("0001")
      exposureNumber should ===(ExposureNumber(1))
      exposureNumber.toString should ===("0001")
    }

    it("should create valid ExposureNumber with subArray | CSW-121") {
      val exposureNumber = ExposureNumber("0001-01")
      exposureNumber should ===(ExposureNumber(1, Some(1)))
      exposureNumber.toString should ===("0001-01")
    }

    it("should throw exception if ExposureNumber is invalid | CSW-121") {
      val exception = intercept[IllegalArgumentException](ExposureNumber("10000"))
      exception.getMessage should ===(
        "requirement failed: Invalid exposure number 10000: exposure number should be provided 4 digit. " +
          "ExposureNumber should be 4 digit number and optional 2 digit sub array in format XXXX-XX or XXXX"
      )
    }

    it("should throw exception if subarray in exposure number is invalid | CSW-121") {
      val exception = intercept[IllegalArgumentException](ExposureNumber("0002-123"))
      exception.getMessage should ===(
        "requirement failed: Invalid exposure number 123: exposure number should be provided 2 digit. " +
          "ExposureNumber should be 4 digit number and optional 2 digit sub array in format XXXX-XX or XXXX"
      )
    }

    it("should throw exception if exposure number contains more than one '-'  | CSW-121") {
      val exception = intercept[IllegalArgumentException](ExposureNumber("0001-01-hhhs"))
      exception.getMessage should ===(
        "requirement failed: Invalid exposure number 0001-01-hhhs: exposure number should be provided 4 digit. " +
          "ExposureNumber should be 4 digit number and optional 2 digit sub array in format XXXX-XX or XXXX"
      )
    }
  }
}
