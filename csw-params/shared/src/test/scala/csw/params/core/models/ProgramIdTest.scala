package csw.params.core.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ProgramIdTest extends AnyFunSpec with Matchers {
  describe("Create ProgramId") {
    it("should create ProgramId") {
      val programId = ProgramId(SemesterId("2030A"), 1)
      programId should ===(ProgramId("2030A-001"))
      programId.semesterId should ===(SemesterId("2030A"))
      programId.programNumber should ===(1)
      programId.toString should ===("2030A-001")
    }

    it("should throw exception if invalid program id") {
      val exception = intercept[IllegalArgumentException](ProgramId("2020A-1234"))
      exception.getMessage should ===("requirement failed: Program Number should be integer in the range of 1 to 999")
    }

    it("should throw exception if invalid semester in SemesterId") {
      a[NoSuchElementException] shouldBe thrownBy(ProgramId("202C-123"))
    }

    it("should throw exception if program id is invalid") {
      val exception = intercept[IllegalArgumentException](ProgramId("2020A"))
      exception.getMessage should ===(
        "requirement failed: ProgramId must form with semesterId, programNumber separated with '-' ex: 2020A-001"
      )
    }
  }
}
