package csw.config.commons

import java.nio.file.{Files, Path, Paths}

class ArgsUtil {

  val relativeRepoPath = "/path/hcd/trombone.conf"
  val inputFileContents: String = """
                           |axisName1 = tromboneAxis1
                           |axisName2 = tromboneAxis2
                           |axisName3 = tromboneAxis3
                           |""".stripMargin

  val updatedInputFileContents: String = """
                            |axisName11 = tromboneAxis11
                            |axisName22 = tromboneAxis22
                            |axisName33 = tromboneAxis33
                            |""".stripMargin

  val inputFilePath: String        = createTempFile("input", inputFileContents).toString
  val updatedInputFilePath: String = createTempFile("updated_input", updatedInputFileContents).toString
  val outputFilePath               = "/tmp/output.conf"
  val id                           = "1"
  val comment                      = "test commit comment!!!"
  val maxFileVersions              = 32

  val createAllArgs: List[String]      = List("create", relativeRepoPath, "-i", inputFilePath, "--annex", "-c", comment)
  val createMinimalArgs: List[String]  = List("create", relativeRepoPath, "-i", inputFilePath, "-c", comment)
  val updateAllArgs: List[String]      = List("update", relativeRepoPath, "-i", updatedInputFilePath, "-c", comment)
  val getLatestArgs: List[String]      = List("get", relativeRepoPath, "-o", outputFilePath)
  val getByIdArgs: List[String]        = List("get", relativeRepoPath, "-o", outputFilePath, "--id")
  val getMinimalArgs: List[String]     = List("getActive", relativeRepoPath, "-o", outputFilePath)
  val existsArgs: List[String]         = List("exists", relativeRepoPath)
  val deleteArgs: List[String]         = List("delete", relativeRepoPath, "-c", comment)
  val historyArgs: List[String]        = List("history", relativeRepoPath, "--max", maxFileVersions.toString)
  val historyActiveArgs: List[String]  = List("historyActive", relativeRepoPath, "--max", maxFileVersions.toString)
  val setActiveAllArgs: List[String]   = List("setActiveVersion", relativeRepoPath, "--id", id, "-c", comment)
  val resetActiveAllArgs: List[String] = List("resetActiveVersion", relativeRepoPath, "-c", comment)
  val meteDataArgs: List[String]       = List("getMetadata")
  val loginArgs: List[String]          = List("login")
  val logoutArgs: List[String]         = List("logout")

  private def createTempFile(fileName: String, fileContent: String): Path =
    Files.writeString(Files.createTempFile(fileName, ".conf"), fileContent)

  def readFile(filePath: String): String = Files.readString(Paths.get(filePath))
}
