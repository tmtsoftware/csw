package csw.services.config.server.commons

import java.nio.file.Paths

import csw.services.config.server.commons.PathValidator.RichPath
import org.scalatest.{FunSuite, Matchers}

class PathValidatorTest extends FunSuite with Matchers {

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
      path.isValid() shouldBe false
    }
  }

  test("should return true for valid file path") {
    Paths.get("/validpath/sample.txt").isValid() shouldBe true
  }
}
