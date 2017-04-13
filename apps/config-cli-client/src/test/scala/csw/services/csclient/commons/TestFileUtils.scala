package csw.services.csclient.commons

import java.io.File
import java.nio.file.Paths

import csw.services.config.server.Settings

class TestFileUtils(settings: Settings) {

  val repositoryFilePath = "/path/hcd/trombone.conf"
  val inputFilePath = "/tmp/ConfigCliClient/input.txt"
  val outputFilePath = "/tmp/ConfigCliClient/output.txt"
  val id = "1234"
  val comment = "test commit comment!!!"


  def deleteServerFiles(): Unit = {
    val oversizeFileDir = Paths.get(settings.`oversize-files-dir`).toFile
    val tmpDir = Paths.get(settings.`tmp-dir`).toFile
    deleteDirectoryRecursively(tmpDir)
    deleteDirectoryRecursively(oversizeFileDir)
    deleteDirectoryRecursively(settings.repositoryFile)
  }

  /**
    * FOR TESTING: Deletes the contents of the given directory (recursively).
    * This is meant for use by tests that need to always start with an empty Svn repository.
    */
  def deleteDirectoryRecursively(dir: File): Unit = {
    // just to be safe, don't delete anything that is not in /tmp/
    val p = dir.getPath
    if (!p.startsWith("/tmp/") && !p.startsWith(settings.`tmp-dir`))
      throw new RuntimeException(s"Refusing to delete $dir since not in /tmp/ or ${settings.`tmp-dir`}")

    if (dir.isDirectory) {
      dir.list.foreach {
        filePath =>
          val file = new File(dir, filePath)
          if (file.isDirectory) {
            deleteDirectoryRecursively(file)
          } else {
            file.delete()
          }
      }
      dir.delete()
    }
  }

}