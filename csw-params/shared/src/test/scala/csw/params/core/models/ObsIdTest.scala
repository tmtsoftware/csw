package csw.params.core.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ObsIdTest extends AnyFunSpec with Matchers {
  describe("create ObsId") {
    it("should create valid obsId") {
      val obsId = ObsId("2020A-001-123")
      obsId.programId should ===(ProgramId("2020A-001"))
      obsId.observationNumber should ===(123)
      obsId.toString should ===("2020A-001-123")
    }

    it("should throw exception if program Id is invalid") {
      val exception = intercept[IllegalArgumentException](ObsId("2020A-1234-123"))
      exception.getMessage should ===("requirement failed: Program Number should be integer in the range of 1 to 999")
    }

    it("should throw exception if observation number is invalid") {
      val exception = intercept[IllegalArgumentException](ObsId("2020A-001-2334"))
      exception.getMessage should ===("requirement failed: Program Number should be integer in the range of 1 to 999")
    }

    it("should throw exception if observation id is invalid") {
      val exception = intercept[IllegalArgumentException](ObsId("2020A-001"))
      exception.getMessage should ===(
        "requirement failed: ObsId must form with semsterId, programNumer, observationNumber separated with '-' ex: 2020A-001-123"
      )
    }
  }
}
