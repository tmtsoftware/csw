package csw.params.core.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ObsIdTest extends AnyFunSpec with Matchers {
  describe("create ObsId") {
    it("should create valid obsId") {
      val obsId = ObsId("2020A-P001-O123")
      obsId.programId should ===(ProgramId("2020A-P001"))
      obsId.observationNumber should ===("O123")
    }

    it("should throw exception if program Id is invalid") {
      val exception = intercept[IllegalArgumentException](ObsId("2020A-001-O123"))
      exception.getMessage should ===("requirement failed: Program Number should start with letter 'P'")
    }

    it("should throw exception if observation number fixed part is invalid") {
      val exception = intercept[IllegalArgumentException](ObsId("2020A-P001-123"))
      exception.getMessage should ===("requirement failed: Observation Number should start with letter 'O'")
    }

    it("should throw exception if observation number is invalid") {
      val exception = intercept[IllegalArgumentException](ObsId("2020A-P001-O23"))
      exception.getMessage should ===(
        "requirement failed: Observation Number should be valid three digit integer prefixed with letter 'O' ex: O123, O001 etc"
      )
    }

    it("should throw exception if observation id is invalid") {
      val exception = intercept[IllegalArgumentException](ObsId("2020A-P001"))
      exception.getMessage should ===(
        "requirement failed: ObsId must form with semsterId, programNumer, observationNumber separated with '-' ex: 2020A-P001-O123"
      )
    }
  }
}
