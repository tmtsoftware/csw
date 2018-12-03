package csw.config.server.svn

import java.io.ByteArrayOutputStream
import java.nio.file.{Path, Paths}
import java.time.Instant

import csw.config.api.exceptions.{FileAlreadyExists, FileNotFound}
import csw.config.api.models.{FileType, _}
import csw.config.api.scaladsl.ConfigService
import csw.config.server.commons.ConfigServerLogger
import csw.config.server.files.AnnexFileService
import csw.config.server.{ActorRuntime, Settings}
import csw.logging.scaladsl.Logger
import org.tmatesoft.svn.core.wc.SVNRevision

import scala.async.Async._
import scala.concurrent.Future

class SvnConfigService(settings: Settings, actorRuntime: ActorRuntime, svnRepo: SvnRepo, annexFileService: AnnexFileService)
    extends ConfigService {

  import actorRuntime._

  private val log: Logger = ConfigServerLogger.getLogger

  override def create(path: Path, configData: ConfigData, annex: Boolean, comment: String = ""): Future[ConfigId] = async {
    // if the file already exists in the repo, throw exception
    if (await(exists(path))) {
      throw FileAlreadyExists(path)
    }

    val id = await(createFile(path, configData, annex, comment)) // create file
    // Set the first version of file as active. A version of the file is set as active by storing its revision
    // in another file at <<filePath>> + settings.`active-config-suffix`. For every file stored in svn there is a corresponding
    // file representing the active revision of original svn file.
    await(setActiveVersion(path, id, "initializing active file with the first version"))
    id
  }

  private def createFile(path: Path, configData: ConfigData, annex: Boolean = false, comment: String): Future[ConfigId] = {

    // Create the annex file on local disk and get it's sha. Then create a file in svn repo storing that sha.
    // For every annex file there is a file in svn repo. This file is stored at <<annexFilePath>> + settings.`sha1-suffix` in svn repo.
    def createAnnex(): Future[ConfigId] = async {
      val sha1 = await(annexFileService.post(configData))
      await(createFile(shaFilePath(path), ConfigData.fromString(sha1), annex = false, comment))
    }

    async {
      // if the annex flag is set by the client or file size qualifies for annex then create the file as annex
      if (annex || configData.length > settings.`annex-min-file-size`) {
        log.info(
          s"Either annex=$annex is specified or Input file length ${configData.length} exceeds ${settings.`annex-min-file-size`}; Storing file in Annex"
        )
        await(createAnnex())
      } else {
        await(put(path, configData, update = false, comment))
      }
    }
  }

  override def update(path: Path, configData: ConfigData, comment: String): Future[ConfigId] = {

    // Update the content of the annex file in local disk. Then update the file at path <<annexFilePath>> + settings.`sha1-suffix`
    // in svn repo with new sha calculated after updating annex file.
    def updateAnnex(): Future[ConfigId] = async {
      log.info(s"Updating annex file at path ${path.toString}")
      val sha1 = await(annexFileService.post(configData))
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

  private def get(path: Path, configId: Option[ConfigId] = None) = async {

    // in case of no configId provided the latest revision for this path is taken
    val svnRevision = await(svnRepo.svnRevision(configId.map(_.id.toLong)))

    await(pathStatus(path, configId)) match {
      case PathStatus.NormalSize ⇒
        log.info(s"Getting normal file at path ${path.toString}")
        await(getNormal(path, svnRevision))
      case PathStatus.Annex ⇒
        log.info(s"Getting annex file at path ${path.toString}")
        await(getAnnex(path, svnRevision))
      case PathStatus.Missing ⇒ None
    }
  }

  // Returns the content of the given svn revision of the file, if found
  private def getNormal(path: Path, revision: SVNRevision): Future[Option[ConfigData]] = async {
    val outputStream = new ByteArrayOutputStream()
    await(svnRepo.getFile(path, revision.getNumber, outputStream))
    Some(ConfigData.fromBytes(outputStream.toByteArray))
  }

  // Get the file stored in svn for this annex file. If the svn file exists then get it's file content, which is nothing but
  // the sha of annex file and then use it to get the annex file from local disk.
  private def getAnnex(path: Path, revision: SVNRevision): Future[Option[ConfigData]] = async {
    await(getNormal(shaFilePath(path), revision)) match {
      case None =>
        None
      case Some(configData) =>
        val sha1 = await(configData.toStringF)
        await(annexFileService.get(sha1))
    }
  }

  override def exists(path: Path, id: Option[ConfigId]): Future[Boolean] = async {
    await(pathStatus(path, id)).isInstanceOf[PathStatus.Present]
  }

  // Find the status of the path (Annex, Normal or Missing) and delete accordingly. If the file is normal simply delete
  // it from svn. If the file is annex then only delete the svn file having its sha but not the actual annex file
  // that is on local disk. The actual annex file is kept around so that if there is a feature of reverting a delete operation
  // ever in future then the revert of any annex file will result in reviving it's corresponding svn file which would be
  // having its sha.
  override def delete(path: Path, comment: String = "deleted"): Future[Unit] = async {
    await(pathStatus(path)) match {
      case PathStatus.NormalSize ⇒
        log.info(s"Deleting normal file at path ${path.toString}")
        await(svnRepo.delete(path, comment))
      case PathStatus.Annex ⇒
        log.info(s"Deleting annex file at path ${path.toString}")
        await(svnRepo.delete(shaFilePath(path), comment))
      case PathStatus.Missing ⇒ throw FileNotFound(path)
    }
  }

  // list the file info - (path, id (revision), commit message)
  override def list(fileType: Option[FileType] = None, pattern: Option[String] = None): Future[List[ConfigFileInfo]] = async {
    await(svnRepo.list(fileType, pattern)).map { entry =>
      ConfigFileInfo(Paths.get(entry.getRelativePath), ConfigId(entry.getRevision), entry.getAuthor, entry.getCommitMessage)
    }
  }

  // get the history (id (revision), comment, time) of the file at given path
  override def history(path: Path, from: Instant, to: Instant, maxResults: Int): Future[List[ConfigFileRevision]] = async {
    await(pathStatus(path)) match {
      case PathStatus.NormalSize ⇒
        log.info(s"Fetching history for normal file for path ${path.toString}")
        await(hist(path, from, to, maxResults))
      case PathStatus.Annex ⇒
        log.info(s"Fetching history for annex file for path ${path.toString}")
        // for annex file get it's corresponding svn file and return the (revision, comment, time) of this svn file
        await(hist(shaFilePath(path), from, to, maxResults))
      case PathStatus.Missing ⇒ throw FileNotFound(path)
    }
  }

  // get the history of only active revisions of the file for given path
  override def historyActive(path: Path, from: Instant, to: Instant, maxResults: Int): Future[List[ConfigFileRevision]] = async {
    // get the file representing active revisions for provided path
    val activePath = activeFilePath(path)

    if (await(exists(activePath))) {

      // prepare the history of files representing active revisions (id (revision) of file representing active versions, comment, time)
      val configFileRevisions = await(hist(activePath, from, to, maxResults))

      // map the collected history to (active id (revision) of actual file, comment, time)
      val history = Future.sequence(configFileRevisions.map(historyActiveRevisions(activePath, _)))

      await(history)
    } else
      throw FileNotFound(path)
  }

  // add or update the file representing active version of actual file with the provided id (revision)
  override def setActiveVersion(path: Path, id: ConfigId, comment: String = ""): Future[Unit] = async {
    if (!await(exists(path, Some(id)))) {
      throw FileNotFound(path)
    }

    val activePath = activeFilePath(path)
    val present    = await(exists(activePath))

    if (present) {
      log.info(s"Updating active version for file with path ${path.toString}")
      await(update(activePath, ConfigData.fromString(id.id), comment))
    } else {
      log.info(s"Setting active version for file with path ${path.toString}")
      await(createFile(activePath, ConfigData.fromString(id.id), comment = comment))
    }
  }

  // reset will set the active version to latest availavble version for the file
  override def resetActiveVersion(path: Path, comment: String): Future[Unit] = async {
    if (!await(exists(path))) {
      throw FileNotFound(path)
    }

    val currentVersion = await(getCurrentVersion(path))
    await(setActiveVersion(path, currentVersion.get, comment))
  }

  override def getActive(path: Path): Future[Option[ConfigData]] = {

    // Get the latest file representing active versions of this file and get the data from that file. This data is nothing
    // but the id (revision) marked as active for actual file. Then get the data from actual file for this active id revision.
    def getActiveById(configId: ConfigId): Future[Option[ConfigData]] = async {
      val maybeActiveVersion = await(getLatest(activeFilePath(path)))
      val id                 = if (maybeActiveVersion.isDefined) await(maybeActiveVersion.get.toStringF) else configId.id
      await(getById(path, ConfigId(id)))
    }

    async {
      await(getCurrentVersion(path)) match {
        case None           ⇒ None
        case Some(configId) ⇒ await(getActiveById(configId))
      }
    }
  }

  // Get the file representing active versions for the given file path and with the given time. Get the data from this
  // file which is nothing but the id (revision) marked active for the actual file. Get the data of actual file for this id (revision).
  override def getActiveByTime(path: Path, time: Instant): Future[Option[ConfigData]] = async {
    val activeVersion = await(getByTime(activeFilePath(path), time))
    val activeId      = await(activeVersion.get.toStringF)
    await(getById(path, ConfigId(activeId)))
  }

  // get the id (revision) marked active for given file path
  override def getActiveVersion(path: Path): Future[Option[ConfigId]] = async {
    val configData = await(getLatest(activeFilePath(path)))
    if (configData.isDefined)
      Some(ConfigId(await(configData.get.toStringF)))
    else
      None
  }

  override def getMetadata: Future[ConfigMetadata] = Future {
    ConfigMetadata(
      settings.`repository-dir`,
      settings.`annex-files-dir`,
      settings.annexMinFileSizeAsMetaInfo,
      settings.`max-content-length`
    )
  }

  // get the content of the file representing active revisions and prepare the history view as (id (revision marked active), comment, time)
  private def historyActiveRevisions(path: Path, configFileRevision: ConfigFileRevision): Future[ConfigFileRevision] = async {
    val configData = await(getById(path, configFileRevision.id))
    ConfigFileRevision(
      ConfigId(await(configData.get.toStringF)),
      configFileRevision.author,
      configFileRevision.comment,
      configFileRevision.time
    )
  }

  // determines whether the file at given path and id exists as normal or annex or is missing
  private def pathStatus(path: Path, id: Option[ConfigId] = None): Future[PathStatus] = async {
    val revision = id.map(_.id.toLong)
    if (await(svnRepo.pathExists(path, revision))) {
      log.debug(s"Found the type of file at path ${path.toString} as a normal")
      PathStatus.NormalSize
    } else if (await(svnRepo.pathExists(shaFilePath(path), revision))) {
      log.debug(s"Found the type of file at path ${path.toString} as an annex")
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
  private def put(path: Path, configData: ConfigData, update: Boolean, comment: String): Future[ConfigId] = async {
    val inputStream = configData.toInputStream

    val commitInfo = if (update) {
      log.info(s"Updating normal file at path ${path.toString}")
      await(svnRepo.modifyFile(path, comment, inputStream))
    } else {
      log.info(s"Creating normal file at path ${path.toString}")
      await(svnRepo.addFile(path, comment, inputStream))
    }
    ConfigId(commitInfo.getNewRevision)
  }

  // Returns the current version of the file, if known
  private def getCurrentVersion(path: Path): Future[Option[ConfigId]] = async {
    await(pathStatus(path)) match {
      case PathStatus.NormalSize ⇒
        log.info(s"Getting current version for normal file at path ${path.toString}")
        await(hist(path, Instant.MIN, Instant.now, 1)).headOption.map(_.id)
      case PathStatus.Annex ⇒
        log.info(s"Getting current version for annex file at path ${path.toString}")
        await(hist(shaFilePath(path), Instant.MIN, Instant.now, 1)).headOption.map(_.id)
      case PathStatus.Missing ⇒ None
    }
  }

  // get the file history - (id (revision), comment, time)
  private def hist(path: Path, from: Instant, to: Instant, maxResults: Int): Future[List[ConfigFileRevision]] = async {
    await(svnRepo.hist(path, from, to, maxResults))
      .map(e => ConfigFileRevision(ConfigId(e.getRevision), e.getAuthor, e.getMessage, e.getDate.toInstant))
  }

  // File used to store the SHA-1 of the actual file, if annexd.
  private def shaFilePath(path: Path): Path = Paths.get(s"${path.toString}${settings.`sha1-suffix`}")

  // File used to store the id of the active version of the file.
  private def activeFilePath(path: Path): Path = Paths.get(s"${path.toString}${settings.`active-config-suffix`}")
}
