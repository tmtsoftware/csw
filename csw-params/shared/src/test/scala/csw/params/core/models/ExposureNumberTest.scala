package csw.params.core.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExposureNumberTest extends AnyFunSpec with Matchers {
  describe("create ExposureNumber") {
    it("should create valid ExposureNumber") {
      val exposureNumber = ExposureNumber("0001")
      exposureNumber should ===(ExposureNumber(1))
      exposureNumber.toString should ===("0001")
    }

    it("should create valid ExposureNumber with subArray") {
      val exposureNumber = ExposureNumber("0001-01")
      exposureNumber should ===(ExposureNumber(1, Some(1)))
      exposureNumber.toString should ===("0001-01")
    }

    it("should throw exception if ExposureNumber is invalid") {
      val exception = intercept[IllegalArgumentException](ExposureNumber("10000"))
      exception.getMessage should ===(
        "requirement failed: Invalid exposure number 10000: exposure number should be provided 4 digit with value between 0001 to 1000"
      )
    }

    it("should throw exception if subarray in exposure number is invalid") {
      val exception = intercept[IllegalArgumentException](ExposureNumber("0002-123"))
      exception.getMessage should ===(
        "requirement failed: Invalid subArray 123 SubArray for exposure number should be provided 2 digit with value between 01 to 10"
      )
    }
  }
}
