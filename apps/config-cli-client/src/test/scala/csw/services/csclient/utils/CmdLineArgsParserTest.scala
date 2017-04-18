package csw.services.csclient.utils

import java.nio.file.Paths
import java.time.Instant

import csw.services.csclient.models.Options
import org.scalatest.{FunSuite, Matchers}

class CmdLineArgsParserTest
  extends FunSuite
    with Matchers{

  val relativeRepoPath = "path/in/repository"
  val inputFilePath = "/tmp/some/input/file"
  val outputFilePath = "/tmp/some/output/file"
  val id = "1234"
  val date: String = Instant.now().toString
  val comment = "test commit comment!!!"
  val maxFileVersions = 32

  test("test arguments without specifying operation") {
    val argv = Array("", relativeRepoPath, "-o", outputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test create with no sub-options") {
    val argv = Array("create")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test create with all applicable sub-options") {
    val argv = Array("create", relativeRepoPath, "-i", inputFilePath, "--oversize", "-c", comment)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("create", Some(Paths.get(relativeRepoPath)), Some(Paths.get(inputFilePath)), None, None, None, Int.MaxValue, oversize = true, comment))
  }

  test("test create with bare minimum sub-options") {
    val argv = Array("create", relativeRepoPath, "-i", inputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("create", Some(Paths.get(relativeRepoPath)), Some(Paths.get(inputFilePath)), None, None, None, Int.MaxValue, oversize = false, ""))
  }

  test("test update with no sub-options") {
    val argv = Array("update")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test update with all applicable sub-options") {
    val argv = Array("update", relativeRepoPath, "-i", inputFilePath, "-c", comment)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("update", Some(Paths.get(relativeRepoPath)), Some(Paths.get(inputFilePath)), None, None, None, Int.MaxValue, oversize = false, comment))
  }

  test("test update with bare minimum sub-options") {
    val argv = Array("update", relativeRepoPath, "-i", inputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("update", Some(Paths.get(relativeRepoPath)), Some(Paths.get(inputFilePath)), None, None, None, Int.MaxValue, oversize = false))
  }

  test("test get with no sub-options") {
    val argv = Array("get")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test get with all applicable sub-options") {
    val argv = Array("get", relativeRepoPath, "-o", outputFilePath, "--id", id)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("get", Some(Paths.get(relativeRepoPath)), None, Some(Paths.get(outputFilePath)), Some(id), None, Int.MaxValue))
  }

  test("test get with date sub-option") {
    val argv = Array("get", relativeRepoPath, "-o", outputFilePath, "--date", date)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("get", Some(Paths.get(relativeRepoPath)), None, Some(Paths.get(outputFilePath)), None, Some(Instant.parse(date)), Int.MaxValue, oversize = false, ""))
  }

  test("test get with bare minimum sub-options") {
    val argv = Array("get", relativeRepoPath, "-o", outputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("get", Some(Paths.get(relativeRepoPath)), None, Some(Paths.get(outputFilePath)), None, None, Int.MaxValue))
  }

  test("test exists with no sub-options") {
    val argv = Array("exists")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test exists with sub-options") {
    val argv = Array("exists", relativeRepoPath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("exists", Some(Paths.get(relativeRepoPath)), None, None, None, None, Int.MaxValue))
  }

  test("test delete with no sub-options") {
    val argv = Array("delete")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test delete with sub-options") {
    val argv = Array("delete", relativeRepoPath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("delete", Some(Paths.get(relativeRepoPath)), None, None, None, None, Int.MaxValue))
  }

  test("test list") {
    val argv = Array("list")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("list", None, None, None, None, None, Int.MaxValue))
  }

  test("test history with no sub-options") {
    val argv = Array("history")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test history with sub-options") {
    val argv = Array("history", relativeRepoPath, "--max", maxFileVersions.toString)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("history", Some(Paths.get(relativeRepoPath)), None, None, None, None, maxFileVersions, oversize = false))
  }

  test("test setDefault with no sub-options") {
    val argv = Array("setDefault")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test setDefault with all sub-options") {
    val argv = Array("setDefault", relativeRepoPath, "--id", id)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("setDefault", Some(Paths.get(relativeRepoPath)), None, None, Some(id), None, Int.MaxValue, oversize = false, ""))
  }

  test("test setDefault with bare minimum sub-options") {
    val argv = Array("setDefault", relativeRepoPath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("setDefault", Some(Paths.get(relativeRepoPath)), None, None, None, None, Int.MaxValue, oversize = false, ""))
  }

  test("test resetDefault with no sub-options") {
    val argv = Array("resetDefault")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test resetDefault with sub-options") {
    val argv = Array("resetDefault", relativeRepoPath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("resetDefault", Some(Paths.get(relativeRepoPath)), None, None, None, None, Int.MaxValue, oversize = false, ""))
  }

  test("test getDefault with no sub-options") {
    val argv = Array("getDefault")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test getDefault with sub-options") {
    val argv = Array("getDefault", relativeRepoPath, "-o", outputFilePath)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (Options("getDefault", Some(Paths.get(relativeRepoPath)), None, Some(Paths.get(outputFilePath)), None, None, Int.MaxValue, oversize = false, ""))
  }
}