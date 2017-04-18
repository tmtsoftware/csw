package csw.services.csclient.commons

import java.io.{BufferedWriter, File, FileWriter}

object CmdLineArgsUtil {

  val relativeRepoPath = "/path/hcd/trombone.conf"
  val inputFileContents: String ="""
                           |axisName1 = tromboneAxis1
                           |axisName2 = tromboneAxis2
                           |axisName3 = tromboneAxis3
                           |""".stripMargin

  val updatedInputFileContents: String ="""
                            |axisName11 = tromboneAxis11
                            |axisName22 = tromboneAxis22
                            |axisName33 = tromboneAxis33
                            |""".stripMargin

  val inputFilePath: String = createTempFile("input", inputFileContents).toPath.toString
  val updatedInputFilePath: String = createTempFile("updated_input", updatedInputFileContents).toPath.toString
  val outputFilePath = "/tmp/output.conf"
  val id = "1"
  val comment = "test commit comment!!!"
  val maxFileVersions = 32

  val createAllArgs = Array("create", relativeRepoPath, "-i", inputFilePath, "--oversize", "-c", comment)
  val createMinimalArgs = Array("create", relativeRepoPath, "-i", inputFilePath)
  val updateAllArgs = Array("update", relativeRepoPath, "-i", updatedInputFilePath, "-c", comment)
  val updateMinimalArgs = Array("update", relativeRepoPath, "-i", updatedInputFileContents)
  val getAllArgs = Array("get", relativeRepoPath, "-o", outputFilePath, "--id", id)
  val getMinimalArgs = Array("get", relativeRepoPath, "-o", outputFilePath)
  val existsArgs = Array("exists", relativeRepoPath)
  val deleteArgs = Array("delete", relativeRepoPath)
  val listArgs = Array("list")
  val historyArgs = Array("history", relativeRepoPath, "--max", maxFileVersions.toString)
  val setDefaultAllArgs = Array("setDefault", relativeRepoPath, "--id", id)
  val setDefaultMinimalArgs = Array("setDefault", relativeRepoPath)
  val getDefaultArgs = Array("getDefault", relativeRepoPath, "-o", outputFilePath)

  private def createTempFile(fileName: String, fileContent: String): File = {
    val tempFile = java.io.File.createTempFile(fileName, ".conf")
    val bw = new BufferedWriter(new FileWriter(tempFile))
    bw.write(fileContent)
    bw.close()
    tempFile
  }

}
