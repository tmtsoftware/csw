package csw.services.config.server.svn

import java.io.ByteArrayOutputStream
import java.nio.file.{Path, Paths}
import java.time.Instant

import csw.services.config.api.exceptions.{FileAlreadyExists, FileNotFound}
import csw.services.config.api.models.{FileType, _}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.files.AnnexFileService
import csw.services.config.server.{ActorRuntime, Settings}
import csw.services.config.server.commons.ConfigServerLogger
import org.tmatesoft.svn.core.wc.SVNRevision
import scala.async.Async._
import scala.concurrent.Future

class SvnConfigService(settings: Settings, fileService: AnnexFileService, actorRuntime: ActorRuntime, svnRepo: SvnRepo)
    extends ConfigService
    with ConfigServerLogger.Simple {

  import actorRuntime._

  override def create(path: Path, configData: ConfigData, annex: Boolean, comment: String = ""): Future[ConfigId] =
    async {
      // If the file already exists in the repo, throw exception
      if (await(exists(path))) {
        throw FileAlreadyExists(path)
      }

      val id = await(createFile(path, configData, annex, comment))
      await(setActiveVersion(path, id, "initializing active file with the first version"))
      id
    }

  private def createFile(path: Path,
                         configData: ConfigData,
                         annex: Boolean = false,
                         comment: String): Future[ConfigId] = {

    def createAnnex(): Future[ConfigId] = async {
      val sha1 = await(fileService.post(configData))
      await(createFile(shaFilePath(path), ConfigData.fromString(sha1), annex = false, comment))
    }

    async {
      // If the file does not already exists in the repo, create it
      if (annex || configData.length > settings.`annex-min-file-size`) {
        log.info(
            s"Either annex=$annex is specified or Input file length ${configData.length} exceeds ${settings.`annex-min-file-size`}; Storing file in Annex")
        await(createAnnex())
      } else {
        await(put(path, configData, update = false, comment))
      }
    }
  }

  override def update(path: Path, configData: ConfigData, comment: String): Future[ConfigId] = {

    def updateAnnex(): Future[ConfigId] = async {
      val sha1 = await(fileService.post(configData))
      await(update(shaFilePath(path), ConfigData.fromString(sha1), comment))
    }

    // If the file already exists in the repo, update it
    async {
      await(pathStatus(path)) match {
        case PathStatus.NormalSize ⇒ await(put(path, configData, update = true, comment))
        case PathStatus.Annex      ⇒ await(updateAnnex())
        case PathStatus.Missing    ⇒ throw FileNotFound(path)
      }
    }
  }

  // Returns the contents of the given version of the file, if found
  private def getNormalSize(path: Path, revision: SVNRevision): Future[Option[ConfigData]] = async {
    val outputStream = new ByteArrayOutputStream()
    await(svnRepo.getFile(path, revision.getNumber, outputStream))
    Some(ConfigData.fromBytes(outputStream.toByteArray))
  }

  // Get annex files that are stored in the annex server
  private def getAnnex(path: Path, revision: SVNRevision): Future[Option[ConfigData]] = async {
    await(getNormalSize(shaFilePath(path), revision)) match {
      case None =>
        None
      case Some(configData) =>
        val sha1 = await(configData.toStringF)
        await(fileService.get(sha1))
    }
  }

  private def get(path: Path, configId: Option[ConfigId] = None) =
    async {
      val svnRevision = await(svnRepo.svnRevision(configId.map(_.id.toLong)))

      await(pathStatus(path, configId)) match {
        case PathStatus.NormalSize ⇒ await(getNormalSize(path, svnRevision))
        case PathStatus.Annex      ⇒ await(getAnnex(path, svnRevision))
        case PathStatus.Missing    ⇒ None
      }
    }
  // If the file exists in the repo, get data of its latest revision
  override def getLatest(path: Path): Future[Option[ConfigData]] = get(path)

  // If the version specified by configId for the file exists in the repo, get its data
  override def getById(path: Path, configId: ConfigId): Future[Option[ConfigData]] = get(path, Some(configId))

  override def getByTime(path: Path, time: Instant): Future[Option[ConfigData]] = {

    // Gets the ConfigFileHistory matching the date
    def getHist: Future[Option[ConfigFileRevision]] = async {
      val h     = await(history(path))
      val found = h.find(t => t.time.isBefore(time) || t.time.equals(time))
      if (found.nonEmpty) found
      else if (h.isEmpty) None
      else Some(if (time.isAfter(h.head.time)) h.head else h.last)
    }

    async {
      val hist = await(getHist)
      if (hist.isEmpty) None else await(getById(path, hist.map(_.id).get))
    }
  }

  override def exists(path: Path, id: Option[ConfigId]): Future[Boolean] = async {
    await(pathStatus(path, id)).isInstanceOf[PathStatus.Present]
  }

  override def delete(path: Path, comment: String = "deleted"): Future[Unit] = async {
    await(pathStatus(path)) match {
      case PathStatus.NormalSize ⇒ await(svnRepo.delete(path, comment))
      case PathStatus.Annex      ⇒ await(svnRepo.delete(shaFilePath(path), comment))
      case PathStatus.Missing    ⇒ throw FileNotFound(path)
    }
  }

  override def list(fileType: Option[FileType] = None, pattern: Option[String] = None): Future[List[ConfigFileInfo]] =
    async {
      await(svnRepo.list(fileType, pattern)).map { entry =>
        ConfigFileInfo(Paths.get(entry.getRelativePath), ConfigId(entry.getRevision), entry.getCommitMessage)
      }
    }

  override def history(path: Path, from: Instant, to: Instant, maxResults: Int): Future[List[ConfigFileRevision]] =
    async {
      await(pathStatus(path)) match {
        case PathStatus.NormalSize ⇒ await(hist(path, from, to, maxResults))
        case PathStatus.Annex      ⇒ await(hist(shaFilePath(path), from, to, maxResults))
        case PathStatus.Missing    ⇒ throw FileNotFound(path)
      }
    }

  override def historyActive(path: Path,
                             from: Instant,
                             to: Instant,
                             maxResults: Int): Future[List[ConfigFileRevision]] =
    async {
      val activePath = activeFilePath(path)

      if (await(exists(activePath))) {

        val configFileRevisions = await(hist(activePath, from, to, maxResults))

        val history = Future.sequence(configFileRevisions.map(historyActiveRevisions(activePath, _)))

        await(history)
      } else
        throw FileNotFound(path)
    }

  override def setActiveVersion(path: Path, id: ConfigId, comment: String = ""): Future[Unit] = async {
    if (!await(exists(path, Some(id)))) {
      throw FileNotFound(path)
    }

    val activePath = activeFilePath(path)
    val present    = await(exists(activePath))

    if (present) {
      await(update(activePath, ConfigData.fromString(id.id), comment))
    } else {
      await(createFile(activePath, ConfigData.fromString(id.id), comment = comment))
    }
  }

  override def resetActiveVersion(path: Path, comment: String): Future[Unit] = async {
    if (!await(exists(path))) {
      throw FileNotFound(path)
    }

    val currentVersion = await(getCurrentVersion(path))
    await(setActiveVersion(path, currentVersion.get, comment))
  }

  override def getActive(path: Path): Future[Option[ConfigData]] = {

    def getActiveById(configId: ConfigId): Future[Option[ConfigData]] = async {
      val d  = await(getLatest(activeFilePath(path)))
      val id = if (d.isDefined) await(d.get.toStringF) else configId.id
      await(getById(path, ConfigId(id)))
    }

    async {
      await(getCurrentVersion(path)) match {
        case None           ⇒ None
        case Some(configId) ⇒ await(getActiveById(configId))
      }
    }
  }

  override def getActiveByTime(path: Path, time: Instant): Future[Option[ConfigData]] = async {
    val activeVersion = await(getByTime(activeFilePath(path), time))
    val activeId      = await(activeVersion.get.toStringF)
    await(getById(path, ConfigId(activeId)))
  }

  override def getActiveVersion(path: Path): Future[Option[ConfigId]] = async {
    val configData = await(getLatest(activeFilePath(path)))
    if (configData.isDefined)
      Some(ConfigId(await(configData.get.toStringF)))
    else
      None
  }

  override def getMetadata: Future[ConfigMetadata] = Future {
    ConfigMetadata(settings.`repository-dir`, settings.`annex-files-dir`, settings.annexMinFileSizeAsMetaInfo,
      settings.`max-content-length`)
  }

  private def historyActiveRevisions(path: Path, configFileRevision: ConfigFileRevision): Future[ConfigFileRevision] =
    async {
      val configData = await(getById(path, configFileRevision.id))
      ConfigFileRevision(ConfigId(await(configData.get.toStringF)), configFileRevision.comment,
        configFileRevision.time)
    }

  private def pathStatus(path: Path, id: Option[ConfigId] = None): Future[PathStatus] = async {
    val revision = id.map(_.id.toLong)
    if (await(svnRepo.pathExists(path, revision))) {
      PathStatus.NormalSize
    } else if (await(svnRepo.pathExists(shaFilePath(path), revision))) {
      PathStatus.Annex
    } else {
      PathStatus.Missing
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
  private def put(path: Path, configData: ConfigData, update: Boolean, comment: String): Future[ConfigId] =
    async {
      val inputStream = configData.toInputStream

      val commitInfo = if (update) {
        await(svnRepo.modifyFile(path, comment, inputStream))
      } else {
        await(svnRepo.addFile(path, comment, inputStream))
      }
      ConfigId(commitInfo.getNewRevision)
    }

  // Returns the current version of the file, if known
  private def getCurrentVersion(path: Path): Future[Option[ConfigId]] = async {
    await(pathStatus(path)) match {
      case PathStatus.NormalSize ⇒ await(hist(path, Instant.MIN, Instant.now, 1)).headOption.map(_.id)
      case PathStatus.Annex      ⇒ await(hist(shaFilePath(path), Instant.MIN, Instant.now, 1)).headOption.map(_.id)
      case PathStatus.Missing    ⇒ None
    }
  }

  private def hist(path: Path, from: Instant, to: Instant, maxResults: Int): Future[List[ConfigFileRevision]] = async {
    await(svnRepo.hist(path, from, to, maxResults))
      .map(e => ConfigFileRevision(ConfigId(e.getRevision), e.getMessage, e.getDate.toInstant))
  }

  // File used to store the SHA-1 of the actual file, if annexd.
  private def shaFilePath(path: Path): Path = Paths.get(s"${path.toString}${settings.`sha1-suffix`}")

  // File used to store the id of the active version of the file.
  private def activeFilePath(path: Path): Path = Paths.get(s"${path.toString}${settings.`active-config-suffix`}")
}
