package csw.services.config.server.repo

import csw.services.config.server.Settings
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.io.SVNRepositoryFactory

class SvnAdmin(settings: Settings) {
  /**
    * Initializes an svn repository in the given dir.
    */
  def initSvnRepo(): Unit = {
    // Create the new main repo
    FSRepositoryFactory.setup()
    SVNRepositoryFactory.createLocalRepository(settings.repositoryFile, false, false)
  }
}
