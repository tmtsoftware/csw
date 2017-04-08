package csw.services.config.repo

import java.io.File

import csw.services.config.Settings
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.io.SVNRepositoryFactory

class SvnAdmin(settings: Settings) {

  /**
    * Initializes an svn repository in the given dir.
    */
  def initSvnRepo(): Unit = {
    // Create the new main repo
    FSRepositoryFactory.setup()
    SVNRepositoryFactory.createLocalRepository(settings.repositoryFile, false, true)
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
