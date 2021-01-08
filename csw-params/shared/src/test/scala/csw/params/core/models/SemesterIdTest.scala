package csw.params.core.models

import java.time.Year

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class SemesterIdTest extends AnyFunSpec with Matchers {
  describe("Create SemesterId") {
    it("should create semesterId with valid year and semester") {
      val semesterId = SemesterId("2010B")
      semesterId.toString should ===("2010B")
      semesterId.year should ===(Year.of(2010))
      semesterId.semester should ===(Semester.B)
    }

    it("should throw exception if semester is invalid") {
      a[NoSuchElementException] shouldBe thrownBy(SemesterId("2010C"))
    }
  }
}
