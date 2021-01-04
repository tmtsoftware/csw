package csw.params.core.models

import java.time.{DateTimeException, Year}

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ProgramIdTest extends AnyFunSpec with Matchers {
  describe("Create ProgramId") {
    it("should create ProgramId") {
      val programId = ProgramId(SemesterId("2030A"), 123)
      programId.semesterId should ===(SemesterId(Year.parse("2030"), Semester.A))
      programId.programNumber should ===(123)
      programId.toString should ===("2030A-123")
    }

    it("should throw exception if invalid program id") {
      a[IllegalArgumentException] shouldBe thrownBy(ProgramId(SemesterId("2020A"), 1234))
    }

    it("should throw exception if invalid semester id") {
      a[DateTimeException] shouldBe thrownBy(ProgramId(SemesterId("202A"), 123))
    }
  }
}
