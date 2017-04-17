package csw.services.csclient.utils

import java.nio.file.Paths
import java.time.Instant

import csw.services.csclient.models.Options
import org.scalatest.{FunSuite, Matchers}

class CmdLineArgsParserTest
  extends FunSuite
    with Matchers{

  val repositoryFilePath = "path/in/repository"
  val inputFilePath = "/tmp/some/input/file"
  val outputFilePath = "/tmp/some/output/file"
  val id = "1234"
  val date = Instant.now().toString
  val comment = "test commit comment!!!"
  val maxFileVersions = 32

  test("test arguments without specifying operation") {
    val argv = Array("", repositoryFilePath, "-o", outputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test create with no sub-options") {
    val argv = Array("create")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test create with all applicable sub-options") {
    val argv = Array("create", repositoryFilePath, "-i", inputFilePath, "--oversize", "-c", comment)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("create", Some(Paths.get(repositoryFilePath)), Some(Paths.get(inputFilePath)), None, None, None, Int.MaxValue, true, comment))
  }

  test("test create with bare minimum sub-options") {
    val argv = Array("create", repositoryFilePath, "-i", inputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("create", Some(Paths.get(repositoryFilePath)), Some(Paths.get(inputFilePath)), None, None, None, Int.MaxValue, false, ""))
  }

  test("test update with no sub-options") {
    val argv = Array("update")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test update with all applicable sub-options") {
    val argv = Array("update", repositoryFilePath, "-i", inputFilePath, "-c", comment)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("update", Some(Paths.get(repositoryFilePath)), Some(Paths.get(inputFilePath)), None, None, None, Int.MaxValue, false, comment))
  }

  test("test update with bare minimum sub-options") {
    val argv = Array("update", repositoryFilePath, "-i", inputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("update", Some(Paths.get(repositoryFilePath)), Some(Paths.get(inputFilePath)), None, None, None, Int.MaxValue, false, ""))
  }

  test("test get with no sub-options") {
    val argv = Array("get")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test get with all applicable sub-options") {
    val argv = Array("get", repositoryFilePath, "-o", outputFilePath, "--id", id)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("get", Some(Paths.get(repositoryFilePath)), None, Some(Paths.get(outputFilePath)), Some(id), None, Int.MaxValue, false, ""))
  }

  test("test get with date sub-option") {
    val argv = Array("get", repositoryFilePath, "-o", outputFilePath, "--date", date)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("get", Some(Paths.get(repositoryFilePath)), None, Some(Paths.get(outputFilePath)), None, Some(Instant.parse(date)), Int.MaxValue, false, ""))
  }

  test("test get with bare minimum sub-options") {
    val argv = Array("get", repositoryFilePath, "-o", outputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("get", Some(Paths.get(repositoryFilePath)), None, Some(Paths.get(outputFilePath)), None, None, Int.MaxValue, false, ""))
  }

  test("test exists with no sub-options") {
    val argv = Array("exists")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test exists with sub-options") {
    val argv = Array("exists", repositoryFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("exists", Some(Paths.get(repositoryFilePath)), None, None, None, None, Int.MaxValue, false, ""))
  }

  test("test delete with no sub-options") {
    val argv = Array("delete")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test delete with sub-options") {
    val argv = Array("delete", repositoryFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("delete", Some(Paths.get(repositoryFilePath)), None, None, None, None, Int.MaxValue, false, ""))
  }

  test("test list") {
    val argv = Array("list")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("list", None, None, None, None, None, Int.MaxValue, false, ""))
  }

  test("test history with no sub-options") {
    val argv = Array("history")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test history with sub-options") {
    val argv = Array("history", repositoryFilePath, "--max", maxFileVersions.toString)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("history", Some(Paths.get(repositoryFilePath)), None, None, None, None, maxFileVersions, false, ""))
  }

  test("test setDefault with no sub-options") {
    val argv = Array("setDefault")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test setDefault with all sub-options") {
    val argv = Array("setDefault", repositoryFilePath, "--id", id)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("setDefault", Some(Paths.get(repositoryFilePath)), None, None, Some(id), None, Int.MaxValue, false, ""))
  }

  test("test setDefault with bare minimum sub-options") {
    val argv = Array("setDefault", repositoryFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("setDefault", Some(Paths.get(repositoryFilePath)), None, None, None, None, Int.MaxValue, false, ""))
  }

  test("test resetDefault with no sub-options") {
    val argv = Array("resetDefault")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test resetDefault with sub-options") {
    val argv = Array("resetDefault", repositoryFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("resetDefault", Some(Paths.get(repositoryFilePath)), None, None, None, None, Int.MaxValue, false, ""))
  }

  test("test getDefault with no sub-options") {
    val argv = Array("getDefault")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test getDefault with sub-options") {
    val argv = Array("getDefault", repositoryFilePath, "-o", outputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("getDefault", Some(Paths.get(repositoryFilePath)), None, Some(Paths.get(outputFilePath)), None, None, Int.MaxValue, false, ""))
  }
}