package csw.services.config.server.files

import java.nio.file.Paths

import csw.services.config.api.exceptions.InvalidFilePath
import csw.services.config.server.files.PathExt.RichPath
import org.scalatest.FunSuite

class PathExtTest extends FunSuite {

  test("should throw InvalidFilePath exception for invalid path") {

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
      Paths.get("/invalid path/sample.txt")
    )

    paths.foreach { path ⇒
      intercept[InvalidFilePath](path.validate)
    }
  }
}
