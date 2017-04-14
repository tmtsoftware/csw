package csw.services.config.api.commons

import java.io.File
import java.nio.file.Paths

import csw.services.config.server.Settings

class TestFileUtils(settings: Settings) {

  def deleteServerFiles(): Unit = {
    val oversizeFileDir = Paths.get(settings.`oversize-files-dir`).toFile
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
    if (!p.startsWith("/tmp/"))
      throw new RuntimeException(s"Refusing to delete $dir since not in /tmp/")

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
