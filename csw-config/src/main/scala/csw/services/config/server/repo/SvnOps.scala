package csw.services.config.server.repo

import java.io.{InputStream, OutputStream}
import java.nio.file.Path

import akka.Done
import akka.dispatch.MessageDispatcher
import csw.services.config.server.Settings
import org.tmatesoft.svn.core.{SVNCommitInfo, SVNNodeKind}
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator
import org.tmatesoft.svn.core.io.{SVNRepository, SVNRepositoryFactory}

import scala.concurrent.Future
import scala.util.control.NonFatal

class SvnOps(settings: Settings, dispatcher: MessageDispatcher) {

  private implicit val blockingIoDispatcher = dispatcher

  def getFile(path: Path, revision: Long, outputStream: OutputStream, onError: Throwable ⇒ Unit): Future[Done] = {
    val svn = svnHandle()

    Future {
      svn.getFile(path.toString, revision, null, outputStream)
      outputStream.flush()
      outputStream.close()
    } recover {
      case NonFatal(ex) ⇒ onError(ex)
    } map { _ ⇒
      svn.closeSession()
      Done
    }
  }

  // Adds the given file (and dir if needed) to svn.
  // See http://svn.svnkit.com/repos/svnkit/tags/1.3.5/doc/examples/src/org/tmatesoft/svn/examples/repository/Commit.java.
  def addFile(comment: String, path: Path, data: InputStream): Future[SVNCommitInfo] = Future {
    val svn = svnHandle()
    try {
      val editor = svn.getCommitEditor(comment, null)
      editor.openRoot(SVNRepository.INVALID_REVISION)
      val dirPath = path.getParent

      // Recursively add any missing directories leading to the file
      def addDir(dir: Path): Unit = {
        if (dir != null) {
          addDir(dir.getParent)
          if (!dirExists(dir)) {
            editor.addDir(dir.toString, null, SVNRepository.INVALID_REVISION)
          }
        }
      }

      addDir(dirPath)
      val filePath = path.toString
      editor.addFile(filePath, null, SVNRepository.INVALID_REVISION)
      editor.applyTextDelta(filePath, null)
      val deltaGenerator = new SVNDeltaGenerator
      val checksum = deltaGenerator.sendDelta(filePath, data, editor, true)
      editor.closeFile(filePath, checksum)
      editor.closeDir() // XXX TODO I think all added parent dirs need to be closed also
      editor.closeEdit()
    } finally {
      svn.closeSession()
    }
  }

  // True if the directory path exists in the repository
  private def dirExists(path: Path): Boolean = checkPath(path, SVNNodeKind.DIR)
  
  private def checkFilePath(path: Path): Boolean = checkPath(path, SVNNodeKind.FILE)

  private def checkPath(path: Path, kind: SVNNodeKind): Boolean = {
    val svn = svnHandle()
    try {
      svn.checkPath(path.toString, SVNRepository.INVALID_REVISION) == kind
    } finally {
      svn.closeSession()
    }
  }


  // Gets an object for accessing the svn repository (not reusing a single instance since not thread safe)
  private def svnHandle(): SVNRepository = {
    val svn = SVNRepositoryFactory.create(settings.svnUrl)
    val authManager = BasicAuthenticationManager.newInstance(settings.`svn-user-name`, Array[Char]())
    svn.setAuthenticationManager(authManager)
    svn
  }

}
