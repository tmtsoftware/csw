package csw.services.config.server.svn

import java.io.{InputStream, OutputStream}
import java.nio.file.Path
import java.time.Instant
import java.util.regex.Pattern

import akka.dispatch.MessageDispatcher
import csw.services.config.api.models.FileType
import csw.services.config.server.Settings
import csw.services.config.server.commons.ConfigServerLogger
import csw.services.config.server.commons.SVNDirEntryExt.RichSvnDirEntry
import org.tmatesoft.svn.core._
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator
import org.tmatesoft.svn.core.io.{SVNRepository, SVNRepositoryFactory}
import org.tmatesoft.svn.core.wc.{SVNClientManager, SVNRevision}
import org.tmatesoft.svn.core.wc2.{ISvnObjectReceiver, SvnOperationFactory, SvnTarget}

import scala.concurrent.Future

/**
 * Performs file operations on SVN using SvnKit
 * @param settings                  server runtime configuration
 * @param blockingIoDispatcher      dispatcher to be used for blocking operations
 */
class SvnRepo(settings: Settings, blockingIoDispatcher: MessageDispatcher) extends ConfigServerLogger.Simple {

  private implicit val _blockingIoDispatcher = blockingIoDispatcher

  // Intitialize repository
  def initSvnRepo(): Unit =
    try {
      // Create the new main repo
      FSRepositoryFactory.setup()
      SVNRepositoryFactory.createLocalRepository(settings.repositoryFile, false, false)
      log.info(s"New Repository created at ${settings.svnUrl}")
    } catch {
      // If the repo already exists, print stracktrace and continue to boot
      case ex: SVNException if ex.getErrorMessage.getErrorCode == SVNErrorCode.IO_ERROR ⇒
        log.info(s"Repository already exists at ${settings.svnUrl}")
    }

  // Fetch the file from svn repo and write the contents on outputStream
  def getFile(path: Path, revision: Long, outputStream: OutputStream): Future[Unit] = Future {
    val svn = svnHandle()
    try {
      svn.getFile(path.toString, revision, null, outputStream)
    } finally {
      svn.closeSession()
    }
  }

  def getFileSize(path: Path, revision: Long): Future[Long] = Future {
    val svn = svnHandle()
    try {
      svn.info(path.toString, revision).getSize
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

      var openDirDepth = 1
      // Recursively add any missing directories leading to the file
      def addDir(dir: Path): Unit =
        if (dir != null) {
          addDir(dir.getParent)
          if (!dirExists(dir)) {
            editor.addDir(dir.toString, null, SVNRepository.INVALID_REVISION)
            openDirDepth += 1
          }
        }

      def closeDir(checksum: String, depth: Int): SVNCommitInfo = {
        editor.closeFile(path.toString, checksum)
        for (i <- 1 to openDirDepth)
          editor.closeDir()
        editor.closeEdit
      }

      addDir(dirPath)
      val filePath = path.toString
      editor.addFile(filePath, null, SVNRepository.INVALID_REVISION)
      editor.applyTextDelta(filePath, null)
      val deltaGenerator = new SVNDeltaGenerator

      val checksum = deltaGenerator.sendDelta(filePath, data, editor, true)
      closeDir(checksum, openDirDepth)
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
      val checksum       = deltaGenerator.sendDelta(filePath, data, editor, true)
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

  def list(fileType: Option[FileType] = None, pattern: Option[String] = None): Future[List[SVNDirEntry]] = Future {
    val svnOperationFactory = new SvnOperationFactory()
    // svn always stores file in the repo without '/' prefix.
    // Hence if input pattern is provided like '/root/', then prefix '/' need to be striped to get the list of files from root folder.
    val compiledPattern            = pattern.map(pat ⇒ Pattern.compile(pat.stripPrefix("/")))
    var entries: List[SVNDirEntry] = List.empty
    val receiver: ISvnObjectReceiver[SVNDirEntry] = { (_, entry: SVNDirEntry) ⇒
      if (entry.isFile && entry.isNotActiveFile(settings.`active-config-suffix`) && entry.matchesFileType(fileType,
            settings.`sha1-suffix`)) {
        entry.stripAnnexSuffix(settings.`sha1-suffix`)
        if (entry.matches(compiledPattern)) {
          entries = entry :: entries
        }
      }
    }
    try {
      val svnList = svnOperationFactory.createList()
      svnList.setSingleTarget(SvnTarget.fromURL(settings.svnUrl, SVNRevision.HEAD))
      svnList.setRevision(SVNRevision.HEAD)
      svnList.setDepth(SVNDepth.INFINITY)
      svnList.setReceiver(receiver)
      svnList.run()
      entries.sortWith(_.getRelativePath < _.getRelativePath)
    } finally {
      svnOperationFactory.dispose()
    }
  }

  def hist(path: Path, from: Instant, to: Instant, maxResults: Int): Future[List[SVNLogEntry]] = Future {
    val clientManager = SVNClientManager.newInstance()
    var logEntries    = List[SVNLogEntry]()
    try {
      val logClient = clientManager.getLogClient
      val handler: ISVNLogEntryHandler = logEntry =>
        logEntries = {
          if (logEntry.getDate.toInstant.isAfter(from) && logEntry.getDate.toInstant.isBefore(to))
            logEntry :: logEntries
          else
            logEntries
      }
      logClient.doLog(settings.svnUrl, Array(path.toString), SVNRevision.HEAD, null, null, true, true, maxResults,
        handler)
      logEntries.sortWith(_.getRevision > _.getRevision)
    } finally {
      clientManager.dispose()
    }
  }

  // Gets the svn revision from the given id, defaulting to HEAD
  def svnRevision(id: Option[Long] = None): Future[SVNRevision] = Future {
    id match {
      case Some(value) => SVNRevision.create(value)
      case None        => SVNRevision.HEAD
    }
  }

  def pathExists(path: Path, id: Option[Long]): Future[Boolean] = Future {
    checkPath(path, SVNNodeKind.FILE, id.getOrElse(SVNRepository.INVALID_REVISION))
  }

  // True if the directory path exists in the repository
  private def dirExists(path: Path): Boolean =
    checkPath(path, SVNNodeKind.DIR, SVNRepository.INVALID_REVISION)

  private def checkPath(path: Path, kind: SVNNodeKind, revision: Long): Boolean = {
    val svn = svnHandle()
    try {
      svn.checkPath(path.toString, revision) == kind
    } catch {
      case ex: SVNException if ex.getErrorMessage.getErrorCode == SVNErrorCode.FS_NO_SUCH_REVISION ⇒ false
    } finally {
      svn.closeSession()
    }
  }

  // Gets an object for accessing the svn repository (not reusing a single instance since not thread safe)
  private def svnHandle(): SVNRepository = {
    val svn         = SVNRepositoryFactory.create(settings.svnUrl)
    val authManager = BasicAuthenticationManager.newInstance(settings.`svn-user-name`, Array[Char]())
    svn.setAuthenticationManager(authManager)
    svn
  }

  // Test if there're no problems with accessing a repository
  def testConnection(): Unit = {
    val svn = svnHandle()
    try {
      svn.testConnection()
    } finally {
      svn.closeSession()
    }
  }

}
