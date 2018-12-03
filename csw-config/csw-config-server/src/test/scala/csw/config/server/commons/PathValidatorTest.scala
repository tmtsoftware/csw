package csw.config.server.commons

import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-47: Unique name for configuration file
// DEOPSCSW-135: Validation of suffix for active and sha files
class PathValidatorTest extends FunSuite with Matchers {

  test("should return false for invalid path") {

    val paths = List(
      "/invalidpath!/sample.txt",
      "/invalidpath#/sample.txt",
      "/invalidpath$/sample.txt",
      "/invalidpath/%sample.txt",
      "/invalidpath/&sample.txt",
      "/invalidpath/sa'mple.txt",
      "/invalidpath/samp@le.txt",
      "/invalidpath/samp`le.txt",
      "/invalid+path/sample.txt",
      "/invalid,path/sample.txt",
      "/invalidpath;/sample.txt",
      "/invalidpath/sam=ple.txt",
      "/invalid path/sample.txt",
      "/invalidpath/<sample.txt",
      "/invalidpath/sample>.txt"
    )

    paths.foreach { path ⇒
      PathValidator.isValid(path) shouldBe false
    }
  }

  test("should return true for valid file path") {
    PathValidator.isValid("/validpath/sample.txt") shouldBe true
  }
}
