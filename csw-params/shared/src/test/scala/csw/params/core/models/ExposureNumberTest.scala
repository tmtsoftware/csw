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
        "requirement failed: Invalid exposure number: 10000. " +
          "An ExposureNumber must be a 4 digit number and optional 2 digit sub array in format XXXX or XXXX-XX"
      )
    }

    it("should throw exception if subarray in exposure number is invalid | CSW-121") {
      // Creates same message as above test
      intercept[IllegalArgumentException](ExposureNumber("0002-123"))
    }

    it("should throw exception if exposure number contains more than one '-'  | CSW-121") {
      // Creates the same message as above test
      intercept[IllegalArgumentException](ExposureNumber("0001-01-hhhs"))
    }
  }
}
