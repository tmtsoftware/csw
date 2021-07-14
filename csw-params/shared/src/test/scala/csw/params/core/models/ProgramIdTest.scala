package csw.params.core.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ProgramIdTest extends AnyFunSpec with Matchers {
  describe("Create ProgramId") {
    it("should create ProgramId | CSW-121") {
      val programId = ProgramId(SemesterId("2030A"), 1)
      programId should ===(ProgramId("2030A-001"))
      programId.semesterId should ===(SemesterId("2030A"))
      programId.programNumber should ===(1)
      programId.toString should ===("2030A-001")
    }

    it("should throw exception if invalid program id | CSW-121") {
      val exception = intercept[IllegalArgumentException](ProgramId("2020A-1234"))
      exception.getMessage should ===("requirement failed: Program Number should be integer in the range of 1 to 999")
    }

    it("should throw exception if invalid semester in SemesterId | CSW-121") {
      val exception = intercept[IllegalArgumentException](ProgramId("202C-123"))
      exception.getMessage should ===("Failed to parse semester C: C is not a member of Enum (A, B)")
    }

    it("should throw exception if program id is invalid | CSW-121") {
      val exception = intercept[IllegalArgumentException](ProgramId("2020A-001-123"))
      exception.getMessage should ===(
        "A program Id consists of a semester Id and program number separated by '-' ex: 2020A-001"
      )
    }
  }
}
