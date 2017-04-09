package csw.services.config.server.repo

import csw.services.config.server.Settings
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.io.SVNRepositoryFactory

import scala.util.control.NonFatal

class SvnAdmin(settings: Settings) {
  /**
    * Initializes an svn repository in the given dir.
    */
  def initSvnRepo(): Unit = try {
    // Create the new main repo
    FSRepositoryFactory.setup()
    SVNRepositoryFactory.createLocalRepository(settings.repositoryFile, false, false)
  } catch {
    case NonFatal(ex) ⇒ ex.printStackTrace()
  }
}
