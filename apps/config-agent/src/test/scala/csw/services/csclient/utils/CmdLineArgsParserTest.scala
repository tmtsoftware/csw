package csw.services.csclient.utils

import java.io.File

import csw.services.csclient.models.Options
import org.scalatest.{FunSuite, Matchers}

class CmdLineArgsParserTest
  extends FunSuite
    with Matchers{

  val repositoryFilepath = new File("path/in/repository")
  val inputFile = new File("/tmp/some/input/file")
  val outputFile = new File("/tmp/some/output/file")
  val id = "1234"
  val comment = "test commit comment!!!"

  test("test arguments without specifying operation") {
    val argv = Array("", repositoryFilepath.toString, "-o", outputFile.toString)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test create with no sub-options") {
    val argv = Array("create")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test create with all applicable sub-options") {
    val argv = Array("create", repositoryFilepath.toString, "-i", inputFile.toString, "--oversize", "-c", comment)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("create", Some(repositoryFilepath), Some(inputFile), None, None, true, comment))
  }

  test("test create with bare minimum sub-options") {
    val argv = Array("create", repositoryFilepath.toString, "-i", inputFile.toString)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("create", Some(repositoryFilepath), Some(inputFile), None, None, false, ""))
  }

  test("test update with no sub-options") {
    val argv = Array("update")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test update with all applicable sub-options") {
    val argv = Array("update", repositoryFilepath.toString, "-i", inputFile.toString, "-c", comment)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("update", Some(repositoryFilepath), Some(inputFile), None, None, false, comment))
  }

  test("test update with bare minimum sub-options") {
    val argv = Array("update", repositoryFilepath.toString, "-i", inputFile.toString)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("update", Some(repositoryFilepath), Some(inputFile), None, None, false, ""))
  }

  test("test get with no sub-options") {
    val argv = Array("get")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test get with all applicable sub-options") {
    val argv = Array("get", repositoryFilepath.toString, "-o", outputFile.toString, "--id", id)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("get", Some(repositoryFilepath), None, Some(outputFile), Some(id), false, ""))
  }

  test("test get with bare minimum sub-options") {
    val argv = Array("get", repositoryFilepath.toString, "-o", outputFile.toString)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("get", Some(repositoryFilepath), None, Some(outputFile), None, false, ""))
  }

  test("test exists with no sub-options") {
    val argv = Array("exists")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test exists with sub-options") {
    val argv = Array("exists", repositoryFilepath.toString)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("exists", Some(repositoryFilepath), None, None, None, false, ""))
  }

  test("test delete with no sub-options") {
    val argv = Array("delete")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test delete with sub-options") {
    val argv = Array("delete", repositoryFilepath.toString)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("delete", Some(repositoryFilepath), None, None, None, false, ""))
  }

  test("test list") {
    val argv = Array("list")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("list", None, None, None, None, false, ""))
  }

  test("test history with no sub-options") {
    val argv = Array("history")
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x shouldEqual None
  }

  test("test history with sub-options") {
    val argv = Array("history", repositoryFilepath.toString)
    val x: Option[Options] = CmdLineArgsParser.parser.parse(argv, Options())
    x should contain (new Options("history", Some(repositoryFilepath), None, None, None, false, ""))
  }
}