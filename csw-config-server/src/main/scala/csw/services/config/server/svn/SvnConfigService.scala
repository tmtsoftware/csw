package csw.services.config.server.svn

import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.stream.scaladsl.StreamConverters
import csw.services.config.api.exceptions.{FileAlreadyExists, FileNotFound}
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.files.OversizeFileService
import csw.services.config.server.commons.PathExt.RichPath
import csw.services.config.server.{ActorRuntime, Settings}
import csw.services.location.internal.StreamExt.RichSource

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

class SvnConfigService(settings: Settings,
                       fileService: OversizeFileService,
                       actorRuntime: ActorRuntime,
                       svnRepo: SvnRepo)
    extends ConfigService {

  import actorRuntime._

  override def name: String = "my name is missing"

  override def create(path: Path, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = {

    def createOversize(): Future[ConfigId] = async {
      val sha1 = await(fileService.post(configData))
      await(create(shaFilePath(path), ConfigData.fromString(sha1), oversize = false, comment))
    }

    async {
      path.validateName
      // If the file does not already exists in the repo, create it
      if (await(exists(path))) {
        throw FileAlreadyExists(path)
      }
      if (oversize) {
        await(createOversize())
      } else {
        await(put(path, configData, update = false, comment))
      }
    }
  }

  override def update(path: Path, configData: ConfigData, comment: String): Future[ConfigId] = {

    def updateOversize(): Future[ConfigId] = async {
      val sha1 = await(fileService.post(configData))
      await(update(shaFilePath(path), ConfigData.fromString(sha1), comment))
    }

    // If the file already exists in the repo, update it
    async {
      await(pathStatus(path)) match {
        case PathStatus.NormalSize ⇒ await(put(path, configData, update = true, comment))
        case PathStatus.Oversize   ⇒ await(updateOversize())
        case PathStatus.Missing    ⇒ throw FileNotFound(path)
      }
    }
  }

  override def get(path: Path, id: Option[ConfigId]): Future[Option[ConfigData]] = {
    // Returns the contents of the given version of the file, if found
    def getNormalSize: Future[Option[ConfigData]] = async {
      val outputStream = StreamConverters.asOutputStream().cancellableMat
      val revision     = await(svnRepo.svnRevision(id.map(_.id.toLong)))
      val source = outputStream.mapMaterializedValue {
        case (out, switch) ⇒
          svnRepo.getFile(path, revision.getNumber, out).recover {
            case NonFatal(ex) ⇒ switch.abort(ex)
          }
      }
      Some(ConfigData.fromSource(source))
    }

    // Get oversize files that are stored in the annex server
    def getOversize: Future[Option[ConfigData]] = async {
      await(get(shaFilePath(path), id)) match {
        case None =>
          None
        case Some(configData) =>
          val sha1 = await(configData.toStringF)
          await(fileService.get(sha1))
      }
    }

    // If the file exists in the repo, get its data
    async {
      await(pathStatus(path, id)) match {
        case PathStatus.NormalSize ⇒ await(getNormalSize)
        case PathStatus.Oversize   ⇒ await(getOversize)
        case PathStatus.Missing    ⇒ None
      }
    }
  }

  override def get(path: Path, time: Instant): Future[Option[ConfigData]] = {

    // Gets the ConfigFileHistory matching the date
    def getHist: Future[Option[ConfigFileHistory]] = async {
      val h     = await(history(path))
      val found = h.find(t => t.time.isBefore(time) || t.time.equals(time))
      if (found.nonEmpty) found
      else if (h.isEmpty) None
      else Some(if (time.isAfter(h.head.time)) h.head else h.last)
    }

    async {
      val hist = await(getHist)
      if (hist.isEmpty) None else await(get(path, hist.map(_.id)))
    }
  }

  override def exists(path: Path, id: Option[ConfigId]): Future[Boolean] = async {
    await(pathStatus(path, id)).isInstanceOf[PathStatus.Present]
  }

  override def delete(path: Path, comment: String = "deleted"): Future[Unit] = async {
    await(pathStatus(path)) match {
      case PathStatus.NormalSize ⇒ await(svnRepo.delete(path, comment))
      case PathStatus.Oversize   ⇒ await(svnRepo.delete(shaFilePath(path), comment))
      case PathStatus.Missing    ⇒ throw FileNotFound(path)
    }
  }

  override def list(): Future[List[ConfigFileInfo]] = async {
    await(svnRepo.list()).map { entry =>
      ConfigFileInfo(Paths.get(entry.getRelativePath), ConfigId(entry.getRevision), entry.getCommitMessage)
    }
  }

  // XXX Should .sha1 files have the .sha1 suffix removed in the result?
  override def history(path: Path, maxResults: Int): Future[List[ConfigFileHistory]] = async {
    await(pathStatus(path)) match {
      case PathStatus.NormalSize ⇒ await(hist(path, maxResults))
      case PathStatus.Oversize   ⇒ await(hist(shaFilePath(path), maxResults))
      case PathStatus.Missing    ⇒ throw FileNotFound(path)
    }
  }

  override def setDefault(path: Path, id: Option[ConfigId] = None): Future[Unit] = async {
    if (!await(exists(path, id))) {
      throw FileNotFound(path)
    }
    val defaultPath = defaultFilePath(path)

    val present = await(exists(defaultPath))

    id match {
      case Some(configId) if present ⇒ await(update(defaultPath, ConfigData.fromString(configId.id)))
      case Some(configId)            ⇒ await(create(defaultPath, ConfigData.fromString(configId.id)))
      case None if present           ⇒ await(delete(defaultPath))
      case None                      ⇒ ()
    }
  }

  override def resetDefault(path: Path): Future[Unit] = setDefault(path, None)

  override def getDefault(path: Path): Future[Option[ConfigData]] = {

    def getDefaultById(configId: ConfigId): Future[Option[ConfigData]] = async {
      val d  = await(get(defaultFilePath(path)))
      val id = if (d.isDefined) await(d.get.toStringF) else configId.id
      await(get(path, Some(ConfigId(id))))
    }

    async {
      await(getCurrentVersion(path)) match {
        case None           ⇒ None
        case Some(configId) ⇒ await(getDefaultById(configId))
      }
    }
  }

  private def pathStatus(path: Path, id: Option[ConfigId] = None): Future[PathStatus] = async {
    val revision = id.map(_.id.toLong)
    if (await(svnRepo.pathExists(path, revision))) {
      PathStatus.NormalSize
    } else if (await(svnRepo.pathExists(shaFilePath(path), revision))) {
      PathStatus.Oversize
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
  private def put(path: Path, configData: ConfigData, update: Boolean, comment: String = ""): Future[ConfigId] =
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
      case PathStatus.NormalSize ⇒ await(hist(path, 1)).headOption.map(_.id)
      case PathStatus.Oversize   ⇒ await(hist(shaFilePath(path), 1)).headOption.map(_.id)
      case PathStatus.Missing    ⇒ None
    }
  }

  private def hist(path: Path, maxResults: Int = Int.MaxValue): Future[List[ConfigFileHistory]] = async {
    await(svnRepo.hist(path, maxResults))
      .map(e => ConfigFileHistory(ConfigId(e.getRevision), e.getMessage, e.getDate.toInstant))
  }

  // File used to store the SHA-1 of the actual file, if oversized.
  private def shaFilePath(path: Path): Path = Paths.get(s"${path.toString}${settings.`sha1-suffix`}")

  // File used to store the id of the default version of the file.
  private def defaultFilePath(path: Path): Path = Paths.get(s"${path.toString}${settings.`default-suffix`}")
}
