package csw.params.core.models

import org.scalatest.{FunSpec, Matchers}

class ObsId2Tests extends FunSpec with Matchers {

  val obsID1 = "2022A-Q-P012-O123"       // without file
  val obsID2 = "2022A-C-P014-O123-I0234" // with file

  describe("Basic ObsID Test") {
    it("Should create obsId with no file") {
      val oid = ObsId3(obsID1)
      oid.isValid shouldBe true
      oid.semester shouldBe "2022A"
      oid.year shouldBe "2022"
      oid.whichSemester shouldBe "A"
      oid.programType shouldBe "Q"
      oid.program shouldBe "P012"
      oid.observation shouldBe "O123"
      oid.hasFile shouldBe false
      oid.file shouldBe ""
      oid.detector shouldBe ""
    }

    it("Should create obsId with file") {
      val oid = ObsId3(obsID2)
      oid.isValid shouldBe true
      oid.semester shouldBe "2022A"
      oid.year shouldBe "2022"
      oid.whichSemester shouldBe "A"
      oid.programType shouldBe "C"
      oid.program shouldBe "P014"
      oid.observation shouldBe "O123"
      oid.hasFile shouldBe true
      oid.file shouldBe "0234"
      oid.detector shouldBe "I"
    }

    it("Shouldn't be a fail with a string") {
      val oid = ObsId3("A")
      oid.toString shouldBe "A"
      ObsId3("2022A-Q-P012-O123-0001").toString shouldBe "2022A-Q-P012-O123-0001"
      ObsId3("2022A-Q-P012-O123-A0001").toString shouldBe "2022A-Q-P012-O123-A0001"
      ObsId3("2022A-Q-P012-O123-A").toString shouldBe "2022A-Q-P012-O123" // No good file info
    }

    it("Should obsId fail with det and no/yes file") {
      ObsId3("2022A-Q-P012-O123-A0001").detector shouldEqual "A"
      ObsId3("2022A-Q-P012-O123-A0001").file shouldEqual "0001"
      ObsId3("2022A-Q-P012-O123-0001").detector shouldEqual ""
      ObsId3("2022A-Q-P012-O123-0001").file shouldEqual "0001"
      ObsId3("2022A-Q-P012-O123-A").detector shouldEqual ""
      ObsId3("2022A-Q-P012-O123-A").file shouldEqual ""
    }

    it("Should create obsId with det and file") {
      ObsId2.create("2022A-Q-P012-O123-A0001") shouldBe ObsId2("2022", "A", "Q", "012", "123", Some("A0001"))
    }

    it("Should allow obsId with file but no det") {
      ObsId2.create("2022A-Q-P012-O123-0001") shouldBe ObsId2("2022", "A", "Q", "012", "123", Some("0001"))
    }
  }
}
