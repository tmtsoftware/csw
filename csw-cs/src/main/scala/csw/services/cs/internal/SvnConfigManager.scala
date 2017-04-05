package csw.services.cs.internal

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, IOException}

import akka.actor.ActorSystem
import csw.services.cs.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.cs.scaladsl.ConfigManager
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator
import org.tmatesoft.svn.core.io.{SVNRepository, SVNRepositoryFactory}
import org.tmatesoft.svn.core.{SVNCommitInfo, SVNNodeKind}

import scala.concurrent.Future

class SvnConfigManager(settings: Settings) extends ConfigManager {

  private implicit val system = ActorSystem()
  import system.dispatcher

  override def name: String = "my name is missing"

  /**
    * Initializes an svn repository in the given dir.
    *
    * @param dir directory to contain the new repository
    */
  def initSvnRepo(dir: File): Unit = {
    // Create the new main repo
    FSRepositoryFactory.setup()
    SVNRepositoryFactory.createLocalRepository(settings.file, false, true)
  }

  override def create(path: File, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = {

    // If the file does not already exists in the repo, create it
    def createImpl(present: Boolean): Future[ConfigId] = {
      if (present) {
        Future.failed(new IOException("File already exists in repository: " + path))
      } else {
        put(path, configData, update = false, comment)
      }
    }

    for {
      present <- exists(path)
      configId <- createImpl(present)
    } yield configId

  }

  /**
    * Creates or updates a config file with the given path and data and optional comment.
    *
    * @param path       the config file path
    * @param configData the contents of the file
    * @param comment    an optional comment to associate with this file
    * @return a future unique id that can be used to refer to the file
    */
  private def put(path: File, configData: ConfigData, update: Boolean, comment: String = ""): Future[ConfigId] = {
    val os = new ByteArrayOutputStream()
    for {
      _ <- configData.writeToOutputStream(os)
    } yield {
      val data = os.toByteArray
      val commitInfo = if (update) {
        modifyFile(comment, path, data)
      } else {
        addFile(comment, path, data)
      }
      ConfigId(commitInfo.getNewRevision)
    }
  }

  // Modifies the contents of the given file in the repository.
  // See http://svn.svnkit.com/repos/svnkit/tags/1.3.5/doc/examples/src/org/tmatesoft/svn/examples/repository/Commit.java.
  def modifyFile(comment: String, path: File, data: Array[Byte]): SVNCommitInfo = {
    val svn = getSvn
    try {
      val editor = svn.getCommitEditor(comment, null)
      editor.openRoot(SVNRepository.INVALID_REVISION)
      val filePath = path.getPath
      editor.openFile(filePath, SVNRepository.INVALID_REVISION)
      editor.applyTextDelta(filePath, null)
      val deltaGenerator = new SVNDeltaGenerator
      val checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(data), editor, true)
      editor.closeFile(filePath, checksum)
      editor.closeDir()
      editor.closeEdit
    } finally {
      svn.closeSession()
    }
  }

  // Gets an object for accessing the svn repository (not reusing a single instance since not thread safe)
  private def getSvn: SVNRepository = {
    val svn = SVNRepositoryFactory.create(settings.url)
    val authManager = BasicAuthenticationManager.newInstance(settings.userName, Array[Char]())
    svn.setAuthenticationManager(authManager)
    svn
  }

  // Adds the given file (and dir if needed) to svn.
  // See http://svn.svnkit.com/repos/svnkit/tags/1.3.5/doc/examples/src/org/tmatesoft/svn/examples/repository/Commit.java.
  private def addFile(comment: String, path: File, data: Array[Byte]): SVNCommitInfo = {
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
      val checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(data), editor, true)
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

  // File used to store the SHA-1 of the actual file, if oversized.
  private def shaFile(file: File): File =
    new File(s"${file.getPath}${settings.sha1Suffix}")

  override def update(path: File, configData: ConfigData, comment: String): Future[ConfigId] = ???

  override def createOrUpdate(path: File, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = ???

  override def get(path: File, id: Option[ConfigId]): Future[Option[ConfigData]] = ???

  override def exists(path: File): Future[Boolean] = Future(pathExists(path))

  override def delete(path: File, comment: String): Future[Unit] = ???

  override def list(): Future[List[ConfigFileInfo]] = ???

  override def history(path: File, maxResults: Int): Future[List[ConfigFileHistory]] = ???

  override def setDefault(path: File, id: Option[ConfigId]): Future[Unit] = ???

  override def resetDefault(path: File): Future[Unit] = ???

  override def getDefault(path: File): Future[Option[ConfigData]] = ???
}
