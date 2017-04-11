package csw.services.config.server.repo

import java.io._
import java.util.Date

import csw.services.config.api.commons.ActorRuntime
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.api.scaladsl.ConfigManager
import csw.services.config.server.Settings
import org.tmatesoft.svn.core._
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator
import org.tmatesoft.svn.core.io.{SVNRepository, SVNRepositoryFactory}
import org.tmatesoft.svn.core.wc.{SVNClientManager, SVNRevision}
import org.tmatesoft.svn.core.wc2.{ISvnObjectReceiver, SvnOperationFactory, SvnTarget}

import scala.concurrent.Future
import async.Async._

class SvnConfigManager(settings: Settings, oversizeFileManager: OversizeFileManager, actorRuntime: ActorRuntime) extends ConfigManager {

  import actorRuntime._

  override def name: String = "my name is missing"

  override def create(path: File, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = {

    def createOversize(): Future[ConfigId] = async {
      val sha1 = await(oversizeFileManager.post(configData))
      await(create(shaFile(path), ConfigData.fromString(sha1), oversize = false, comment))
    }

    // If the file does not already exists in the repo, create it
    def createImpl(present: Boolean): Future[ConfigId] = {
      if (present) {
        Future.failed(new IOException("File already exists in repository: " + path))
      } else if (oversize) {
        createOversize()
      } else {
        put(path, configData, update = false, comment)
      }
    }

    async {
      val present = await(exists(path))
      await(createImpl(present))
    }
  }

  override def update(path: File, configData: ConfigData, comment: String): Future[ConfigId] = {

    def updateOversize(): Future[ConfigId] = async {
      val sha1 = await(oversizeFileManager.post(configData))
      await(update(shaFile(path), ConfigData.fromString(sha1), comment))
    }

    // If the file already exists in the repo, update it
    def updateImpl(present: Boolean): Future[ConfigId] = {
      if (!present) {
        Future.failed(new FileNotFoundException("File not found: " + path))
      } else if (isOversize(path)) {
        updateOversize()
      } else {
        put(path, configData, update = true, comment)
      }
    }

    async {
      val present = await(exists(path))
      await(updateImpl(present))
    }
  }

  override def get(path: File, id: Option[ConfigId]): Future[Option[ConfigData]] = {
    // Get oversize files that are stored in the annex server
    def getOversize: Future[Option[ConfigData]] = async {
      val opt = await(get(shaFile(path), id))
      await(getData(opt))
    }

    // Gets the actual file data using the SHA-1 value contained in the checked in file
    def getData(opt: Option[ConfigData]): Future[Option[ConfigData]] = async {
      opt match {
        case None             =>
          None
        case Some(configData) =>
          val sha1 = await(configData.toStringF)
          oversizeFileManager.get(sha1)
      }
    }

    // Returns the contents of the given version of the file, if found
    def getConfigData: Future[Option[ConfigData]] = async {
      val os = new ByteArrayOutputStream()
      val svn = getSvn
      try {
        svn.getFile(path.getPath, svnRevision(id).getNumber, null, os)
      } finally {
        svn.closeSession()
      }
      Some(ConfigData.fromBytes(os.toByteArray))
    }

    // If the file exists in the repo, get its data
    def getImpl(present: Boolean): Future[Option[ConfigData]] = {
      if (!present) {
        Future(None)
      } else if (isOversize(path)) {
        getOversize
      } else {
        getConfigData
      }
    }

    async {
      val present = await(exists(path))
      await(getImpl(present))
    }
  }

  override def get(path: File, date: Date): Future[Option[ConfigData]] = {
    val t = date.getTime

    // Gets the ConfigFileHistory matching the date
    def getHist: Future[Option[ConfigFileHistory]] = async {
      val h = await(history(path))
      val found = h.find(_.time.getTime <= t)
      if (found.nonEmpty) found
      else if (h.isEmpty) None
      else Some(if (t > h.head.time.getTime) h.head else h.last)
    }

    async {
      val hist = await(getHist)
      if (hist.isEmpty) None else await(get(path, hist.map(_.id)))
    }
  }

  override def exists(path: File): Future[Boolean] = Future(pathExists(path))

  //TODO: This implementation deletes all versions of a file. This is different than the expecations
  override def delete(path: File, comment: String = "deleted"): Future[Unit] = {
    def deleteFile(path: File, comment: String = "deleted"): Unit = {
      if (isOversize(path)) {
        deleteFile(shaFile(path), comment)
      } else {
        if (!pathExists(path)) {
          throw new FileNotFoundException("Can't delete " + path + " because it does not exist")
        }

        val svnOperationFactory = new SvnOperationFactory()
        try {
          val remoteDelete = svnOperationFactory.createRemoteDelete()
          remoteDelete.setSingleTarget(SvnTarget.fromURL(settings.svnUrl.appendPath(path.getPath, false)))
          remoteDelete.setCommitMessage(comment)
          remoteDelete.run()
        } finally {
          svnOperationFactory.dispose()
        }
      }
    }

    Future {
      deleteFile(path, comment)
    }
  }

  override def list(): Future[List[ConfigFileInfo]] = Future {
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
    entries.filter(_.getKind == SVNNodeKind.FILE).sortWith(_.getRelativePath < _.getRelativePath)
      .map(e => ConfigFileInfo(new File(e.getRelativePath), ConfigId(e.getRevision), e.getCommitMessage))
  }

  override def history(path: File, maxResults: Int): Future[List[ConfigFileHistory]] = async {
    // XXX Should .sha1 files have the .sha1 suffix removed in the result?
    if (isOversize(path))
      hist(shaFile(path), maxResults)
    else
      hist(path, maxResults)
  }

  override def setDefault(path: File, id: Option[ConfigId] = None): Future[Unit] = {
    (if (id.isDefined) id else getCurrentVersion(path)) match {
      case Some(configId) =>
        create(defaultFile(path), ConfigData.fromString(configId.id)).map(_ => ())
      case None           =>
        Future.failed(new FileNotFoundException(s"Unknown path $path"))
    }
  }

  override def resetDefault(path: File): Future[Unit] = {
    delete(defaultFile(path))
  }

  override def getDefault(path: File): Future[Option[ConfigData]] = async {
    getCurrentVersion(path) match {
      case None ⇒
        None
      case Some(configId) ⇒
        val d = await(get(defaultFile(path)))
        val id = if (d.isDefined) await(d.get.toStringF) else configId.id
        await(get(path, Some(ConfigId(id))))
    }
  }

  /**
    * Creates or updates a config file with the given path and data and optional comment.
    *
    * @param path       the config file path
    * @param configData the contents of the file
    * @param comment    an optional comment to associate with this file
    * @return a future unique id that can be used to refer to the file
    */
  private def put(path: File, configData: ConfigData, update: Boolean, comment: String = ""): Future[ConfigId] = Future {
    val inputStream = configData.toInputStream
    val commitInfo = if (update) {
      modifyFile(comment, path, inputStream)
    } else {
      addFile(comment, path, inputStream)
    }
    ConfigId(commitInfo.getNewRevision)
  }

  // Modifies the contents of the given file in the repository.
  // See http://svn.svnkit.com/repos/svnkit/tags/1.3.5/doc/examples/src/org/tmatesoft/svn/examples/repository/Commit.java.
  private def modifyFile(comment: String, path: File, data: InputStream): SVNCommitInfo = {
    val svn = getSvn
    try {
      val editor = svn.getCommitEditor(comment, null)
      editor.openRoot(SVNRepository.INVALID_REVISION)
      val filePath = path.getPath
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

  // Gets an object for accessing the svn repository (not reusing a single instance since not thread safe)
  private def getSvn: SVNRepository = {
    val svn = SVNRepositoryFactory.create(settings.svnUrl)
    val authManager = BasicAuthenticationManager.newInstance(settings.`svn-user-name`, Array[Char]())
    svn.setAuthenticationManager(authManager)
    svn
  }

  // Adds the given file (and dir if needed) to svn.
  // See http://svn.svnkit.com/repos/svnkit/tags/1.3.5/doc/examples/src/org/tmatesoft/svn/examples/repository/Commit.java.
  private def addFile(comment: String, path: File, data: InputStream): SVNCommitInfo = {
    val svn = getSvn
    try {
      val editor = svn.getCommitEditor(comment, null)
      editor.openRoot(SVNRepository.INVALID_REVISION)
      val dirPath = path.getParentFile

      // Recursively add any missing directories leading to the file
      def addDir(dir: File): Unit = {
        if (dir != null) {
          addDir(dir.getParentFile)
          if (!dirExists(dir)) {
            editor.addDir(dir.getPath, null, SVNRepository.INVALID_REVISION)
          }
        }
      }

      addDir(dirPath)
      val filePath = path.getPath
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
  private def dirExists(path: File): Boolean = {
    val svn = getSvn
    try {
      svn.checkPath(path.getPath, SVNRepository.INVALID_REVISION) == SVNNodeKind.DIR
    } finally {
      svn.closeSession()
    }
  }

  // True if the path exists in the repository
  private def pathExists(path: File): Boolean = {
    val svn = getSvn
    try {
      svn.checkPath(path.getPath, SVNRepository.INVALID_REVISION) == SVNNodeKind.FILE || isOversize(path)
    } finally {
      svn.closeSession()
    }
  }

  // True if the .sha1 file exists, meaning the file needs special oversize handling.
  private def isOversize(path: File): Boolean = {
    val svn = getSvn
    try {
      svn.checkPath(shaFile(path).getPath, SVNRepository.INVALID_REVISION) == SVNNodeKind.FILE
    } finally {
      svn.closeSession()
    }
  }

  // Gets the svn revision from the given id, defaulting to HEAD
  private def svnRevision(id: Option[ConfigId] = None): SVNRevision = {
    id match {
      case Some(configId) => SVNRevision.create(configId.id.toLong)
      case None           => SVNRevision.HEAD
    }
  }

  // File used to store the SHA-1 of the actual file, if oversized.
  private def shaFile(file: File): File = new File(s"${file.getPath}${settings.`sha1-suffix`}")

  // Returns the current version of the file, if known
  private def getCurrentVersion(path: File): Option[ConfigId] = {
    if (isOversize(path))
      hist(shaFile(path), 1).headOption.map(_.id)
    else
      hist(path, 1).headOption.map(_.id)
  }

  private def hist(path: File, maxResults: Int = Int.MaxValue): List[ConfigFileHistory] = {
    val clientManager = SVNClientManager.newInstance()
    var logEntries = List[SVNLogEntry]()
    try {
      val logClient = clientManager.getLogClient
      logClient.doLog(settings.svnUrl, Array(path.getPath), SVNRevision.HEAD, null, null, true, true, maxResults,
        new ISVNLogEntryHandler() {
          override def handleLogEntry(logEntry: SVNLogEntry): Unit = logEntries = logEntry :: logEntries
        })
      logEntries.sortWith(_.getRevision > _.getRevision)
        .map(e => ConfigFileHistory(ConfigId(e.getRevision), e.getMessage, e.getDate))
    } catch {
      case ex: SVNException => Nil
    } finally {
      clientManager.dispose()
    }
  }

  // File used to store the id of the default version of the file.
  private def defaultFile(file: File): File =
    new File(s"${file.getPath}${settings.`default-suffix`}")

}
