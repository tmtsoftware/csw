package csw.config.commons

import java.nio.file.{Files, Path, Paths}

class ArgsUtil {

  val relativeRepoPath          = "/path/hcd/trombone.conf"
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

  val createAllArgs      = Array("create", relativeRepoPath, "-i", inputFilePath, "--annex", "-c", comment)
  val createMinimalArgs  = Array("create", relativeRepoPath, "-i", inputFilePath, "-c", comment)
  val updateAllArgs      = Array("update", relativeRepoPath, "-i", updatedInputFilePath, "-c", comment)
  val getLatestArgs      = Array("get", relativeRepoPath, "-o", outputFilePath)
  val getByIdArgs        = Array("get", relativeRepoPath, "-o", outputFilePath, "--id")
  val getMinimalArgs     = Array("getActive", relativeRepoPath, "-o", outputFilePath)
  val existsArgs         = Array("exists", relativeRepoPath)
  val deleteArgs         = Array("delete", relativeRepoPath, "-c", comment)
  val historyArgs        = Array("history", relativeRepoPath, "--max", maxFileVersions.toString)
  val historyActiveArgs  = Array("historyActive", relativeRepoPath, "--max", maxFileVersions.toString)
  val setActiveAllArgs   = Array("setActiveVersion", relativeRepoPath, "--id", id, "-c", comment)
  val resetActiveAllArgs = Array("resetActiveVersion", relativeRepoPath, "-c", comment)
  val meteDataArgs       = Array("getMetadata")
  val loginArgs          = Array("login")
  val logoutArgs         = Array("logout")

  private def createTempFile(fileName: String, fileContent: String): Path =
    Files.write(Files.createTempFile(fileName, ".conf"), fileContent.getBytes)

  def readFile(filePath: String): String =
    new String(Files.readAllBytes(Paths.get(filePath)))
}
