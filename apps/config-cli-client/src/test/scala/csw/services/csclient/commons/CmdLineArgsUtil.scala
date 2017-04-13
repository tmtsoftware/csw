package csw.services.csclient.commons

import java.io.{BufferedWriter, File, FileWriter}

object CmdLineArgsUtil {

  val repositoryFilePath = "/path/hcd/trombone.conf"
  val inputFileContents ="""
                           |axisName1 = tromboneAxis1
                           |axisName2 = tromboneAxis2
                           |axisName3 = tromboneAxis3
                           |""".stripMargin

  val updatedInputFileContents ="""
                            |axisName11 = tromboneAxis11
                            |axisName22 = tromboneAxis22
                            |axisName33 = tromboneAxis33
                            |""".stripMargin

  val inputFilePath = createTempFile("input", inputFileContents).toPath.toString
  val updatedInputFilePath = createTempFile("updated_input", updatedInputFileContents).toPath.toString
  val outputFilePath = "/tmp/output.conf"
  val id = "1"
  val comment = "test commit comment!!!"
  val maxFileVersions = 32

  val createAllArgs = Array("create", repositoryFilePath, "-i", inputFilePath, "--oversize", "-c", comment)
  val createMinimalArgs = Array("create", repositoryFilePath, "-i", inputFilePath)
  val updateAllArgs = Array("update", repositoryFilePath, "-i", updatedInputFilePath, "-c", comment)
  val updateMinimalArgs = Array("update", repositoryFilePath, "-i", updatedInputFileContents)
  val getAllArgs = Array("get", repositoryFilePath, "-o", outputFilePath, "--id", id)
  val getMinimalArgs = Array("get", repositoryFilePath, "-o", outputFilePath)
  val existsArgs = Array("exists", repositoryFilePath)
  val deleteArgs = Array("delete", repositoryFilePath)
  val listArgs = Array("list")
  val historyArgs = Array("history", repositoryFilePath, "--max", maxFileVersions.toString)
  val setDefaultAllArgs = Array("setDefault", repositoryFilePath, "--id", id)
  val setDefaultMinimalArgs = Array("setDefault", repositoryFilePath)
  val resetDefaultArgs = Array("resetDefault", repositoryFilePath)
  val getDefaultArgs = Array("getDefault", repositoryFilePath, "-o", outputFilePath)

  private def createTempFile(fileName: String, fileContent: String): File = {
    val tempFile = java.io.File.createTempFile(fileName, ".conf")
    val bw = new BufferedWriter(new FileWriter(tempFile))
    bw.write(fileContent)
    bw.close()
    tempFile
  }

}
