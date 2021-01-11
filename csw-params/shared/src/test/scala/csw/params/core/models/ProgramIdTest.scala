package csw.params.core.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ProgramIdTest extends AnyFunSpec with Matchers {
  describe("Create ProgramId") {
    it("should create ProgramId") {
      val programId = ProgramId(SemesterId("2030A"), "P001")
      programId should ===(ProgramId("2030A-P001"))
      programId.semesterId should ===(SemesterId("2030A"))
      programId.programNumber should ===("P001")
      programId.toString should ===("2030A-P001")
    }

    it("should throw exception if invalid program id") {
      val exception = intercept[IllegalArgumentException](ProgramId(SemesterId("2020A"), "1234"))
      exception.getMessage should ===("requirement failed: Program Number should start with letter 'P'")
    }

    it("should throw exception if invalid semester in SemesterId") {
      a[NoSuchElementException] shouldBe thrownBy(ProgramId(SemesterId("202C"), "P123"))
    }

    it("should throw exception if program id is invalid") {
      val exception = intercept[IllegalArgumentException](ProgramId("2020A"))
      exception.getMessage should ===(
        "requirement failed: ProgramId must form with semsterId, programNumer separated with '-' ex: 2020A-P001"
      )
    }
  }
}
