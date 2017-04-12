package csw.services.config.server.repo

import java.io._
import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.stream.scaladsl.StreamConverters
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.api.scaladsl.ConfigManager
import csw.services.config.server.{ActorRuntime, Settings}
import csw.services.location.internal.StreamExt.RichSource

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

class SvnConfigManager(settings: Settings, oversizeFileManager: OversizeFileManager, actorRuntime: ActorRuntime, svnOps: SvnOps) extends ConfigManager {

  import actorRuntime._

  override def name: String = "my name is missing"

  override def create(path: Path, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = {

    def createOversize(): Future[ConfigId] = async {
      val sha1 = await(oversizeFileManager.post(configData))
      await(create(shaPath(path), ConfigData.fromString(sha1), oversize = false, comment))
    }

    // If the file does not already exists in the repo, create it
    def createImpl(present: Boolean): Future[ConfigId] = async {
      if (present) {
        throw new IOException("File already exists in repository: " + path)
      } else if (oversize) {
        await(createOversize())
      } else {
        await(put(path, configData, update = false, comment))
      }
    }

    async {
      val present = await(exists(path))
      await(createImpl(present))
    }
  }

  override def update(path: Path, configData: ConfigData, comment: String): Future[ConfigId] = {

    def updateOversize(): Future[ConfigId] = async {
      val sha1 = await(oversizeFileManager.post(configData))
      await(update(shaPath(path), ConfigData.fromString(sha1), comment))
    }

    // If the file already exists in the repo, update it
    def updateImpl(present: Boolean): Future[ConfigId] = async {
      if (!present) {
        throw new FileNotFoundException("File not found: " + path)
      } else if (await(isOversize(path))) {
        await(updateOversize())
      } else {
        await(put(path, configData, update = true, comment))
      }
    }

    async {
      val present = await(exists(path))
      await(updateImpl(present))
    }
  }

  override def get(path: Path, id: Option[ConfigId]): Future[Option[ConfigData]] = {
    // Get oversize files that are stored in the annex server
    def getOversize: Future[Option[ConfigData]] = async {
      val opt = await(get(shaPath(path), id))
      await(getData(opt))
    }

    // Gets the actual file data using the SHA-1 value contained in the checked in file
    def getData(opt: Option[ConfigData]): Future[Option[ConfigData]] = async {
      opt match {
        case None             =>
          None
        case Some(configData) =>
          val sha1 = await(configData.toStringF)
          await(oversizeFileManager.get(sha1))
      }
    }

    // Returns the contents of the given version of the file, if found
    def getConfigData: Future[Option[ConfigData]] = async {
      val outputStream = StreamConverters.asOutputStream().cancellableMat
      val revision = await(svnOps.svnRevision(id.map(_.id.toLong)))
      val source = outputStream.mapMaterializedValue { case (out, switch) ⇒
        svnOps.getFile(path, revision.getNumber, out).recover {
          case NonFatal(ex) ⇒ switch.abort(ex)
        }
      }
      Some(ConfigData.fromSource(source))
    }

    // If the file exists in the repo, get its data
    def getImpl(present: Boolean): Future[Option[ConfigData]] = async {
      if (!present) {
        None
      } else if (await(isOversize(path))) {
        await(getOversize)
      } else {
        await(getConfigData)
      }
    }

    async {
      val present = await(exists(path))
      await(getImpl(present))
    }
  }

  override def get(path: Path, time: Instant): Future[Option[ConfigData]] = {

    // Gets the ConfigFileHistory matching the date
    def getHist: Future[Option[ConfigFileHistory]] = async {
      val h = await(history(path))
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

  override def exists(path: Path): Future[Boolean] = async {
    val normalSize = await(isNormalSize(path))
    if(normalSize) normalSize else await(isOversize(path))
  }

  //TODO: This implementation deletes all versions of a file. This is different than the expecations
  override def delete(path: Path, comment: String = "deleted"): Future[Unit] = async {
    if (await(isOversize(path))) await(svnOps.delete(shaPath(path), comment))
    else if (await(isNormalSize(path))) await(svnOps.delete(path, comment))
    else throw new FileNotFoundException("Can't delete " + path + " because it does not exist")
  }

  override def list(): Future[List[ConfigFileInfo]] = async {
    await(svnOps.list())
      .map(e => ConfigFileInfo(Paths.get(e.getRelativePath), ConfigId(e.getRevision), e.getCommitMessage))
  }

  override def history(path: Path, maxResults: Int): Future[List[ConfigFileHistory]] = async {
    // XXX Should .sha1 files have the .sha1 suffix removed in the result?
    if (await(isOversize(path))) {
      await(hist(shaPath(path), maxResults))
    }
    else {
      await(hist(path, maxResults))
    }
  }

  override def setDefault(path: Path, id: Option[ConfigId] = None): Future[Unit] = async {
    val maybeConfigId = if (id.isDefined) id else await(getCurrentVersion(path))
    maybeConfigId match {
      case Some(configId) => await(create(defaultFile(path), ConfigData.fromString(configId.id)))
      case None           => throw new FileNotFoundException(s"Unknown path $path")
    }
  }

  override def resetDefault(path: Path): Future[Unit] = delete(defaultFile(path))

  override def getDefault(path: Path): Future[Option[ConfigData]] = async {
    await(getCurrentVersion(path)) match {
      case None           ⇒
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
  private def put(path: Path, configData: ConfigData, update: Boolean, comment: String = ""): Future[ConfigId] = async {
    val inputStream = configData.toInputStream
    val commitInfo = if (update) {
      await(svnOps.modifyFile(path, comment, inputStream))
    } else {
      await(svnOps.addFile(path, comment, inputStream))
    }
    ConfigId(commitInfo.getNewRevision)
  }

  // True if the file exists
  private def isNormalSize(path: Path): Future[Boolean] = {
    svnOps.pathExists(path)
  }

  // True if the .sha1 file exists, meaning the file needs special oversize handling.
  private def isOversize(path: Path): Future[Boolean] = {
    svnOps.pathExists(shaPath(path))
  }

  // File used to store the SHA-1 of the actual file, if oversized.
  private def shaPath(path: Path): Path = Paths.get(s"${path.toString}${settings.`sha1-suffix`}")

  // Returns the current version of the file, if known
  private def getCurrentVersion(path: Path): Future[Option[ConfigId]] = async {
    if (await(isOversize(path))) {
      await(hist(shaPath(path), 1)).headOption.map(_.id)
    }
    else {
      await(hist(path, 1)).headOption.map(_.id)
    }
  }

  private def hist(path: Path, maxResults: Int = Int.MaxValue): Future[List[ConfigFileHistory]] = async {
    await(svnOps.hist(path, maxResults))
      .map(e => ConfigFileHistory(ConfigId(e.getRevision), e.getMessage, e.getDate.toInstant))
  }

  // File used to store the id of the default version of the file.
  private def defaultFile(path: Path): Path = Paths.get(s"${path.toString}${settings.`default-suffix`}")
}
