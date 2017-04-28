package csw.services.config.server.commons

import java.nio.file.Paths

import org.scalatest.{FunSuite, Matchers}

class PathValidatorTest extends FunSuite with Matchers {

  // DEOPSCSW-47: Unique name for configuration file
  test("should return false for invalid path") {

    val paths = List(
      Paths.get("/invalidpath!/sample.txt"),
      Paths.get("/invalidpath#/sample.txt"),
      Paths.get("/invalidpath$/sample.txt"),
      Paths.get("/invalidpath/%sample.txt"),
      Paths.get("/invalidpath/&sample.txt"),
      Paths.get("/invalidpath/sa'mple.txt"),
      Paths.get("/invalidpath/samp@le.txt"),
      Paths.get("/invalidpath/samp`le.txt"),
      Paths.get("/invalid+path/sample.txt"),
      Paths.get("/invalid,path/sample.txt"),
      Paths.get("/invalidpath;/sample.txt"),
      Paths.get("/invalidpath/sam=ple.txt"),
      Paths.get("/invalid path/sample.txt"),
      Paths.get("/invalidpath/<sample.txt"),
      Paths.get("/invalidpath/sample>.txt")
    )

    paths.foreach { path ⇒
      PathValidator.isValid(path) shouldBe false
    }
  }

  test("should return true for valid file path") {
    PathValidator.isValid(Paths.get("/validpath/sample.txt")) shouldBe true
  }
}
