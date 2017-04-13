package csw.services.config.server.svn

import java.io.{InputStream, OutputStream}
import java.nio.file.Path

import akka.dispatch.MessageDispatcher
import csw.services.config.server.Settings
import org.tmatesoft.svn.core._
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator
import org.tmatesoft.svn.core.io.{SVNRepository, SVNRepositoryFactory}
import org.tmatesoft.svn.core.wc.{SVNClientManager, SVNRevision}
import org.tmatesoft.svn.core.wc2.{SvnOperationFactory, SvnTarget}

import scala.concurrent.Future

class SvnRepo(settings: Settings, blockingIoDispatcher: MessageDispatcher) {

  private implicit val _blockingIoDispatcher = blockingIoDispatcher

  def getFile(path: Path, revision: Long, outputStream: OutputStream): Future[Unit] = Future {
    val svn = svnHandle()
    try {
      svn.getFile(path.toString, revision, null, outputStream)
      outputStream.flush()
      outputStream.close()
    } finally {
      svn.closeSession()
    }
  }

  // Adds the given file (and dir if needed) to svn.
  // See http://svn.svnkit.com/repos/svnkit/tags/1.3.5/doc/examples/src/org/tmatesoft/svn/examples/repository/Commit.java.
  def addFile(path: Path, comment: String, data: InputStream): Future[SVNCommitInfo] = Future {
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

  // Modifies the contents of the given file in the repository.
  // See http://svn.svnkit.com/repos/svnkit/tags/1.3.5/doc/examples/src/org/tmatesoft/svn/examples/repository/Commit.java.
  def modifyFile(path: Path, comment: String, data: InputStream) = Future {
    val svn = svnHandle()
    try {
      val editor = svn.getCommitEditor(comment, null)
      editor.openRoot(SVNRepository.INVALID_REVISION)
      val filePath = path.toString
      editor.openFile(filePath, SVNRepository.INVALID_REVISION)
      editor.applyTextDelta(filePath, null)
      val deltaGenerator = new SVNDeltaGenerator
      val checksum = deltaGenerator.sendDelta(filePath, data, editor, true)
      editor.closeFile(filePath, checksum)
      editor.closeDir()
      editor.closeEdit
    } finally {
      svn.closeSession()
    }
  }


  def delete(path: Path, comment: String): Future[SVNCommitInfo] = Future {
    val svnOperationFactory = new SvnOperationFactory()
    try {
      val remoteDelete = svnOperationFactory.createRemoteDelete()
      remoteDelete.setSingleTarget(SvnTarget.fromURL(settings.svnUrl.appendPath(path.toString, false)))
      remoteDelete.setCommitMessage(comment)
      remoteDelete.run()
    } finally {
      svnOperationFactory.dispose()
    }
  }

  def list(): Future[List[SVNDirEntry]] = Future {
    // XXX Should .sha1 files have the .sha1 suffix removed in the result?
    var entries = List[SVNDirEntry]()
    val svnOperationFactory = new SvnOperationFactory()
    try {
      val svnList = svnOperationFactory.createList()
      svnList.setSingleTarget(SvnTarget.fromURL(settings.svnUrl, SVNRevision.HEAD))
      svnList.setRevision(SVNRevision.HEAD)
      svnList.setDepth(SVNDepth.INFINITY)
      svnList.setReceiver((_, e: SVNDirEntry) => entries = e :: entries)
      svnList.run()
    } finally {
      svnOperationFactory.dispose()
    }
    entries
      .filter(_.getKind == SVNNodeKind.FILE)
      .sortWith(_.getRelativePath < _.getRelativePath)
  }

  def hist(path: Path, maxResults: Int): Future[List[SVNLogEntry]] = Future {
    val clientManager = SVNClientManager.newInstance()
    var logEntries = List[SVNLogEntry]()
    try {
      val logClient = clientManager.getLogClient
      val handler: ISVNLogEntryHandler = logEntry => logEntries = logEntry :: logEntries
      logClient.doLog(settings.svnUrl, Array(path.toString), SVNRevision.HEAD, null, null, true, true, maxResults, handler)
      logEntries.sortWith(_.getRevision > _.getRevision)
    } finally {
      clientManager.dispose()
    }
  } recover {
    case ex: SVNException => Nil
  }

  // Gets the svn revision from the given id, defaulting to HEAD
  def svnRevision(id: Option[Long] = None): Future[SVNRevision] = Future {
    id match {
      case Some(value) => SVNRevision.create(value)
      case None        => SVNRevision.HEAD
    }
  }

  def pathExists(path: Path, id: Option[Long]): Future[Boolean] = Future {
    checkPath(path, SVNNodeKind.FILE, SVNRepository.INVALID_REVISION)
  }

  // True if the directory path exists in the repository
  private def dirExists(path: Path): Boolean = {
    checkPath(path, SVNNodeKind.DIR, SVNRepository.INVALID_REVISION)
  }

  private def checkPath(path: Path, kind: SVNNodeKind, revision: Long): Boolean = {
    val svn = svnHandle()
    try {
      svn.checkPath(path.toString, revision) == kind
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
